package com.xpb.configration;


import com.alibaba.fastjson2.support.spring6.data.redis.GenericFastJsonRedisSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitMQConfig {
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(Jackson2MessageConverter());
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                assert correlationData != null;
                log.info("消息确认送到交换机(Exchange)，消息的唯一标识符：{}", correlationData.getId());
            } else {
                log.info("投递失败，错误原因 ：{}", cause);
            }
        });
        return rabbitTemplate;
    }

    @Bean
    public Jackson2JsonMessageConverter Jackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}
