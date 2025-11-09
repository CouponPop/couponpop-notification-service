package com.couponpop.notificationservice.common.rabbitmq.consumer;

import com.couponpop.couponpopcoremodule.dto.coupon.event.model.CouponUsageStatsFcmSendMessage;
import com.couponpop.notificationservice.common.slack.service.SlackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.couponpop.notificationservice.common.config.CouponUsageStatsFcmSendConsumerConfig.COUPON_USAGE_STATS_FCM_SEND_DLQ;
import static com.couponpop.notificationservice.common.utils.SecurityStringUtils.maskToken;
import static com.couponpop.notificationservice.common.utils.SecurityStringUtils.safeString;
import static java.util.Map.entry;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponUsageStatsFcmSendDlqConsumer {

    private final SlackService slackService;

    @RabbitListener(queues = COUPON_USAGE_STATS_FCM_SEND_DLQ)
    public void handleDlq(CouponUsageStatsFcmSendMessage payload, Message message) {

        log.error("쿠폰 사용 통계 FCM DLQ 적재: memberId={}, token={}, headers={}",
                payload.memberId(),
                payload.token(),
                message.getMessageProperties().getHeaders());

        // Slack 알림 전송
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        Map<String, String> slackData = Map.ofEntries(
                entry("traceId", safeString(payload.traceId())),
                entry("memberId", safeString(payload.memberId())),
                entry("token", maskToken(payload.token())),
                entry("topDong", safeString(payload.topDong())),
                entry("topHour", safeString(payload.topHour())),
                entry("activeEventCount", safeString(payload.activeEventCount())),
                entry("xDeath", safeString(headers.get("x-death"))),
                entry("exception", safeString(headers.get("x-exception-message"))),
                entry("stacktrace", safeString(headers.get("x-exception-stacktrace"))),
                entry("routingKey", safeString(message.getMessageProperties().getReceivedRoutingKey()))
        );
        slackService.sendMessage("쿠폰 사용 통계 FCM DLQ 적재", slackData);
    }
}
