package com.raspel.erp.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ bildirim kuyruğu tanımları.
 * Bildirimler bu kuyruk üzerinden asenkron taşınır ve tüketici tarafında işlenir.
 * Testlerde (app.rabbitmq.enabled=false) kapatılarak RabbitMQ bağımlılığı ortadan kaldırılır.
 */
@Configuration
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    public static final String BILDIRIM_QUEUE = "raspel.bildirim.queue";
    public static final String BILDIRIM_EXCHANGE = "raspel.bildirim.exchange";
    public static final String BILDIRIM_ROUTING_KEY = "raspel.bildirim.routing";

    @Bean
    public Queue bildirimQueue() {
        return QueueBuilder.durable(BILDIRIM_QUEUE).build();
    }

    @Bean
    public DirectExchange bildirimExchange() {
        return new DirectExchange(BILDIRIM_EXCHANGE, true, false);
    }

    @Bean
    public Binding bildirimBinding(Queue bildirimQueue, DirectExchange bildirimExchange) {
        return BindingBuilder.bind(bildirimQueue).to(bildirimExchange).with(BILDIRIM_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
