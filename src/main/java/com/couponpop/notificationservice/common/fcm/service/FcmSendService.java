package com.couponpop.notificationservice.common.fcm.service;

import com.couponpop.notificationservice.common.fcm.exception.NonRetryableFcmException;
import com.couponpop.notificationservice.common.fcm.exception.RetryableFcmException;
import com.couponpop.notificationservice.common.fcm.factory.FcmMessageFactory;
import com.couponpop.notificationservice.domain.fcmtoken.service.FcmTokenService;
import com.couponpop.notificationservice.domain.notification.service.NotificationIdempotencyService;
import com.couponpop.notificationservice.domain.notificationhistory.dto.payload.NotificationHistoryPayload;
import com.couponpop.notificationservice.domain.notificationhistory.enums.NotificationHistoryStatus;
import com.couponpop.notificationservice.domain.notificationhistory.enums.NotificationHistoryType;
import com.couponpop.notificationservice.domain.notificationhistory.service.NotificationHistoryService;
import com.google.firebase.IncomingHttpResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmSendService {

    private static final NotificationHistoryType NOTIFICATION_HISTORY_TYPE = NotificationHistoryType.FCM;

    private final FcmMessageFactory fcmMessageFactory;
    private final NotificationHistoryService notificationHistoryService;
    private final NotificationIdempotencyService notificationIdempotencyService;
    private final FcmTokenService fcmTokenService;
    private final FirebaseMessaging firebaseMessaging;
    private final Executor fcmTaskExecutor;

    /**
     * 비동기 푸시 알림 전송
     *
     * @param memberId 회원 ID
     * @param token    FCM 토큰
     * @param title    알림 제목
     * @param body     알림 내용
     * @return 비동기 실행 결과
     */
    public CompletableFuture<Void> sendNotification(String traceId, Long memberId, String token, String title, String body) {
        log.info("푸시 알림 전송 시작: memberId={}, token={}", memberId, token);

        if (!StringUtils.hasText(token)) {
            log.info("FCM 전송 토큰이 유효하지 않아 전송을 건너뜁니다.");
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            // 멱등성 키 획득 시도
            if (!notificationIdempotencyService.acquireProcessingKey(traceId)) {
                log.info("이미 처리 중인 FCM 전송 요청입니다. memberId={}, traceId={}", memberId, traceId);
                return;
            }

            Message message = fcmMessageFactory.createMessage(token, title, body);

            try {
                String messageId = firebaseMessaging.send(message);
                log.info("FCM 전송 성공: memberId={}, token={}, messageId={}", memberId, token, messageId);

                fcmTokenService.updateLastUsedAt(token);
                createHistory(traceId, memberId, title, body, NotificationHistoryStatus.SUCCESS, null);

                // 멱등성 키 완료 처리
                notificationIdempotencyService.markAsDone(traceId);
            } catch (FirebaseMessagingException e) {
                // 멱등성 키 해제
                notificationIdempotencyService.release(traceId);

                log.error("FCM 전송 실패: memberId={}, token={}, reason={}", memberId, token, e.getMessage(), e);

                removeInvalidToken(token, e);
                createHistory(traceId, memberId, title, body, NotificationHistoryStatus.FAILURE, e.getMessage());

                if (isRetryable(e)) {
                    throw new RetryableFcmException(e.getMessage(), e);
                }
                throw new NonRetryableFcmException(e.getMessage(), e);
            } catch (Exception e) {
                // 멱등성 키 해제
                notificationIdempotencyService.release(traceId);

                log.error("FCM 전송 중 예상치 못한 오류 발생: memberId={}, token={}, reason={}", memberId, token, e.getMessage(), e);
                createHistory(traceId, memberId, title, body, NotificationHistoryStatus.FAILURE, e.getMessage());

                throw new NonRetryableFcmException("FCM 전송 중 예상치 못한 오류가 발생했습니다.", e);
            }
        }, fcmTaskExecutor);
    }

    private void createHistory(
            String traceId,
            Long memberId,
            String title,
            String body,
            NotificationHistoryStatus status,
            String failReason
    ) {

        NotificationHistoryPayload notificationHistoryPayload = NotificationHistoryPayload.of(
                traceId,
                memberId,
                NOTIFICATION_HISTORY_TYPE,
                title,
                body,
                status,
                failReason
        );
        notificationHistoryService.createNotificationHistory(notificationHistoryPayload);
    }

    private void removeInvalidToken(String token, FirebaseMessagingException exception) {

        // UNREGISTERED 에러인 경우 토큰 삭제
        if (exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
            fcmTokenService.deleteToken(token);
        }
    }

    private boolean isRetryable(FirebaseMessagingException e) {

        MessagingErrorCode errorCode = e.getMessagingErrorCode();

        if (errorCode != null) {
            return switch (errorCode) {
                case INTERNAL, /* 서버 내부 오류 */
                     UNAVAILABLE, /* FCM 서비스 사용 불가 */
                     QUOTA_EXCEEDED /* 할당량 초과 */ -> true;
                default -> false;
            };
        }

        IncomingHttpResponse httpResponse = e.getHttpResponse();
        if (httpResponse == null) {
            return false;
        }
        int statusCode = httpResponse.getStatusCode();
        return statusCode == 429 // Too Many Requests
                || (500 <= statusCode && statusCode < 600); // 5xx 서버 오류
    }
}
