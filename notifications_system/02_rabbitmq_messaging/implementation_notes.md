# RabbitMQ Messaging - Implementation Notes

## Spring AMQP Integration

### RabbitTemplate Configuration

```java
@Bean
public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setDefaultExchange("notifications.exchange");
    template.setDefaultRoutingKey("notification.restaurant");
    return template;
}
```

### Publishing Messages

```java
// From EventOutboxOrchestrator
rabbitTemplate.convertAndSend(queue, message);

// Equivalent to:
rabbitTemplate.convertAndSend(
    "notifications.exchange",      // exchange
    "notification.restaurant",     // routing key  
    message                        // object (converted to JSON)
);
```

### Message Converter

```java
@Bean
public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
}
```

Automatically converts:
- Java Map/Object → JSON string (on send)
- JSON string → Java Map/Object (on receive)

---

## Listener Configuration

### BaseNotificationListener<T>

Abstract base class for all 4 listeners:

```java
@Slf4j
public abstract class BaseNotificationListener<T extends ANotification> {
    
    @Transactional
    protected void processNotificationMessage(
        @Payload Map<String, Object> message,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        Channel channel
    ) {
        try {
            // 1. Parse & idempotency check
            String eventId = (String) message.get("event_id");
            if (existsByEventId(eventId)) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            // 2. Disaggregate
            NotificationOrchestrator<T> orchestrator = 
                getTypeSpecificOrchestrator(message);
            List<T> notifications = orchestrator.disaggregateAndProcess(message);
            
            // 3. Persist
            for (T notification : notifications) {
                persistNotification(notification);
            }
            
            // 4. ACK
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, true);  // requeue
            throw e;
        }
    }
}
```

### Concrete Listener Implementations

```java
@Component
@RabbitListener(queues = "notification.restaurant")
public class RestaurantNotificationListener 
    extends BaseNotificationListener<RestaurantUserNotification> {
    
    @RabbitHandler
    @Transactional
    public void handleMessage(
        @Payload Map<String, Object> message,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        Channel channel
    ) throws IOException {
        processNotificationMessage(message, deliveryTag, channel);
    }
    
    @Override
    protected NotificationOrchestrator<RestaurantUserNotification>
        getTypeSpecificOrchestrator(Map<String, Object> message) {
        return restaurantUserOrchestrator;
    }
    
    @Override
    protected boolean existsByEventId(String eventId) {
        return restaurantNotificationDAO.existsByEventId(eventId);
    }
    
    @Override
    protected void persistNotification(RestaurantUserNotification notification) {
        restaurantNotificationDAO.save(notification);
    }
}
```

Similar for Customer, Agency, Admin listeners.

---

## Queue & Exchange Declaration

### Using @Bean Methods

```java
@Configuration
public class RabbitConfiguration {
    
    // Exchanges
    @Bean
    public TopicExchange notificationsExchange() {
        return new TopicExchange("notifications.exchange", true, false);
    }
    
    // Queues
    @Bean
    public Queue restaurantQueue() {
        return new Queue("notification.restaurant", true);
    }
    
    @Bean
    public Queue customerQueue() {
        return new Queue("notification.customer", true);
    }
    
    // Bindings
    @Bean
    public Binding restaurantBinding(
        TopicExchange exchange,
        Queue restaurantQueue
    ) {
        return BindingBuilder
            .bind(restaurantQueue)
            .to(exchange)
            .with("notification.restaurant");
    }
}
```

### Alternative: Using Annotations

```java
@RabbitListener(
    bindings = @QueueBinding(
        value = @Queue(
            name = "notification.restaurant",
            durable = true
        ),
        exchange = @Exchange(
            name = "notifications.exchange",
            type = ExchangeTypes.TOPIC,
            durable = true
        ),
        key = "notification.restaurant"
    )
)
public void listen(Message message) { ... }
```

---

## Message Publishing Pattern

### Synchronous Publishing

```java
// In EventOutboxOrchestrator
rabbitTemplate.convertAndSend(queue, message);
```

**Behavior**:
- Blocks until message sent to broker
- Throws exception if broker unreachable
- No retry (caller must handle exception)

### Asynchronous Publishing

```java
ListenableFuture<SendResult<String>> future = 
    rabbitTemplate.convertAndSendAsyncWithCorrelationData(
        exchange,
        routingKey,
        message,
        new CorrelationData(eventId)
    );

future.addCallback(
    result -> log.info("✅ Message sent"),
    ex -> log.error("❌ Message failed", ex)
);
```

**Current Implementation**: Synchronous (simpler, sufficient for current throughput).

---

## Error Handling Strategies

### Automatic Requeue

```java
try {
    // Process message
} catch (Exception e) {
    channel.basicNack(deliveryTag, false, true);  // requeue=true
    throw e;  // Optional, for @Retryable
}
```

**Behavior**:
- Message returned to queue
- Attempted by next consumer
- No exponential backoff (immediate)
- Infinite retries (potential issue)

### Dead Letter Queue

```java
@Bean
public Queue dlq() {
    return new Queue("notification.dlq", true);
}

@Bean
public Queue mainQueue() {
    return QueueBuilder.durable("notification.restaurant")
        .deadLetterExchange("notifications.dlq.exchange")
        .deadLetterRoutingKey("notification.dlq")
        .build();
}
```

**Behavior**:
- After max retries, message → DLQ
- Admin can inspect, replay
- Prevents infinite loop

### @Retryable Configuration

```java
@Retryable(
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
protected void processNotificationMessage(...) {
    // Process message
}
```

**Behavior**:
- Retry on exception
- Exponential backoff: 1s, 2s, 4s
- After 3 attempts: exception propagates (NACK)

---

## Concurrency & Performance Tuning

### Consumer Concurrency

```properties
spring.rabbitmq.listener.simple.concurrency=1
spring.rabbitmq.listener.simple.max-concurrency=3
```

**Behavior**:
- concurrency=1: One consumer thread per listener
- max-concurrency=3: Can scale to 3 threads if needed

**Trade-off**:
- Higher concurrency: More throughput, more memory
- Lower concurrency: Simpler, less resource usage

### Prefetch Count

```properties
spring.rabbitmq.listener.simple.prefetch=1
```

**Behavior**:
- prefetch=1: Listener asks for 1 message at a time
- prefetch=10: Listener pulls 10 messages, processes sequentially

**Trade-off**:
- Higher prefetch: Better throughput, risk of message loss if listener crashes
- Lower prefetch: Fair distribution across listeners, lower throughput

### Channel Cache Size

```properties
spring.rabbitmq.connection-factory.channel-cache-size=10
```

**Behavior**: Cache 10 channel instances for connection pooling.

---

## Connection Management

### Connection Pool

```properties
spring.rabbitmq.connection-factory.cache-mode=CHANNEL
```

**Mode**: CHANNEL (reuse connections, create new channels)

**Alternatives**:
- CHANNEL: Default, efficient for many listeners
- CONNECTION: One connection per consumer (simpler, less efficient)

### Heartbeat Configuration

```properties
spring.rabbitmq.requested-heartbeat=60
```

**Behavior**: Heartbeat every 60 seconds (detect broken connections).

### Connection Timeout

```properties
spring.rabbitmq.connection-timeout=10000
```

**Behavior**: Timeout after 10 seconds if can't connect to broker.

---

## Monitoring & Debugging

### Log Points in BaseNotificationListener

```java
log.info("📩 Listener: received message");  // Entry
log.info("🔍 Processing event: eventId={}", eventId);  // Parsing
log.warn("⚠️ Duplicate eventId detected: {}", eventId);  // Idempotency
log.info("✅ Orchestrator disaggregated {} notifications", list.size());  // Disagg
log.info("✔️ Message ACK'd successfully");  // Success
log.error("❌ Error processing notification: {}", e.getMessage());  // Error
```

### RabbitMQ Admin Interface

**URL**: `http://localhost:15672` (default)

**Navigate to**:
- Queues tab → See queue depth
- Connections tab → See listener connections
- Channels tab → See message throughput

### Metrics Exposure

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
```

**Metrics Available**:
- `rabbitmq.messages.acked`
- `rabbitmq.messages.nacked`
- `rabbitmq.messages.failed`
- `rabbitmq.connections.opened`
- `rabbitmq.connections.closed`

---

## Deployment Checklist

- [ ] RabbitMQ broker running and accessible
- [ ] Queue & exchange declared (auto-created or manual)
- [ ] Spring AMQP dependencies in pom.xml
- [ ] @RabbitListener annotations on concrete listeners
- [ ] @Bean configurations for exchanges/queues (if using code-based approach)
- [ ] Application properties configured (host, port, auth)
- [ ] Error handling strategy chosen (requeue, DLQ, or retry)
- [ ] Monitoring setup (RabbitMQ admin UI or metrics)
- [ ] Load testing completed (target throughput validated)

---

**Document Version**: 1.0  
**Last Updated**: November 23, 2025  
**Component**: RabbitMQ Messaging
