# Notification Disaggregation - Functional Description  

## Purpose
Transform 1 generic message into N recipient-specific notification records by calculating which users should receive notifications and through which channels.

## Core Responsibility
Load recipients → Load preferences → Load rules → Calculate channels → Create records

## Key Components

### NotificationOrchestrator (Abstract Base)
Template method pattern base class defining the disaggregation algorithm:
- `disaggregateAndProcess()`: Main method, orchestrates the entire flow
- `loadRecipients()`: Get list of users who should receive
- `loadUserPreferences()`: Get enabled channels per user
- `loadGroupSettings()`: Get organization-level settings
- `loadEventTypeRules()`: Get rules per event type
- `createNotificationRecord()`: Create entity per recipient×channel

### 4 Concrete Orchestrators
1. **RestaurantUserOrchestrator**: For restaurant staff notifications
   - Loads staff members from RestaurantStaff table
   - Applies restaurant-specific rules (escalation, SMS to managers only)
   - Supports shared read for group notifications

2. **CustomerOrchestrator**: For customer notifications
   - Single customer as recipient (trivial)
   - Simpler channel calculation
   - No shared read (individual only)

3. **AgencyUserOrchestrator**: For agency staff notifications
   - Loads agency users
   - Priority-based routing (managers vs agents)
   - Urgent SMS delivery

4. **AdminOrchestrator**: For admin notifications
   - All system admins as recipients
   - Incident tracking for critical events
   - Slack integration for CRITICAL

### NotificationOrchestratorFactory
Type-safe factory pattern to select correct orchestrator:
- Extracts `aggregate_type` from message
- Maps to correct orchestrator implementation
- Eliminates string-based dispatch errors

## Functional Behavior

### 1. **Receive Message**
Generic message from RabbitMQ (1 message for all recipients)

### 2. **Extract Information**
- Event ID, type, aggregate type
- Recipient type from aggregate_type
- Payload with event details

### 3. **Load Recipients**
Query database for all users who should receive:
- RESTAURANT: Active staff of restaurant
- CUSTOMER: The specific customer
- AGENCY: Active staff of agency
- ADMIN: All system admins

### 4. **Per-Recipient Processing**
For each recipient:
- Load their notification preferences (which channels enabled)
- Load organization settings (defaults)
- Load event-type routing rules (mandatory/optional channels)
- Calculate intersection: Group ∩ User ∩ Event = final channels
- Create 1 notification record per final channel

### 5. **Return Records**
List of disaggregated notification entities ready to persist

---

## Disaggregation Example

### Input
```json
{
  "event_type": "RESERVATION_REQUESTED",
  "aggregate_type": "RESTAURANT",
  "restaurant_id": 5,
  "payload": {...}
}
```

### Processing
```
Restaurant 5 has 10 active staff:
  Staff1: preferences=[WEBSOCKET, EMAIL]
  Staff2: preferences=[WEBSOCKET, SMS]
  Staff3: preferences=[EMAIL]
  ...
  Staff10: preferences=[WEBSOCKET, EMAIL, PUSH]

Event rules for RESERVATION_REQUESTED:
  mandatory=[WEBSOCKET]
  optional=[EMAIL, PUSH]

Group settings for restaurant 5:
  enabled_channels=[WEBSOCKET, EMAIL, SMS, PUSH]

For each staff:
  Staff1: final = (mandatory + optional) ∩ user ∩ group
          = ([WEBSOCKET] + [EMAIL, PUSH]) ∩ [WEBSOCKET, EMAIL] ∩ [WEBSOCKET, EMAIL, SMS, PUSH]
          = [WEBSOCKET, EMAIL]
          → Create 2 records
  
  Staff2: = ([WEBSOCKET] + [EMAIL, PUSH]) ∩ [WEBSOCKET, SMS] ∩ [WEBSOCKET, EMAIL, SMS, PUSH]
          = [WEBSOCKET]
          → Create 1 record
  
  ... (continue for all 10 staff)

Total output: ~15-20 notification records
```

### Output
```java
List<RestaurantUserNotification> [
  {eventId: "evt-123-staff1-ws", userId: 1, channel: WEBSOCKET},
  {eventId: "evt-123-staff1-em", userId: 1, channel: EMAIL},
  {eventId: "evt-123-staff2-ws", userId: 2, channel: WEBSOCKET},
  ...
]
```

---

## Idempotency (Level 2)
- Notification table has UNIQUE(eventId)
- If listener reprocesses message: UNIQUE constraint prevents duplicate
- Duplicate insert caught by DataIntegrityViolationException
- Logged but not treated as error

---

## Performance Considerations
- **Queries per message**: 4-6 database queries (recipients, preferences, settings, rules)
- **Latency**: 50-200ms per message (depends on recipient count)
- **Complexity**: O(N recipients × M channels)

---

**Document Version**: 1.0  
**Last Updated**: November 23, 2025  
**Component**: Notification Disaggregation
