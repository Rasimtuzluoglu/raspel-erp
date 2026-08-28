package com.raspel.erp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ bildirim kuyruğu tanımları.
 * Ana kuyruk dead-letter exchange'e bağlıdır: işlenemeyen (poison) mesajlar
 * retry sayısı dolunca DLQ'ya düşer, sonsuz döngüye girmez.
 * Testlerde (app.rabbitmq.enabled=false) kapatılarak RabbitMQ bağımlılığı ortadan kaldırılır.
 */
@Configuration
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class RabbitMQConfig {

    public static final String BILDIRIM_QUEUE = "raspel.bildirim.queue";
    public static final String BILDIRIM_EXCHANGE = "raspel.bildirim.exchange";
    public static final String BILDIRIM_ROUTING_KEY = "raspel.bildirim.routing";

    public static final String BILDIRIM_DLX = "raspel.bildirim.dlx";
    public static final String BILDIRIM_DLQ = "raspel.bildirim.dlq";
    public static final String BILDIRIM_DLQ_ROUTING_KEY = "raspel.bildirim.dlq.routing";

    @Bean
    public Queue bildirimQueue() {
        return QueueBuilder.durable(BILDIRIM_QUEUE)
                .deadLetterExchange(BILDIRIM_DLX)
                .deadLetterRoutingKey(BILDIRIM_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue bildirimDlq() {
        return QueueBuilder.durable(BILDIRIM_DLQ).build();
    }

    @Bean
    public DirectExchange bildirimExchange() {
        return new DirectExchange(BILDIRIM_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange bildirimDlx() {
        return new DirectExchange(BILDIRIM_DLX, true, false);
    }

    @Bean
    public Binding bildirimBinding(Queue bildirimQueue, DirectExchange bildirimExchange) {
        return BindingBuilder.bind(bildirimQueue).to(bildirimExchange).with(BILDIRIM_ROUTING_KEY);
    }

    @Bean
    public Binding bildirimDlqBinding(Queue bildirimDlq, DirectExchange bildirimDlx) {
        return BindingBuilder.bind(bildirimDlq).to(bildirimDlx).with(BILDIRIM_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Publisher confirmations: mesaj broker tarafından kabul edilmediyse loglanır.
     * (spring.rabbitmq.publisher-confirm-type=correlated ile birlikte çalışır)
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.warn("RabbitMQ publish onaylanmadı (mesaj kuyruğa alınamadı): {}", cause);
            }
        });
        return template;
    }
}
