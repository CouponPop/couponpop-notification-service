package com.couponpop.notificationservice.common.rabbitmq.consumer;

import com.couponpop.couponpopcoremodule.dto.coupon.event.model.CouponUsedMessage;
import com.couponpop.notificationservice.common.fcm.service.FcmSendService;
import com.couponpop.notificationservice.domain.fcmtoken.entity.FcmToken;
import com.couponpop.notificationservice.domain.fcmtoken.repository.FcmTokenRepository;
import com.couponpop.notificationservice.domain.notification.constants.NotificationTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.couponpop.couponpopcoremodule.constants.RabbitMqQueue.COUPON_ISSUED_QUEUE;
import static com.couponpop.couponpopcoremodule.constants.RabbitMqQueue.COUPON_USED_QUEUE;


@Slf4j
@Component
@RequiredArgsConstructor
public class CouponConsumer {

    private final FcmTokenRepository fcmTokenRepository;
    private final FcmSendService fcmSendService;

    @RabbitListener(queues = COUPON_USED_QUEUE)
    public void couponUsedHandle(CouponUsedMessage message) {
        try {
            log.info("[쿠폰 사용 이벤트] : {}", message);
            Long memberId = message.memberId();
            List<FcmToken> fcmTokens = fcmTokenRepository.findByMemberIdAndNotificationEnabledIsTrue(memberId);
            String eventName = message.eventName();
            String storeName = message.storeName();

            String title = NotificationTemplates.COUPON_USED_TITLE.formatted(eventName);
            String body = NotificationTemplates.COUPON_USED_BODY.formatted(eventName, storeName);

            /*
             * TODO
             *   - 알림 중복 전송 방지(멱등성)를 위한 이벤트 발행 쪽에서의 traceId 생성 필요합니다.
             *   - traceId 생성 로직은 코어 모듈의 NotificationTraceIdGenerator.generate() 메서드 참고 바랍니다.
             *   - 현재는 traceId 없이 null로 임시 처리했습니다.
             */
            fcmTokens.forEach(fcmToken -> {
                fcmSendService.sendNotification(null, memberId, fcmToken.getFcmToken(), title, body);
            });
        } catch (Exception e) {
            log.error("쿠폰 사용 FCM 요청 처리 중 오류 발생: {}", message, e);
        }
    }

    @RabbitListener(queues = COUPON_ISSUED_QUEUE)
    public void couponIssuedHandle(String event) throws Exception {
        // 일부러 예외 발생
        if (event.equals("ex")) {
            throw new Exception("DLQ 테스트용 예외 발생!");
        }
        log.info("[쿠폰 발행 이벤트] : {}", event);
    }
}
