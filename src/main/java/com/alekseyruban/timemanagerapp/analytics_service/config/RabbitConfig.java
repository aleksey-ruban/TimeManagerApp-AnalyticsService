package com.alekseyruban.timemanagerapp.analytics_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String USER_EVENTS_EXCHANGE = "user.events";
    public static final String USER_CREATED_QUEUE = "analytics.user.created";
    public static final String USER_CREATED_KEY = "user.created";

    public static final String CHRONOMETRY_EVENTS_EXCHANGE = "chronometry.events";
    public static final String CHRONOMETRY_CREATED_QUEUE = "analytics.chronometry.created";
    public static final String CHRONOMETRY_CREATED_KEY = "chronometry.created";

    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(USER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue userCreatedQueue() {
        return new Queue(USER_CREATED_QUEUE, true);
    }

    @Bean
    public Binding userCreatedBinding() {
        return BindingBuilder.bind(userCreatedQueue())
                .to(userEventsExchange())
                .with(USER_CREATED_KEY);
    }

    @Bean
    public TopicExchange chronometryEventsExchange() {
        return new TopicExchange(CHRONOMETRY_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue chronometryCreatedQueue() {
        return new Queue(CHRONOMETRY_CREATED_QUEUE, true);
    }

    @Bean
    public Binding chronometryCreatedBinding() {
        return BindingBuilder.bind(chronometryCreatedQueue())
                .to(chronometryEventsExchange())
                .with(CHRONOMETRY_CREATED_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
