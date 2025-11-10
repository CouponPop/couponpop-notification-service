package com.couponpop.notificationservice.common.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.couponpop.couponpopcoremodule.constants.RabbitMqArguments.*;
import static com.couponpop.couponpopcoremodule.constants.RabbitMqExchanges.COUPON_EXCHANGE;

@Configuration
public class CouponUsageStatsFcmSendConsumerConfig {

    public static final String COUPON_USAGE_STATS_FCM_SEND_QUEUE = "coupon.usage.stats.fcm.send.queue";
    public static final int COUPON_USAGE_STATS_FCM_SEND_QUEUE_TTL_SECONDS = 300; // 5 minutes
    public static final String COUPON_USAGE_STATS_FCM_SEND_BINDING_KEY = "coupon.usage.stats.fcm.send";

    public static final String COUPON_USAGE_STATS_FCM_SEND_DLX = COUPON_EXCHANGE + ".dlx";
    public static final String COUPON_USAGE_STATS_FCM_SEND_DLQ = "coupon.usage.stats.fcm.send.dlq";
    public static final int COUPON_USAGE_STATS_FCM_SEND_DLQ_TTL_SECONDS = 60 * 60 * 24 * 7; // 7 days
    public static final String COUPON_USAGE_STATS_FCM_SEND_DLQ_BINDING_KEY = "coupon.usage.stats.fcm.send.dlq";

    // Exchange
    @Bean
    public TopicExchange couponUsageStatsFcmSendExchange() {

        return new TopicExchange(COUPON_EXCHANGE);
    }

    // Queue
    @Bean
    public Queue couponUsageStatsFcmSendQueue() {

        Map<String, Object> args = new HashMap<>();
        args.put(X_MESSAGE_TTL, TimeUnit.SECONDS.toMillis(COUPON_USAGE_STATS_FCM_SEND_QUEUE_TTL_SECONDS)); // 메시지 TTL 설정
        args.put(X_DEAD_LETTER_EXCHANGE, COUPON_USAGE_STATS_FCM_SEND_DLX); // DLX 설정
        args.put(X_DEAD_LETTER_ROUTING_KEY, COUPON_USAGE_STATS_FCM_SEND_DLQ_BINDING_KEY); // DLQ 라우팅 키 설정

        return QueueBuilder
                .durable(COUPON_USAGE_STATS_FCM_SEND_QUEUE) // Queue 디스크 저장 (재시작 후에도 유지)
                .withArguments(args)
                .build();
    }

    // Binding
    @Bean
    public Binding couponUsageStatsFcmSendBinding(
            TopicExchange couponUsageStatsFcmSendExchange,
            Queue couponUsageStatsFcmSendQueue
    ) {

        return BindingBuilder.bind(couponUsageStatsFcmSendQueue)
                .to(couponUsageStatsFcmSendExchange)
                .with(COUPON_USAGE_STATS_FCM_SEND_BINDING_KEY);
    }

    // DLX
    @Bean
    public TopicExchange couponUsageStatsFcmSendDlx() {

        return new TopicExchange(COUPON_USAGE_STATS_FCM_SEND_DLX);
    }

    // DLQ
    @Bean
    public Queue couponUsageStatsFcmSendDlq() {

        Map<String, Object> args = new HashMap<>();
        args.put(X_MESSAGE_TTL, TimeUnit.SECONDS.toMillis(COUPON_USAGE_STATS_FCM_SEND_DLQ_TTL_SECONDS)); // DLQ 메시지 TTL 설정

        return QueueBuilder
                .durable(COUPON_USAGE_STATS_FCM_SEND_DLQ)
                .withArguments(args)
                .build();
    }

    // DLQ Binding
    @Bean
    public Binding couponUsageStatsFcmSendDlqBinding(
            TopicExchange couponUsageStatsFcmSendDlx,
            Queue couponUsageStatsFcmSendDlq
    ) {

        return BindingBuilder.bind(couponUsageStatsFcmSendDlq)
                .to(couponUsageStatsFcmSendDlx)
                .with(COUPON_USAGE_STATS_FCM_SEND_DLQ_BINDING_KEY);
    }
}
