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
public class CouponIssuedConsumerConfig {

    public static final String COUPON_ISSUED_QUEUE = "coupon.queue.issued";
    public static final int COUPON_ISSUED_QUEUE_TTL_SECONDS = 300;
    public static final String COUPON_ISSUED_ROUTING_KEY = "coupon.issued";

    public static final String COUPON_ISSUED_DLX = COUPON_EXCHANGE + ".dlx";

    public static final String COUPON_ISSUED_DLQ = "coupon.queue.issued.dlq";
    public static final int COUPON_ISSUED_DLQ_SECONDS = 60 * 60 * 24 * 7;
    public static final String COUPON_ISSUED_DLQ_ROUTING_KEY = "coupon.issued.dlq";

    /**
     * Queue
     */
    @Bean
    public Queue issuedCouponQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put(X_MESSAGE_TTL, TimeUnit.SECONDS.toMillis(COUPON_ISSUED_QUEUE_TTL_SECONDS)); // 메시지 TTL 설정
        args.put(X_DEAD_LETTER_EXCHANGE, COUPON_ISSUED_DLX); // DLX 설정
        args.put(X_DEAD_LETTER_ROUTING_KEY, COUPON_ISSUED_DLQ_ROUTING_KEY); // DLQ 라우팅 키 설정

        return QueueBuilder
                .durable(COUPON_ISSUED_QUEUE) // Queue 디스크 저장 (재시작 후에도 유지)
                .withArguments(args)
                .build();
    }

    /**
     * Binding
     */
    @Bean
    public Binding issuedCouponBinding(TopicExchange couponExchange, Queue issuedCouponQueue) {
        return BindingBuilder
                .bind(issuedCouponQueue)
                .to(couponExchange)
                .with(COUPON_ISSUED_ROUTING_KEY);
    }

    /**
     * Dead Letter Exchange
     */
    @Bean
    TopicExchange issuedCouponDlx() {
        return new TopicExchange(COUPON_ISSUED_DLX);
    }

    /**
     * Dead Letter Queue
     */
    @Bean
    public Queue issuedCouponDlq() {
        Map<String, Object> args = new HashMap<>();
        args.put(X_MESSAGE_TTL, TimeUnit.SECONDS.toMillis(COUPON_ISSUED_DLQ_SECONDS)); // DLQ 메시지 TTL 설정

        return QueueBuilder
                .durable(COUPON_ISSUED_DLQ)
                .withArguments(args)
                .build();
    }

    /**
     * DLQ Binding
     */
    @Bean
    public Binding issuedCouponDLQBinding(Queue issuedCouponDlq, TopicExchange issuedCouponDlx) {
        return BindingBuilder
                .bind(issuedCouponDlq)
                .to(issuedCouponDlx)
                .with(COUPON_ISSUED_DLQ_ROUTING_KEY);
    }

}
