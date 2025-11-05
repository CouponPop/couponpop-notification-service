package com.couponpop.notificationservice.common.rabbitmq.consumer;

import com.couponpop.notificationservice.common.fcm.service.FcmSendService;
import com.couponpop.notificationservice.common.rabbitmq.dto.request.CouponUsageStatsFcmSendRequest;
import com.couponpop.notificationservice.domain.notification.constants.NotificationTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponUsageStatsFcmSendEventConsumer {

    private final FcmSendService fcmSendService;

    @RabbitListener(queues = "${rabbitmq.coupon-usage-stats-fcm-send.queue}")
    public void handle(CouponUsageStatsFcmSendRequest message) {
        log.info("쿠폰 사용 통계 FCM 요청 수신: {}", message);

        String title = NotificationTemplates.COUPON_USAGE_STATS_TITLE.formatted(message.topDong(), message.activeEventCount());
        String body = NotificationTemplates.COUPON_USAGE_STATS_BODY.formatted(message.topHour());

        fcmSendService.sendNotification(message.memberId(), message.token(), title, body);
    }
}
