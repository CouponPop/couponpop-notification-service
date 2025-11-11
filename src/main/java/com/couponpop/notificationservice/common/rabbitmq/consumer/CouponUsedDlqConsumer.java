package com.couponpop.notificationservice.common.rabbitmq.consumer;

import com.couponpop.couponpopcoremodule.dto.coupon.event.model.CouponUsedMessage;
import com.couponpop.notificationservice.common.slack.service.SlackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.couponpop.notificationservice.common.rabbitmq.config.CouponUsedConsumerConfig.COUPON_USED_DLQ;
import static com.couponpop.notificationservice.common.utils.SecurityStringUtils.maskToken;
import static com.couponpop.notificationservice.common.utils.SecurityStringUtils.safeString;
import static java.util.Map.entry;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponUsedDlqConsumer {

    private final SlackService slackService;

    @RabbitListener(queues = COUPON_USED_DLQ)
    public void handleDlq(CouponUsedMessage payload, Message message) {

        log.error("쿠폰 사용 FCM DLQ 적재: memberId={}, token={}, headers={}",
                payload.memberId(),
                payload.token(),
                message.getMessageProperties().getHeaders());

        // Slack 알림 전송
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        Map<String, String> slackData = Map.ofEntries(
                entry("traceId", safeString(payload.traceId())),
                entry("token", maskToken(payload.token())),
                entry("memberId", safeString(payload.memberId())),
                entry("couponId", safeString(payload.couponId())),
                entry("storeId", safeString(payload.storeId())),
                entry("storeName", safeString(payload.storeName())),
                entry("eventId", safeString(payload.eventId())),
                entry("eventName", safeString(payload.eventName())),
                entry("messageType", safeString(payload.messageType())),
                entry("xDeath", safeString(headers.get("x-death"))),
                entry("exception", safeString(headers.get("x-exception-message"))),
                entry("stacktrace", safeString(headers.get("x-exception-stacktrace"))),
                entry("routingKey", safeString(message.getMessageProperties().getReceivedRoutingKey()))
        );
        slackService.sendMessage("쿠폰 사용 통계 FCM DLQ 적재", slackData);
    }
}
