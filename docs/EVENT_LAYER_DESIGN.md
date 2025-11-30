# Event Layer Design - Greedys API

**Created:** November 30, 2025  
**Status:** Proposal  
**Related:** `docs/structure-review-greedys.md`

---

## 1. Overview

This document describes the event-driven notification architecture for Greedys API. The system uses a **4-layer approach** to ensure reliable message delivery across multiple channels (WebSocket, Push, Email, SMS).

---

## 2. Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                         EVENT LAYER ARCHITECTURE                                    │
│                                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────────────┐   │
│  │ LAYER 0: Domain Event Publisher                                              │   │
│  │ Location: core/service/event/                                                │   │
│  │                                                                              │   │
│  │  ReservationService                                                          │   │
│  │       │                                                                      │   │
│  │       ▼                                                                      │   │
│  │  DomainEventPublisher.publish(RESERVATION_CREATED, payload)                  │   │
│  │       │                                                                      │   │
│  │       ▼                                                                      │   │
│  │  ┌──────────────────────────────────────────────┐                           │   │
│  │  │ EventOutbox (DB Table)                       │                           │   │
│  │  │ - id, eventType, aggregateType, aggregateId  │                           │   │
│  │  │ - payload (JSON), status, createdAt          │                           │   │
│  │  └──────────────────────────────────────────────┘                           │   │
│  │       │ Same transaction as business logic                                   │   │
│  └───────┼─────────────────────────────────────────────────────────────────────┘   │
│          │                                                                          │
│  ┌───────┼─────────────────────────────────────────────────────────────────────┐   │
│  │ LAYER 1: Event Outbox Orchestrator                                          │   │
│  │ Location: core/service/notification/orchestrator/                           │   │
│  │       │                                                                      │   │
│  │       ▼                                                                      │   │
│  │  @Scheduled(fixedDelay = 1000ms)                                            │   │
│  │  EventOutboxOrchestrator.pollAndPublish()                                   │   │
│  │       │                                                                      │   │
│  │       ▼                                                                      │   │
│  │  ┌──────────────────────────────────────────────┐                           │   │
│  │  │ RabbitMQ Topic Exchange: greedys.events      │                           │   │
│  │  │                                              │                           │   │
│  │  │  Routing by aggregateType:                   │                           │   │
│  │  │  - RESERVATION → notification.restaurant    │                           │   │
│  │  │  - RESERVATION → notification.customer      │                           │   │
│  │  │  - AGENCY      → notification.agency        │                           │   │
│  │  │  - *           → notification.admin         │                           │   │
│  │  └──────────────────────────────────────────────┘                           │   │
│  └───────┼─────────────────────────────────────────────────────────────────────┘   │
│          │                                                                          │
│  ┌───────┼─────────────────────────────────────────────────────────────────────┐   │
│  │ LAYER 2: Notification Listeners                                              │   │
│  │ Location: core/service/notification/listener/                               │   │
│  │       │                                                                      │   │
│  │       ▼                                                                      │   │
│  │  @RabbitListener(queues = "notification.restaurant")                        │   │
│  │  RestaurantTeamNotificationListener.onEvent(payload)                        │   │
│  │       │                                                                      │   │
│  │       ▼                                                                      │   │
│  │  Disaggregation: 1 event → N notifications                                  │   │
│  │  (per recipient × per channel)                                              │   │
│  │       │                                                                      │   │
│  │       ├──→ Save to DB (NotificationOutbox)                                  │   │
│  │       │                                                                      │   │
│  │       └──→ WebSocket: SYNC delivery (immediate)                             │   │
│  │            stompTemplate.convertAndSend("/topic/user/{id}", notification)   │   │
│  └───────┼─────────────────────────────────────────────────────────────────────┘   │
│          │                                                                          │   
│  ┌───────┼─────────────────────────────────────────────────────────────────────┐   │
│  │ LAYER 3: Channel Pollers (Async delivery)                                   │   │
│  │ Location: core/service/notification/channel/                                │   │
│  │       │                                                                      │   │
│  │       ▼                                                                      │   │
│  │  ┌─────────────────────────────────────────────────────────────────────┐    │   │
│  │  │ ChannelPoller                                                       │    │   │
│  │  │                                                                     │    │   │
│  │  │  @Scheduled(fixedDelay = 30000)  // Email                          │    │   │
│  │  │  pollEmail() → EmailChannel.send(notification)                     │    │   │
│  │  │                                                                     │    │   │
│  │  │  @Scheduled(fixedDelay = 10000)  // Push                           │    │   │
│  │  │  pollPush() → PushChannel.send(notification)                       │    │   │
│  │  │                                                                     │    │   │
│  │  │  @Scheduled(fixedDelay = 60000)  // SMS                            │    │   │
│  │  │  pollSms() → SmsChannel.send(notification)                         │    │   │
│  │  │                                                                     │    │   │
│  │  │  WebSocket: NO POLLING - sync in Layer 2                           │    │   │
│  │  └─────────────────────────────────────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Timeline Diagram

```
Timeline: Reservation Created → Notifications Delivered

T0: ReservationService.saveNewReservation()
    │
    ├─ reservation = reservationDAO.save(reservation)
    │
    └─ eventPublisher.publish(RESERVATION_CREATED, payload)
       │
       └─ EventOutbox.save() ← Same DB transaction
    
T1: Transaction COMMIT (T0 + ~50ms)
    │
    └─ EventOutbox row visible to pollers

T2: EventOutboxOrchestrator.pollAndPublish() (T1 + 0-1000ms)
    │
    └─ rabbitTemplate.convertAndSend("greedys.events", routingKey, payload)

T3: RabbitMQ routes message (T2 + ~5ms)
    │
    ├─→ Queue: notification.restaurant
    └─→ Queue: notification.customer

T4: RestaurantTeamNotificationListener receives (T3 + ~10ms)
    │
    ├─ Creates N notifications (1 per staff member × channel)
    └─ Saves to NotificationOutbox

T5: WebSocket SYNC delivery (T4 + ~5ms)
    │
    └─ stompTemplate.convertAndSend() ← IMMEDIATE, no polling

T6: ChannelPoller.pollEmail() (T4 + 0-30000ms)
    │
    └─ EmailChannel.send() → SMTP

T7: ChannelPoller.pollPush() (T4 + 0-10000ms)
    │
    └─ PushChannel.send() → Firebase

T8: ChannelPoller.pollSms() (T4 + 0-60000ms)
    │
    └─ SmsChannel.send() → Twilio

TOTAL LATENCY:
- WebSocket: ~70ms (T0→T5)
- Push: ~80ms-10s (T0→T7)
- Email: ~80ms-30s (T0→T6)
- SMS: ~80ms-60s (T0→T8)
```

---

## 4. EventType Enum

**Location:** `common/domain/event/EventType.java` (existing)

```java
public enum EventType {
    // Reservation lifecycle
    RESERVATION_CREATED("reservation.created"),
    RESERVATION_STATUS_CHANGED("reservation.status_changed"),
    RESERVATION_MODIFIED("reservation.modified"),
    RESERVATION_CANCELLED("reservation.cancelled"),
    RESERVATION_REMINDER("reservation.reminder"),
    
    // Messages
    MESSAGE_SENT("message.sent"),
    MESSAGE_READ("message.read"),
    
    // Support
    SUPPORT_TICKET_CREATED("support.ticket_created"),
    SUPPORT_TICKET_UPDATED("support.ticket_updated"),
    
    // System
    CLIENT_CONNECTED("system.client_connected"),
    CLIENT_DISCONNECTED("system.client_disconnected");
    
    private final String routingKey;
    
    EventType(String routingKey) {
        this.routingKey = routingKey;
    }
    
    public String getRoutingKey() {
        return routingKey;
    }
}
```

---

## 5. EventPayload Records (Proposed)

**Location:** `core/domain/event/payload/`

### 5.1 Base Interface

```java
// core/domain/event/payload/EventPayload.java
public interface EventPayload {
    EventType eventType();
}
```

### 5.2 Reservation Payloads

```java
// core/domain/event/payload/reservation/ReservationCreatedPayload.java
public record ReservationCreatedPayload(
    Long reservationId,
    Long customerId,
    Long restaurantId,
    Integer partySize,
    LocalDateTime requestedDateTime,
    String customerName,
    String customerPhone,
    String customerEmail,
    String notes,
    Long tableId,
    String serviceName
) implements EventPayload {
    
    @Override
    public EventType eventType() {
        return EventType.RESERVATION_CREATED;
    }
    
    public static ReservationCreatedPayload from(Reservation r) {
        return new ReservationCreatedPayload(
            r.getId(),
            r.getCustomer().getId(),
            r.getRestaurant().getId(),
            r.getPax(),
            r.getReservationDateTime(),
            r.getCustomer().getFullName(),
            r.getCustomer().getPhoneNumber(),
            r.getCustomer().getEmail(),
            r.getNotes(),
            r.getTable() != null ? r.getTable().getId() : null,
            r.getService() != null ? r.getService().getName() : null
        );
    }
}
```

```java
// core/domain/event/payload/reservation/ReservationStatusChangedPayload.java
public record ReservationStatusChangedPayload(
    Long reservationId,
    Long restaurantId,
    Long customerId,
    String oldStatus,
    String newStatus,
    String reason,
    String rejectionReason,
    Long changedByUserId,
    String changedByUserType,
    LocalDateTime changedAt
) implements EventPayload {
    
    @Override
    public EventType eventType() {
        return EventType.RESERVATION_STATUS_CHANGED;
    }
}
```

---

## 6. DomainEventPublisher (Proposed)

**Location:** `core/service/event/`

### 6.1 Interface

```java
// core/service/event/DomainEventPublisher.java
public interface DomainEventPublisher {
    
    /**
     * Publish an event. Creates EventOutbox in the same transaction.
     * Checks company-level notification preferences before creating.
     */
    void publish(EventPayload payload);
    
    /**
     * Publish with explicit aggregate info (for cases where payload
     * doesn't contain enough routing information).
     */
    void publish(EventType eventType, String aggregateType, 
                 Long aggregateId, Object payload);
}
```

### 6.2 Implementation

```java
// core/service/event/DomainEventPublisherImpl.java
@Service
@Transactional(propagation = Propagation.MANDATORY)
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublisherImpl implements DomainEventPublisher {
    
    private final EventOutboxDAO eventOutboxDAO;
    private final ObjectMapper objectMapper;
    
    @Override
    public void publish(EventPayload payload) {
        String aggregateType = resolveAggregateType(payload);
        Long aggregateId = resolveAggregateId(payload);
        
        publish(payload.eventType(), aggregateType, aggregateId, payload);
    }
    
    @Override
    public void publish(EventType eventType, String aggregateType, 
                       Long aggregateId, Object payload) {
        try {
            EventOutbox outbox = EventOutbox.builder()
                .eventType(eventType.name())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId.toString())
                .payload(objectMapper.writeValueAsString(payload))
                .status(EventStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
            
            eventOutboxDAO.save(outbox);
            log.debug("Event published: {} for {}:{}", 
                     eventType, aggregateType, aggregateId);
                     
        } catch (JsonProcessingException e) {
            throw new EventPublishException("Failed to serialize payload", e);
        }
    }
    
    private String resolveAggregateType(EventPayload payload) {
        return switch (payload) {
            case ReservationCreatedPayload p -> "RESERVATION";
            case ReservationStatusChangedPayload p -> "RESERVATION";
            case SupportTicketPayload p -> "SUPPORT";
            default -> "UNKNOWN";
        };
    }
    
    private Long resolveAggregateId(EventPayload payload) {
        return switch (payload) {
            case ReservationCreatedPayload p -> p.reservationId();
            case ReservationStatusChangedPayload p -> p.reservationId();
            case SupportTicketPayload p -> p.ticketId();
            default -> 0L;
        };
    }
}
```

---

## 7. Usage Example

### Before (current code):

```java
@Transactional
public Reservation saveNewReservation(ReservationDTO dto) {
    Reservation reservation = mapper.toEntity(dto);
    reservation = reservationDAO.save(reservation);
    
    // Boilerplate for each event
    EventOutbox eventOutbox = new EventOutbox();
    eventOutbox.setEventType(EventType.RESERVATION_CREATED.name());
    eventOutbox.setAggregateType("reservation");
    eventOutbox.setAggregateId(reservation.getId().toString());
    eventOutbox.setPayload(buildPayloadJson(reservation));
    eventOutboxDAO.save(eventOutbox);
    
    return reservation;
}
```

### After (with DomainEventPublisher):

```java
@Transactional
public Reservation saveNewReservation(ReservationDTO dto) {
    Reservation reservation = mapper.toEntity(dto);
    reservation = reservationDAO.save(reservation);
    
    // One line, type-safe, preferences handled automatically
    eventPublisher.publish(ReservationCreatedPayload.from(reservation));
    
    return reservation;
}
```

---

## 8. Existing Components Location

| Component | Current Path | Description |
|-----------|--------------|-------------|
| `EventType.java` | `common/domain/event/` | Enum of event types |
| `EventOutbox.java` | `common/persistence/model/notification/` | Outbox entity |
| `EventOutboxDAO.java` | `common/persistence/dao/notification/` | Outbox repository |
| `EventOutboxOrchestrator.java` | `common/service/notification/orchestrator/` | Layer 1 poller |
| `RestaurantTeamOrchestrator.java` | `common/service/notification/orchestrator/` | Layer 2 listener |
| `CustomerOrchestrator.java` | `common/service/notification/orchestrator/` | Layer 2 listener |
| `AdminOrchestrator.java` | `common/service/notification/orchestrator/` | Layer 2 listener |
| `AgencyUserOrchestrator.java` | `common/service/notification/orchestrator/` | Layer 2 listener |

---

## 9. RabbitMQ Configuration

**Queues:**
- `notification.restaurant` - Restaurant staff notifications
- `notification.customer` - Customer notifications  
- `notification.agency` - Agency user notifications
- `notification.admin` - Admin notifications

**Exchange:** `greedys.events` (Topic Exchange)

**Bindings:**
```
RESERVATION.* → notification.restaurant
RESERVATION.* → notification.customer
AGENCY.*      → notification.agency
#             → notification.admin (receives all)
```

---

## 10. WebSocket Delivery

**Key Point:** WebSocket delivery is **SYNCHRONOUS**, not polled.

When a notification is created in Layer 2, if the recipient is connected via WebSocket:

```java
// In RestaurantTeamNotificationListener
@RabbitListener(queues = "notification.restaurant")
public void onReservationEvent(String payload) {
    // Create notifications
    List<Notification> notifications = createNotifications(payload);
    
    for (Notification n : notifications) {
        notificationDAO.save(n);
        
        // SYNC WebSocket delivery - immediate, no polling
        if (isUserConnected(n.getRecipientId())) {
            stompTemplate.convertAndSend(
                "/topic/user/" + n.getRecipientId(),
                notificationMapper.toDTO(n)
            );
            n.setWebSocketDelivered(true);
        }
    }
}
```

---

## 11. Implementation Phases

### Phase 1: Create EventPayload Records
1. Create `core/domain/event/payload/` package
2. Create `EventPayload` interface
3. Create payload records for each event type

### Phase 2: Create DomainEventPublisher
1. Create `core/service/event/` package
2. Create `DomainEventPublisher` interface
3. Create `DomainEventPublisherImpl`

### Phase 3: Migrate Existing Code
1. Inject `DomainEventPublisher` into services
2. Replace manual EventOutbox creation with `publish()` calls
3. Remove boilerplate code

### Phase 4: Add Company Preferences (Optional)
1. Create `NotificationGroupSettings` entity
2. Add preference checking to `DomainEventPublisher`
3. Allow company-level notification configuration

---

## 12. Benefits

| Aspect | Before | After |
|--------|--------|-------|
| **Type Safety** | JSON string, runtime errors | Compile-time type checking |
| **Boilerplate** | 5-6 lines per event | 1 line |
| **IDE Support** | No autocomplete for payload | Full autocomplete |
| **Refactoring** | Risky, find/replace | Safe, compiler catches issues |
| **Documentation** | In comments | In code (record fields) |
| **Testing** | Parse JSON in tests | Direct record assertions |

---

*Document created for Greedys API event layer architecture proposal.*
