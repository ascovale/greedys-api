package com.application.spring;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

@Configuration
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.host}")
    private String rabbitmqHost;

    @Value("${spring.rabbitmq.port}")
    private int rabbitmqPort;

    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUsername;

    @Bean
    public CachingConnectionFactory rabbitConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(rabbitmqHost, rabbitmqPort);
        factory.setUsername(rabbitmqUsername);

        // Read the password from the Docker secret file
        try (BufferedReader br = new BufferedReader(new FileReader("/run/secrets/rabbitmq_password"))) {
            String rabbitmqPassword = br.readLine();
            factory.setPassword(rabbitmqPassword);
        } catch (IOException e) {
            throw new RuntimeException("Errore durante la lettura della password di RabbitMQ", e);
        }

        factory.setVirtualHost("/"); // Set the virtual host if needed

        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

      @Bean
    Queue restaurantQueue() {
        return new Queue("RestaurantQueue", false);
    }

    @Bean
    Queue clientQueue() {
        return new Queue("ClientQueue", false);
    }

    @Bean
    Queue userEmailQueue() {
        return new Queue("userEmailQueue", false);
    }

    @Bean
    Queue reservationEmailQueue() {
        return new Queue("reservationEmailQueue", false);
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

    @Bean
    Binding userEmailBinding(Queue userEmailQueue, TopicExchange exchange) {
        return BindingBuilder.bind(userEmailQueue).to(exchange).with("userEmailRoutingKey");
    }

    @Bean
    Binding reservationEmailBinding(Queue reservationEmailQueue, TopicExchange exchange) {
        return BindingBuilder.bind(reservationEmailQueue).to(exchange).with("reservationEmailRoutingKey");
    }
}
