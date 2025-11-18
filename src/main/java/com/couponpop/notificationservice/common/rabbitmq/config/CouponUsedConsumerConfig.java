package com.couponpop.notificationservice.common.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.couponpop.couponpopcoremodule.constants.RabbitMqArguments.*;
import static com.couponpop.couponpopcoremodule.constants.RabbitMqExchanges.COUPON_EXCHANGE;


@EnableRabbit
@Configuration
public class CouponUsedConsumerConfig {

    public static final String COUPON_USED_QUEUE = "coupon.queue.used";
    public static final int COUPON_USED_QUEUE_TTL_SECONDS = 300;
    public static final String COUPON_USED_ROUTING_KEY = "coupon.used";

    public static final String COUPON_USED_DLX = COUPON_EXCHANGE + ".dlx";

    public static final String COUPON_USED_DLQ = "coupon.queue.used.dlq";
    public static final int COUPON_USED_DLQ_SECONDS = 60 * 60 * 24 * 7;
    public static final String COUPON_USED_DLQ_ROUTING_KEY = "coupon.used.dlq";

    /**
     * Exchange
     */
    @Bean
    TopicExchange couponExchange() {
        return new TopicExchange(COUPON_EXCHANGE);
    }


    /**
     * Queue
     */
    @Bean
    public Queue usedCouponQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put(X_MESSAGE_TTL, TimeUnit.SECONDS.toMillis(COUPON_USED_QUEUE_TTL_SECONDS)); // 메시지 TTL 설정
        args.put(X_DEAD_LETTER_EXCHANGE, COUPON_USED_DLX); // DLX 설정
        args.put(X_DEAD_LETTER_ROUTING_KEY, COUPON_USED_DLQ_ROUTING_KEY); // DLQ 라우팅 키 설정

        return QueueBuilder
                .durable(COUPON_USED_QUEUE) // Queue 디스크 저장 (재시작 후에도 유지)
                .withArguments(args)
                .build();
    }

    /**
     * Binding
     */
    @Bean
    public Binding usedCouponBinding(TopicExchange couponExchange, Queue usedCouponQueue) {
        return BindingBuilder
                .bind(usedCouponQueue)
                .to(couponExchange)
                .with(COUPON_USED_ROUTING_KEY);
    }

    /**
     * Dead Letter Exchange
     */
    @Bean
    TopicExchange usedCouponDlx() {
        return new TopicExchange(COUPON_USED_DLX);
    }

    /**
     * Dead Letter Queue
     */
    @Bean
    public Queue usedCouponDlq() {
        Map<String, Object> args = new HashMap<>();
        args.put(X_MESSAGE_TTL, TimeUnit.SECONDS.toMillis(COUPON_USED_DLQ_SECONDS)); // DLQ 메시지 TTL 설정

        return QueueBuilder
                .durable(COUPON_USED_DLQ)
                .withArguments(args)
                .build();
    }

    /**
     * DLQ Binding
     */
    @Bean
    public Binding usedCouponDLQBinding(Queue usedCouponDlq, TopicExchange usedCouponDlx) {
        return BindingBuilder
                .bind(usedCouponDlq)
                .to(usedCouponDlx)
                .with(COUPON_USED_DLQ_ROUTING_KEY);
    }

}
