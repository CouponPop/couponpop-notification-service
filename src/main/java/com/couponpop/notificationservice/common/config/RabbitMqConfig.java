package com.couponpop.notificationservice.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;

import java.util.HashMap;
import java.util.Map;

@EnableRabbit
@Configuration
public class RabbitMqConfig {

    private static final int MAX_ATTEMPTS = 5; // 최대 재시도 횟수
    private static final int BACKOFF_INITIAL_INTERVAL = 1_000; // 1 second
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final int BACKOFF_MAX_INTERVAL = 30_000; // 30 seconds
    private static final int DEFAULT_CONCURRENT_CONSUMERS = 2;
    private static final int MAX_CONCURRENT_CONSUMERS = 10;
    private static final int PREFETCH_COUNT = 50;

    @Bean
    public MessageConverter rabbitListenerMessageConverter(ObjectMapper objectMapper) {

        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * RabbitListenerContainerFactory 설정
     *
     * <ul>
     *     <li>{@code @RabbitListener}는 기본적으로 {@code containerFactory = "rabbitListenerContainerFactory"}를 찾기 때문에 적용됨</li>
     *     <li>메서드명이 다를 경우 {@code @RabbitListener(containerFactory = "customFactoryName")}와 같이 명시해야 함</li>
     * </ul>
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitListenerMessageConverter
    ) {

        // AmqpRejectAndDontRequeueException은 즉시 DLQ로 보내도록 재시도 대상에서 제외
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(AmqpRejectAndDontRequeueException.class, false);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                MAX_ATTEMPTS, // 최대 재시도 횟수
                retryableExceptions, // 재시도 대상 예외 설정
                true // 나머지 예외는 재시도 대상
        );

        // 재시도 인터셉터 설정
        RetryOperationsInterceptor retryAdvice = RetryInterceptorBuilder.stateless()
                // 재시도 정책 설정
                .retryPolicy(retryPolicy)
                // 지수 백오프 기반 재시도 설정 (1초, 2초, 4초, 8초, ...)
                .backOffOptions(BACKOFF_INITIAL_INTERVAL, BACKOFF_MULTIPLIER, BACKOFF_MAX_INTERVAL)
                // RetryInterceptor가 최대 재시도 횟수를 모두 소진했을 때 호출되는 복구 전략 (재큐잉하지 않고 바로 DLX로 보냄)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitListenerMessageConverter);
        factory.setConcurrentConsumers(DEFAULT_CONCURRENT_CONSUMERS); // 기본 동시 소비자 수
        factory.setMaxConcurrentConsumers(MAX_CONCURRENT_CONSUMERS);  // 최대 동시 소비자 수
        factory.setPrefetchCount(PREFETCH_COUNT);  // 한 번에 가져올 메시지 수
        factory.setDefaultRequeueRejected(false);  // 예외 시 재큐잉 여부 - nack(requeue=false) 설정
        factory.setAdviceChain(retryAdvice);
        return factory;
    }
}
