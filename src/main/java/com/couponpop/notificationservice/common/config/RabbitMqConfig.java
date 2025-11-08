package com.couponpop.notificationservice.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableRabbit
@Configuration
public class RabbitMqConfig {

    public static final int DEFAULT_CONCURRENT_CONSUMERS = 2;
    public static final int MAX_CONCURRENT_CONSUMERS = 10;
    public static final int PREFETCH_COUNT = 50;

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

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitListenerMessageConverter);
        factory.setConcurrentConsumers(DEFAULT_CONCURRENT_CONSUMERS); // 기본 동시 소비자 수
        factory.setMaxConcurrentConsumers(MAX_CONCURRENT_CONSUMERS);  // 최대 동시 소비자 수
        factory.setPrefetchCount(PREFETCH_COUNT);  // 한 번에 가져올 메시지 수
        factory.setDefaultRequeueRejected(false);  // 예외 시 재큐잉 여부 - nack(requeue=false) 설정
        return factory;
    }
}
