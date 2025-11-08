package com.couponpop.notificationservice.common.rabbitmq.consumer;

import com.couponpop.couponpopcoremodule.dto.coupon.event.model.CouponUsageStatsFcmSendMessage;
import com.couponpop.notificationservice.common.fcm.service.FcmSendService;
import com.couponpop.notificationservice.domain.notification.constants.NotificationTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.couponpop.notificationservice.common.config.CouponUsageStatsFcmSendConsumerConfig.COUPON_USAGE_STATS_FCM_SEND_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponUsageStatsFcmSendConsumer {

    private final FcmSendService fcmSendService;

    @RabbitListener(queues = COUPON_USAGE_STATS_FCM_SEND_QUEUE)
    public void handle(CouponUsageStatsFcmSendMessage message) {
        try {
            log.info("쿠폰 사용 통계 FCM 요청 수신: {}", message);

            Long memberId = message.memberId();
            List<String> tokens = message.tokens();
            String topDong = message.topDong();
            int topHour = message.topHour();
            int activeEventCount = message.activeEventCount();

            String title = NotificationTemplates.COUPON_USAGE_STATS_TITLE.formatted(topDong, activeEventCount);
            String body = NotificationTemplates.COUPON_USAGE_STATS_BODY.formatted(topHour);

            for (String token : tokens) {
                fcmSendService.sendNotification(memberId, token, title, body);
            }
        } catch (Exception e) {
            log.error("쿠폰 사용 통계 FCM 요청 처리 중 오류 발생: {}", message, e);
        }
    }
}
