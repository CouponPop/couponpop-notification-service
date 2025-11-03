package com.couponpop.notificationservice.common.fcm.service;

import com.couponpop.notificationservice.common.fcm.factory.FcmMessageFactory;
import com.couponpop.notificationservice.domain.fcmtoken.service.FcmTokenService;
import com.couponpop.notificationservice.domain.notificationhistory.dto.payload.NotificationHistoryPayload;
import com.couponpop.notificationservice.domain.notificationhistory.enums.NotificationHistoryStatus;
import com.couponpop.notificationservice.domain.notificationhistory.enums.NotificationHistoryType;
import com.couponpop.notificationservice.domain.notificationhistory.service.NotificationHistoryService;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.couponpop.notificationservice.common.constants.AsyncExecutors.FCM_TASK_EXECUTOR;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmSendService {

    private static final NotificationHistoryType NOTIFICATION_HISTORY_TYPE = NotificationHistoryType.FCM;

    private final FcmMessageFactory fcmMessageFactory;
    private final NotificationHistoryService notificationHistoryService;
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
    @Async(FCM_TASK_EXECUTOR)
    public CompletableFuture<Void> sendNotification(Long memberId, String token, String title, String body) {
        log.info("푸시 알림 전송 시작: memberId={}, token={}", memberId, token);

        CompletableFuture<Void> result = new CompletableFuture<>();

        if (!StringUtils.hasText(token)) {
            log.info("FCM 전송 토큰이 유효하지 않아 전송을 건너뜁니다.");
            result.complete(null);
            return result;
        }

        Message message = fcmMessageFactory.createMessage(token, title, body);
        ApiFuture<String> sendFuture = firebaseMessaging.sendAsync(message);

        ApiFutures.addCallback(sendFuture, new ApiFutureCallback<>() {
            @Override
            public void onFailure(Throwable throwable) {
                log.error("FCM 전송 중 오류 발생: {}", throwable.getMessage(), throwable);

                try {
                    fcmTokenService.deleteToken(token);
                } catch (Exception e) {
                    log.warn("FCM 토큰 삭제 중 오류 발생: {}", e.getMessage(), e);
                }

                NotificationHistoryPayload notificationHistoryPayload = NotificationHistoryPayload.of(
                        memberId,
                        NOTIFICATION_HISTORY_TYPE,
                        title,
                        body,
                        NotificationHistoryStatus.FAILURE,
                        throwable.getMessage()
                );
                notificationHistoryService.createNotificationHistory(notificationHistoryPayload);

                result.completeExceptionally(throwable);
            }

            @Override
            public void onSuccess(String messageId) {
                log.info("FCM 전송 성공: messageId={}", messageId);

                fcmTokenService.updateLastUsedAt(token);
                NotificationHistoryPayload notificationHistoryPayload = NotificationHistoryPayload.of(
                        memberId,
                        NOTIFICATION_HISTORY_TYPE,
                        title,
                        body,
                        NotificationHistoryStatus.SUCCESS,
                        null
                );
                notificationHistoryService.createNotificationHistory(notificationHistoryPayload);

                result.complete(null);
            }
        }, fcmTaskExecutor);

        return result;
    }
}
