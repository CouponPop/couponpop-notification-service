CREATE TABLE `notification_histories`
(
    `id`             bigint                                                NOT NULL AUTO_INCREMENT,
    `member_id`      bigint                                                NOT NULL COMMENT '알림을 받은 회원 ID',
    `type`           enum ('FCM') COLLATE utf8mb4_unicode_ci               NOT NULL COMMENT '알림 유형(FCM 등)',
    `title`          varchar(255) COLLATE utf8mb4_unicode_ci               NOT NULL COMMENT '알림 제목',
    `body`           text COLLATE utf8mb4_unicode_ci                       NOT NULL COMMENT '알림 내용',
    `status`         enum ('SUCCESS','FAILURE') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '알림 발송 상태',
    `failure_reason` varchar(255) COLLATE utf8mb4_unicode_ci                        DEFAULT NULL COMMENT '발송 실패 원인',
    `created_at`     datetime                                              NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',

    PRIMARY KEY (`id`),
    KEY `idx_notification_histories_member_id` (`member_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='회원별 알림 발송 이력 저장 테이블';

CREATE TABLE `fcm_tokens`
(
    `id`                   bigint                                  NOT NULL AUTO_INCREMENT,
    `member_id`            bigint                                  NOT NULL COMMENT 'FCM 토큰을 보유한 회원 ID',
    `fcm_token`            varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'FCM에서 발급된 디바이스 토큰 값',
    `device_type`          varchar(32) COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT '디바이스 유형(ANDROID, IOS, WEB 등)',
    `device_identifier`    varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '디바이스 고유 식별자 또는 로컬 키',
    `notification_enabled` BOOLEAN                                 NOT NULL DEFAULT TRUE COMMENT '사용자 푸시 수신 여부',
    `last_used_at`         datetime                                         DEFAULT NULL COMMENT '푸시 발송 성공 또는 앱 사용 기준 최근 사용 시각',
    `created_at`           datetime                                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일',
    `updated_at`           datetime                                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_fcm_token_member_device` (`member_id`, `device_identifier`),
    UNIQUE KEY `uk_member_fcm_token_fcm_token` (`fcm_token`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='회원별 디바이스 FCM 토큰 정보 저장 테이블';