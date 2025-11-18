# couponpop-notification-service

CouponPop 이벤트에 대한 푸시 알림을 담당하는 Spring Boot 3 마이크로서비스입니다. 회원 FCM 토큰을 관리하고 RabbitMQ 이벤트를 소비해 Firebase Cloud Messaging으로 알림을 전송하며, 사용자·내부 API를 모두 제공합니다.

## 주요 역할

- 회원/디바이스별 FCM 토큰을 저장·갱신·만료 처리하고, 내부 서비스에서 조회할 수 있는 API를 제공합니다.
- 쿠폰 발행/사용 이벤트를 RabbitMQ에서 수신한 뒤 템플릿에 맞춰 FCM 메시지를 발송합니다.
- Redis 기반 멱등성 제어와 MySQL 알림 히스토리 기록을 통해 재전송/추적이 가능하도록 합니다.
- 실패 메시지는 DLQ에 적재하고 Slack Webhook으로 운영 알림을 전송합니다.

## 이벤트 & 큐 토폴로지

| 이벤트       | 큐                                      | 라우팅 키                         | 소비자                               | 처리 결과                     |
|-----------|----------------------------------------|-------------------------------|-----------------------------------|---------------------------|
| 쿠폰 발행     | `coupon.queue.issued`                  | `coupon.issued`               | `CouponIssuedConsumer`            | 발행 템플릿으로 FCM 발송 + 히스토리 적재 |
| 쿠폰 사용     | `coupon.queue.used`                    | `coupon.used`                 | `CouponConsumer`                  | 사용 템플릿으로 FCM 발송 + 히스토리 적재 |
| 사용 통계 FCM | `coupon.usage.stats.fcm.send.queue.v1` | `coupon.usage.stats.fcm.send` | `CouponUsageStatsFcmSendConsumer` | “해당 동에서 진행 중인 이벤트” 알림 발송  |

모든 큐는 DLX(`coupon.exchange.dlx`)와 DLQ(`*.dlq`)를 갖고 있으며, 본 큐 메시지는 TTL 5분, DLQ는 7일 보관합니다. DLQ 소비자(`CouponIssuedDlqConsumer`, `CouponUsedDlqConsumer`, `CouponUsageStatsFcmSendDlqConsumer`)는 토큰을 마스킹해 Slack으로 상세 정보를 전송합니다.

기본 리스너 컨테이너(`RabbitMqConfig`)는 JSON 변환, 동시성 2~10, 최대 5회 지수 백오프 재시도를 적용하고 `AmqpRejectAndDontRequeueException`으로 재시도 불가능한 메시지를 즉시 DLQ로 보냅니다.

## HTTP API

### 사용자용 (JWT 인증)

| Method | Path                | 설명                                                                                      |
|--------|---------------------|-----------------------------------------------------------------------------------------|
| `POST` | `/api/v1/fcm-token` | 인증된 회원/디바이스의 FCM 토큰을 upsert. `Authorization: Bearer <JWT>` 필요, Body는 `FcmTokenRequest`. |

### 내부용 (시스템 토큰)

| Method | Path                                         | 설명                                   |
|--------|----------------------------------------------|--------------------------------------|
| `POST` | `/internal/v1/fcm-token/expire`              | 특정 회원/토큰 조합 삭제 (로그아웃 등).             |
| `POST` | `/internal/v1/fcm-tokens/search`             | 회원 ID 리스트 기반 FCM 토큰 조회 (배치 서비스가 사용). |
| `GET`  | `/internal/v1/fcm-tokens/members/{memberId}` | 단일 회원의 활성 토큰 조회.                     |

모든 응답은 공통 `ApiResponse` 형식을 따릅니다.

## 알림 파이프라인

1. **메시지 생성**: `FcmMessageFactory`가 플랫폼별(Android/iOS/Web) TTL·우선순위·데이터 payload를 포함한 `Message`를 생성합니다.
2. **멱등성 확보**: 모든 메시지는 Trace ID를 포함합니다(쿠폰 이벤트는 생산 시 부여, 사용 통계 이벤트는 `NotificationTraceIdGenerator`). `NotificationIdempotencyService`가 5분 TTL Redis 키를 설정해 중복 처리를 막고, 성공 시 7일 TTL의 `done` 상태로 업데이트합니다.
3. **전송**: `FcmSendService`가 전용 비동기 스레드풀(`async.fcm.*`)에서 Firebase Messaging API를 호출하고, 성공 시 `lastUsedAt` 갱신 및 `NotificationHistory` 행을 `SUCCESS`로 저장합니다.
4. **에러 처리**: Firebase `UNREGISTERED` 오류는 해당 토큰을 삭제합니다. 재시도 가능한 오류는 `RetryableFcmException`을 던져 Rabbit이 재시도하게 하고, 그 외는 `NonRetryableFcmException`으로 감싸 즉시 DLQ로 이동합니다.

## 아키텍처 & 의존성

- **프레임워크**: Spring Boot 3.5, Spring Data JPA, Spring Security, Validation, RabbitMQ, Spring Data Redis, Micrometer Prometheus, Firebase Admin SDK, Flyway.
- **공유 모듈**: `couponpop-core`(Rabbit 상수·DTO·Trace 유틸), `couponpop-security`(JWT 파싱, `@CurrentMember`, 시스템 토큰).
- **데이터 스토어**: MySQL Master/Slave ( `RoutingDataSource` + `ReplicationType` ), Redis(멱등성), RabbitMQ(이벤트 소비), Firebase(실제 푸시 전송).
- **운영 연계**: Slack Webhook, AWS Parameter Store(`/couponpop/prod/notification-service/`), Actuator `/actuator/health`, `/actuator/prometheus`.

## 환경 변수

`spring.config.import=optional:file:.env`를 활용해 `couponpop-notification-service/.env`에 설정하거나, 운영에서는 Parameter Store를 이용합니다.

| 변수                                                                         | 설명                                                                                                                                    |
|----------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| `DB_MASTER_URL`, `DB_SLAVE_URL`, `DB_USERNAME`, `DB_PASSWORD`              | 마스터/슬레이브 JDBC URL 및 계정 정보. 쓰기는 마스터, 읽기 전용 트랜잭션은 슬레이브로 라우팅됩니다.                                                                         |
| `REDIS_HOST`, `REDIS_PORT`                                                 | Redis 접속 정보 (`NotificationIdempotencyService`).                                                                                       |
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` | RabbitMQ 브로커 접속 정보.                                                                                                                   |
| `JWT_SECRET_KEY`                                                           | `couponpop-security`에서 사용하는 비밀키.                                                                                                      |
| `SLACK_WEBHOOK_URL`                                                        | DLQ 소비자가 경보를 전송할 Slack Webhook.                                                                                                       |
| `FCM_FIREBASE_CONFIG_PATH`                                                 | `fcm.firebase-config-path` 오버라이드. 로컬은 기본값인 `classpath:firebase/serviceAccountKey.json`, 운영은 `/app/config/serviceAccountKey.json`을 권장. |
| `ASYNC_FCM_*`                                                              | FCM 비동기 스레드풀 세부 설정(선택).                                                                                                               |
| `GITHUB_ACTOR`, `GITHUB_TOKEN`                                             | 사설 GitHub 패키지(보안/코어 모듈) 다운로드용.                                                                                                        |

### 샘플 `.env`

```dotenv
DB_MASTER_URL=jdbc:mysql://localhost:3307/notification_db
DB_SLAVE_URL=jdbc:mysql://localhost:3317/notification_db
DB_USERNAME=root
DB_PASSWORD=1234
REDIS_HOST=localhost
REDIS_PORT=6379
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=admin
JWT_SECRET_KEY=local-dev-secret
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/T000/B000/XXXX
GITHUB_ACTOR=<your-github-id>
GITHUB_TOKEN=<github-personal-access-token>
```

## 로컬 개발 절차

1. **사전 준비**: JDK 17, Docker, 로컬 MySQL/Redis/RabbitMQ. 리포지토리 루트의 `local-docker-infra`를 쓰거나, 모듈 내 `docker-compose.yml`(Redis+RabbitMQ)로도 충분합니다.
2. **데이터베이스 준비**: `local-docker-infra/docker-compose.db.replica.yml` 등으로 Master/Slave MySQL을 띄우고 `.env`의 URL을 맞춥니다.
3. **Firebase 키 배치**: 로컬은 `src/main/resources/firebase/serviceAccountKey.json`, 운영은 `/app/config/serviceAccountKey.json`에 서비스 계정 키를 둡니다.
4. **마이그레이션 실행**
   ```bash
   ./gradlew flywayMigrate
   ```
   (기본 설정은 `conf/flyway.conf`를 사용하며 필요 시 URL/계정 Override).
5. **애플리케이션 실행**
   ```bash
   cd couponpop-notification-service
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```
   로컬 포트 `8084`, 운영 포트 `8080`.
6. **테스트 수행**: `./gradlew test` (Spring Security, Repository, Rabbit Listener 테스트 + JaCoCo 리포트 `build/reports/jacoco/test/html`).
7. **수동 검증**:
    - `POST /api/v1/fcm-token` 호출 후 `fcm_tokens` 테이블 반영 여부 확인.
    - RabbitMQ 관리 콘솔(`http://localhost:15672`)에서 `coupon.issued` 혹은 `coupon.usage.stats.fcm.send` 메시지를 게시하고 로그·`notification_histories`를 확인.

## 운영 시 참고

- **읽기 전용 트랜잭션**: `@Transactional(readOnly = true)`가 선언된 서비스는 자동으로 슬레이브 DB에 연결되므로, 쓰기가 필요한 로직에는 readOnly 플래그를 설정하지 않도록 주의합니다.
- **토큰 중복 처리**: `FcmTokenService`는 `fcmToken` 혹은 (`memberId`,`deviceIdentifier`) 중복을 감지해 토큰이나 디바이스 정보를 최신화하고, 사용 시각을 업데이트합니다.
- **DLQ 알림**: 모든 DLQ 소비자는 토큰을 마스킹한 Slack 메시지와 Rabbit 헤더(`x-death`, `x-exception-message` 등)를 보내주므로, 재처리 여부를 빠르게 판단할 수 있습니다.
- **모니터링**: `/actuator/health`에서 DB/Redis/Rabbit 상태를 확인하고, `/actuator/prometheus`의 `application=notification-service` 태그를 통해 지표를 수집합니다.
- **운영 설정**: 민감한 값은 AWS Parameter Store `/couponpop/prod/notification-service/`에 저장하며, 인스턴스 프로파일이 해당 경로를 읽을 수 있어야 합니다. Firebase 키 파일도 `/app/config/serviceAccountKey.json`에 마운트해야 합니다.
