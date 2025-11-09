package com.couponpop.notificationservice.common.rabbitmq.consumer;

import com.couponpop.couponpopcoremodule.dto.coupon.event.model.CouponUsageStatsFcmSendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.couponpop.notificationservice.common.config.CouponUsageStatsFcmSendConsumerConfig.COUPON_USAGE_STATS_FCM_SEND_DLQ;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponUsageStatsFcmSendDlqConsumer {

    @RabbitListener(queues = COUPON_USAGE_STATS_FCM_SEND_DLQ)
    public void handleDlq(CouponUsageStatsFcmSendMessage payload, Message message) {

        log.error("DLQ 적재: memberId={}, token={}, headers={}",
                payload.memberId(),
                payload.token(),
                message.getMessageProperties().getHeaders());

        /*
         * TODO
         *   - DLQ 적재 시 별도 알림 또는 모니터링 처리 로직 추가
         *   - 예: 슬랙 알림, 이메일 알림, 모니터링 시스템 연동 등
         */

    }
}
