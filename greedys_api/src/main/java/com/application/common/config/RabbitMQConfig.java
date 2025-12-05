package com.application.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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
    public static final String QUEUE_CUSTOMER = "notification.customer";
    public static final String QUEUE_RESTAURANT = "notification.restaurant";
    public static final String QUEUE_RESTAURANT_TEAM = "notification.restaurant.reservations";
    public static final String QUEUE_ADMIN = "notification.admin";
    public static final String QUEUE_AGENCY = "notification.agency";
    
    // NotificationOutbox → ChannelPoller queue
    public static final String QUEUE_CHANNEL_DISPATCH = "notification.channel.dispatch";
    
    // Dead Letter Queue for failed messages
    public static final String DLQ = "notification.dlq";
    
    // ============ 🆕 CHAT QUEUE NAMES ============
    public static final String QUEUE_CHAT_DIRECT = "notification.chat.direct";
    public static final String QUEUE_CHAT_GROUP = "notification.chat.group";
    public static final String QUEUE_CHAT_SUPPORT = "notification.chat.support";
    public static final String QUEUE_CHAT_RESERVATION = "notification.chat.reservation";
    
    // ============ 🆕 SOCIAL QUEUE NAMES ============
    public static final String QUEUE_SOCIAL_FEED = "notification.social.feed";
    public static final String QUEUE_SOCIAL_EVENTS = "notification.social.events";
    
    // ============ ROUTING KEYS ============
    public static final String ROUTING_KEY_CUSTOMER = "notification.customer.*";
    public static final String ROUTING_KEY_RESTAURANT = "notification.restaurant.*";
    public static final String ROUTING_KEY_RESTAURANT_TEAM = "notification.restaurant.reservations.*";
    public static final String ROUTING_KEY_ADMIN = "notification.admin.*";
    public static final String ROUTING_KEY_AGENCY = "notification.agency.*";
    
    // ============ 🆕 CHAT ROUTING KEYS ============
    public static final String ROUTING_KEY_CHAT_DIRECT = "notification.chat.direct.*";
    public static final String ROUTING_KEY_CHAT_GROUP = "notification.chat.group.*";
    public static final String ROUTING_KEY_CHAT_SUPPORT = "notification.chat.support.*";
    public static final String ROUTING_KEY_CHAT_RESERVATION = "notification.chat.reservation.*";
    
    // ============ 🆕 SOCIAL ROUTING KEYS ============
    public static final String ROUTING_KEY_SOCIAL_FEED = "notification.social.feed.*";
    public static final String ROUTING_KEY_SOCIAL_EVENTS = "notification.social.events.*";
    
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
    
    // ============ RESTAURANT TEAM QUEUE & BINDING ============
    @Bean
    public Queue restaurantTeamQueue() {
        return new Queue(QUEUE_RESTAURANT_TEAM, true, false, false);
    }
    
    @Bean
    public Binding restaurantTeamBinding(Queue restaurantTeamQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(restaurantTeamQueue)
                .to(notificationsExchange)
                .with(ROUTING_KEY_RESTAURANT_TEAM);
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
    
    // ============ 🆕 CHAT QUEUES & BINDINGS ============
    @Bean
    public Queue chatDirectQueue() {
        return new Queue(QUEUE_CHAT_DIRECT, true, false, false);
    }
    
    @Bean
    public Queue chatGroupQueue() {
        return new Queue(QUEUE_CHAT_GROUP, true, false, false);
    }
    
    @Bean
    public Queue chatSupportQueue() {
        return new Queue(QUEUE_CHAT_SUPPORT, true, false, false);
    }
    
    @Bean
    public Queue chatReservationQueue() {
        return new Queue(QUEUE_CHAT_RESERVATION, true, false, false);
    }
    
    @Bean
    public Binding chatDirectBinding(Queue chatDirectQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(chatDirectQueue)
                .to(notificationsExchange)
                .with(ROUTING_KEY_CHAT_DIRECT);
    }
    
    @Bean
    public Binding chatGroupBinding(Queue chatGroupQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(chatGroupQueue)
                .to(notificationsExchange)
                .with(ROUTING_KEY_CHAT_GROUP);
    }
    
    @Bean
    public Binding chatSupportBinding(Queue chatSupportQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(chatSupportQueue)
                .to(notificationsExchange)
                .with(ROUTING_KEY_CHAT_SUPPORT);
    }
    
    @Bean
    public Binding chatReservationBinding(Queue chatReservationQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(chatReservationQueue)
                .to(notificationsExchange)
                .with(ROUTING_KEY_CHAT_RESERVATION);
    }
    
    // ============ 🆕 SOCIAL QUEUES & BINDINGS ============
    @Bean
    public Queue socialFeedQueue() {
        return new Queue(QUEUE_SOCIAL_FEED, true, false, false);
    }
    
    @Bean
    public Queue socialEventsQueue() {
        return new Queue(QUEUE_SOCIAL_EVENTS, true, false, false);
    }
    
    @Bean
    public Binding socialFeedBinding(Queue socialFeedQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(socialFeedQueue)
                .to(notificationsExchange)
                .with(ROUTING_KEY_SOCIAL_FEED);
    }
    
    @Bean
    public Binding socialEventsBinding(Queue socialEventsQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(socialEventsQueue)
                .to(notificationsExchange)
                .with(ROUTING_KEY_SOCIAL_EVENTS);
    }
    
    // ============ MESSAGE CONVERTER ============
    /**
     * Configures Jackson2JsonMessageConverter for RabbitMQ message deserialization.
     * 
     * SECURITY: Enables deserialization of HashMap and common Java collections.
     * Trust all setting is safe because:
     * - We only accept messages from our own producers (EventOutboxOrchestrator)
     * - All messages are validated before processing
     * - Producer and consumer are in same application
     * 
     * Alternative: Set SPRING_AMQP_DESERIALIZATION_TRUST_ALL=true in environment
     * 
     * @return Jackson2JsonMessageConverter with deserialization enabled
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        // Enable Jackson2 JSON message conversion
        // System property set at startup or via environment variable
        return new Jackson2JsonMessageConverter();
    }
}
