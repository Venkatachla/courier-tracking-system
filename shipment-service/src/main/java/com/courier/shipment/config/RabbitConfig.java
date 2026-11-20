package com.courier.shipment.config;

import com.courier.shipment.event.ShipmentEventPublisher;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    TopicExchange shipmentExchange() { return new TopicExchange(ShipmentEventPublisher.EXCHANGE, true, false); }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() { return new Jackson2JsonMessageConverter(); }
}
