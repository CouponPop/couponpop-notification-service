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
public class RabbitListenerConfig {

    @Bean
    public MessageConverter rabbitListenerMessageConverter(ObjectMapper objectMapper) {

        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitListenerMessageConverter
    ) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitListenerMessageConverter);
        factory.setConcurrentConsumers(2); // 기본 동시 소비자 수
        factory.setMaxConcurrentConsumers(10);  // 최대 동시 소비자 수
        factory.setPrefetchCount(50);  // 한 번에 가져올 메시지 수
        factory.setDefaultRequeueRejected(false);  // 예외 시 재큐잉 여부
        return factory;
    }
}
