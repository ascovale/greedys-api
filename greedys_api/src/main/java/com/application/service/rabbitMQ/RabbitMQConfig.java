package com.application.service.rabbitMQ;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfig {

    @Bean
    Queue restaurantQueue() {
        return new Queue("RestaurantQueue", false);
    }

    @Bean
    Queue clientQueue() {
        return new Queue("ClientQueue", false);
    }

    @Bean
    TopicExchange exchange() {
        return new TopicExchange("ExchangeName");
    }

    @Bean
    Binding restaurantBinding(Queue restaurantQueue, TopicExchange exchange) {
        return BindingBuilder.bind(restaurantQueue).to(exchange).with("RestaurantRoutingKey");
    }

    @Bean
    Binding clientBinding(Queue clientQueue, TopicExchange exchange) {
        return BindingBuilder.bind(clientQueue).to(exchange).with("ClientRoutingKey");
    }
}

