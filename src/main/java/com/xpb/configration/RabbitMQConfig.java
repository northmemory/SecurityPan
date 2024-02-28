package com.xpb.configration;


import cn.hutool.poi.excel.cell.CellSetter;
import com.alibaba.fastjson2.support.spring6.data.redis.GenericFastJsonRedisSerializer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@Slf4j
public class RabbitMQConfig {
    @Resource
    RedisTemplate redisTemplate;
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(Jackson2MessageConverter());
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                if(correlationData != null){
                    log.info("消息确认送到交换机(Exchange)，消息的唯一标识符：{}", correlationData.getId());
                }else {
                    log.info("消息发送成功");
                }
            } else {
                if (correlationData!=null){
                    Object o = redisTemplate.opsForValue().get(correlationData.getId());
                    log.error("Id为{}的消息：{}投递失败，失败原因为{}",correlationData.getId(),o,cause);
                    redisTemplate.delete(correlationData.getId());
                }
            }
        });
        return rabbitTemplate;
    }

    @Bean
    public Jackson2JsonMessageConverter Jackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /*
    * 消息失败处理*/
    @Bean
    public DirectExchange directExchange(){
        return new DirectExchange("error.direct");
    }

    @Bean
    public Queue errorQueue(){
        return new Queue("error.queue");
    }

    @Bean
    public Binding errorBinding(Queue errorQueue, DirectExchange directExchange){
        return BindingBuilder.bind(errorQueue).to(directExchange).with("error");
    }

    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate){
        return new RepublishMessageRecoverer(rabbitTemplate,"error.direct","error");
    }

}
