package com.courier.tracking.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.retry.policy.SimpleRetryPolicy;
import java.util.Map;
import com.courier.tracking.event.ShipmentEvent;

@Configuration
public class TrackingRabbitConfig {
    public static final String QUEUE = "tracking.shipment.events";
    public static final String DEAD_LETTER_EXCHANGE = "tracking.shipment.events.dlx";
    public static final String DEAD_LETTER_QUEUE = "tracking.shipment.events.dlq";
    public static final String SHIPMENT_EXCHANGE = "shipment.exchange";

    @Bean
    TopicExchange shipmentExchange() { return new TopicExchange(SHIPMENT_EXCHANGE, true, false); }

    @Bean
    DirectExchange trackingDeadLetterExchange() { return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false); }

    @Bean
    Queue trackingShipmentQueue() {
        return QueueBuilder.durable(QUEUE).deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Queue trackingShipmentDeadLetterQueue() { return QueueBuilder.durable(DEAD_LETTER_QUEUE).build(); }

    @Bean
    Binding shipmentEventsBinding(@Qualifier("trackingShipmentQueue") Queue trackingShipmentQueue, TopicExchange shipmentExchange) {
        return BindingBuilder.bind(trackingShipmentQueue).to(shipmentExchange).with("shipment.#");
    }

    @Bean
    Binding deadLetterBinding(@Qualifier("trackingShipmentDeadLetterQueue") Queue trackingShipmentDeadLetterQueue, DirectExchange trackingDeadLetterExchange) {
        return BindingBuilder.bind(trackingShipmentDeadLetterQueue).to(trackingDeadLetterExchange).with(DEAD_LETTER_QUEUE);
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper mapper = new DefaultJackson2JavaTypeMapper();
        mapper.setTrustedPackages("com.courier.tracking.event");
        mapper.setIdClassMapping(Map.of(
                "com.courier.shipment.event.ShipmentEvent", ShipmentEvent.class,
                "com.courier.tracking.event.ShipmentEvent", ShipmentEvent.class));
        converter.setJavaTypeMapper(mapper);
        return converter;
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(converter);
        factory.setDefaultRequeueRejected(false);
        SimpleRetryPolicy policy = new SimpleRetryPolicy(3, Map.of(
                AmqpRejectAndDontRequeueException.class, false,
                MessageConversionException.class, false,
                Exception.class, true), true);
        Advice retry = RetryInterceptorBuilder.stateless().retryPolicy(policy)
                .backOffOptions(500, 2.0, 5000)
                .recoverer(new RejectAndDontRequeueRecoverer()).build();
        factory.setAdviceChain(retry);
        return factory;
    }
}
