package com.couponpop.notificationservice.common.rabbitmq.consumer;

import com.couponpop.couponpopcoremodule.dto.coupon.event.model.CouponIssuedMessage;
import com.couponpop.notificationservice.common.fcm.exception.NonRetryableFcmException;
import com.couponpop.notificationservice.common.fcm.service.FcmSendService;
import com.couponpop.notificationservice.domain.notification.constants.NotificationTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionException;

import static com.couponpop.couponpopcoremodule.constants.RabbitMqQueue.COUPON_ISSUED_QUEUE;


@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssuedConsumer {

    private final FcmSendService fcmSendService;

    @RabbitListener(queues = COUPON_ISSUED_QUEUE)
    public void couponIssuedHandle(CouponIssuedMessage message) {
        try {
            log.info("[쿠폰 발행 이벤트] : {}", message);

            String traceId = message.traceId();
            Long memberId = message.memberId();
            String token = message.token();
            String eventName = message.eventName();
            String storeName = message.storeName();
            int totalCount = message.totalCount();
            int issuedCount = message.issuedCount();

            String title = NotificationTemplates.COUPON_ISSUED_TITLE.formatted(eventName);
            String body = NotificationTemplates.COUPON_ISSUED_BODY.formatted(eventName, totalCount, issuedCount, storeName);

            fcmSendService.sendNotification(traceId, memberId, token, title, body).join();
        } catch (CompletionException e) {
            log.error("쿠폰 발행 FCM 요청 처리 중 오류 발생: {}", message, e);
            Throwable cause = e.getCause();
            if (cause instanceof NonRetryableFcmException nonRetryable) {
                log.warn("재시도 불가 FCM 전송: memberId={}, reason={}", message.memberId(), nonRetryable.getMessage());

                // 재시도 불가 예외는 DLQ로 보내기 위해 AmqpRejectAndDontRequeueException으로 래핑
                throw new AmqpRejectAndDontRequeueException(nonRetryable.getMessage(), nonRetryable);
            }

            // 그 외 예외는 재시도 가능하므로 그대로 예외 던지기
            throw e;
        }
    }

}
