package com.application.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configurazione RabbitMQ per il sistema di notifiche
 * 
 * Architettura:
 * - EXCHANGE_NOTIFICATIONS (Topic Exchange): Routing delle notifiche per recipientType
 * - Code per ogni listener: QUEUE_EVENT, QUEUE_NOTIFICATION
 * 
 * @author Greedy's System
 * @since 2025-01-20
 */
@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.host")
public class RabbitMQConfig {

    // ============ EXCHANGE NAMES ============
    public static final String EXCHANGE_NOTIFICATIONS = "notifications.exchange";
    public static final String EXCHANGE_EVENTS = "events.exchange";
    
    // ============ QUEUE NAMES ============
    // EventOutbox → RabbitMQ queues (by user type)
    public static final String QUEUE_CUSTOMER = "notification.customer.queue";
    public static final String QUEUE_RESTAURANT = "notification.restaurant.queue";
    public static final String QUEUE_ADMIN = "notification.admin.queue";
    public static final String QUEUE_AGENCY = "notification.agency.queue";
    
    // NotificationOutbox → ChannelPoller queue
    public static final String QUEUE_CHANNEL_DISPATCH = "notification.channel.dispatch.queue";
    
    // Dead Letter Queue for failed messages
    public static final String DLQ = "notification.dlq";
    
    // ============ ROUTING KEYS ============
    public static final String ROUTING_KEY_CUSTOMER = "notification.customer.*";
    public static final String ROUTING_KEY_RESTAURANT = "notification.restaurant.*";
    public static final String ROUTING_KEY_ADMIN = "notification.admin.*";
    public static final String ROUTING_KEY_AGENCY = "notification.agency.*";
    
    // ============ TOPIC EXCHANGE ============
    @Bean
    public TopicExchange notificationsExchange() {
        return new TopicExchange(EXCHANGE_NOTIFICATIONS, true, false);
    }
    
    // ============ CUSTOMER QUEUE & BINDING ============
    @Bean
    public Queue customerQueue() {
        return new Queue(QUEUE_CUSTOMER, true, false, false);
    }
    
    @Bean
    public Binding customerBinding(Queue customerQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(customerQueue)
                .to(notificationsExchange)
                .with(ROUTING_KEY_CUSTOMER);
    }
    
    // ============ RESTAURANT QUEUE & BINDING ============
    @Bean
    public Queue restaurantQueue() {
        return new Queue(QUEUE_RESTAURANT, true, false, false);
    }
    
    @Bean
    public Binding restaurantBinding(Queue restaurantQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(restaurantQueue)
                .to(notificationsExchange)
                .with(ROUTING_KEY_RESTAURANT);
    }
    
    // ============ ADMIN QUEUE & BINDING ============
    @Bean
    public Queue adminQueue() {
        return new Queue(QUEUE_ADMIN, true, false, false);
    }
    
    @Bean
    public Binding adminBinding(Queue adminQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(adminQueue)
                .to(notificationsExchange)
                .with(ROUTING_KEY_ADMIN);
    }
    
    // ============ AGENCY QUEUE & BINDING ============
    @Bean
    public Queue agencyQueue() {
        return new Queue(QUEUE_AGENCY, true, false, false);
    }
    
    @Bean
    public Binding agencyBinding(Queue agencyQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(agencyQueue)
                .to(notificationsExchange)
                .with(ROUTING_KEY_AGENCY);
    }
    
    // ============ CHANNEL DISPATCH QUEUE ============
    @Bean
    public DirectExchange eventsExchange() {
        return new DirectExchange(EXCHANGE_EVENTS, true, false);
    }
    
    @Bean
    public Queue channelDispatchQueue() {
        return new Queue(QUEUE_CHANNEL_DISPATCH, true, false, false);
    }
    
    // ============ DEAD LETTER QUEUE ============
    @Bean
    public Queue dlq() {
        return new Queue(DLQ, true, false, false);
    }
}
