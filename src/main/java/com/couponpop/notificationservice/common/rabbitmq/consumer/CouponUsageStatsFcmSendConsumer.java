package com.couponpop.notificationservice.common.rabbitmq.consumer;

import com.couponpop.couponpopcoremodule.dto.coupon.event.model.CouponUsageStatsFcmSendMessage;
import com.couponpop.notificationservice.common.fcm.exception.NonRetryableFcmException;
import com.couponpop.notificationservice.common.fcm.service.FcmSendService;
import com.couponpop.notificationservice.domain.notification.constants.NotificationTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionException;

import static com.couponpop.notificationservice.common.config.CouponUsageStatsFcmSendConsumerConfig.COUPON_USAGE_STATS_FCM_SEND_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponUsageStatsFcmSendConsumer {

    private final FcmSendService fcmSendService;

    @RabbitListener(queues = COUPON_USAGE_STATS_FCM_SEND_QUEUE)
    public void handle(CouponUsageStatsFcmSendMessage payload) {

        try {
            log.info("쿠폰 사용 통계 FCM 요청 수신: {}", payload);

            String traceId = payload.traceId(); // 멱등성
            Long memberId = payload.memberId();
            String token = payload.token();
            String topDong = payload.topDong();
            int topHour = payload.topHour();
            int activeEventCount = payload.activeEventCount();

            String title = NotificationTemplates.COUPON_USAGE_STATS_TITLE.formatted(topDong, activeEventCount);
            String body = NotificationTemplates.COUPON_USAGE_STATS_BODY.formatted(topHour);

            // FCM 알림 푸시 실패 시 재시도/DLQ 처리를 위해 Blocking
            fcmSendService.sendNotification(traceId, memberId, token, title, body).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof NonRetryableFcmException nonRetryable) {
                log.warn("재시도 불가 FCM 전송: memberId={}, reason={}", payload.memberId(), nonRetryable.getMessage());

                // 재시도 불가 예외는 DLQ로 보내기 위해 AmqpRejectAndDontRequeueException으로 래핑
                throw new AmqpRejectAndDontRequeueException(nonRetryable.getMessage(), nonRetryable);
            }

            // 그 외 예외는 재시도 가능하므로 그대로 예외 던지기
            throw e;
        }
    }
}
