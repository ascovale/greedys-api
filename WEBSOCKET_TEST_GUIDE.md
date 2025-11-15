# WebSocket Real-Time Reservations Test Guide

## Overview
This guide explains how to test the WebSocket real-time reservation system where restaurant staff (test@test.it) receives instant notifications when customers create reservations.

## System Architecture

### Flow Diagram
```
Customer Creates Reservation
    ↓
event_outbox (L0) ← Event published
    ↓
ReservationEventListener ← RabbitMQ listener
    ↓
ReservationWebSocketPublisher.publishReservationCreated()
    ↓
/topic/restaurants/{restaurantId}/reservations ← STOMP Topic
    ↓
All connected Restaurant Staff receive WebSocket message INSTANTLY
```

### 3-Level Outbox Pattern
- **L0 (event_outbox)**: Domain event when reservation is created
- **L1 (notification_outbox)**: Notification created by listener
- **L2 (notification_channel_send)**: WebSocket message for this specific channel

## Prerequisites

### 1. Ensure Services Are Running

```bash
# Check Docker containers
docker-compose ps

# You should see:
- mysql (database)
- rabbitmq (event bus)
- redis (caching)
- api (Spring Boot application)
```

### 2. Create Test Customers

First, you need customer accounts that can create reservations:

**Postman Collection**: `Customer-API-Collection.json`

Steps:
1. Set environment variables (baseUrl, customerEmail, customerPassword)
2. Run "Register New Customer"
3. Run "Admin Login"
4. Run "Enable Customer"
5. Run "Customer Login" → saves `customerToken` to environment

### 3. Verify Restaurant test@test.it Exists

Use the Postman request:
- GET `/customer/restaurant/all`

Should return a restaurant with `email: "test@test.it"` and its `id`.

## Testing WebSocket Real-Time Updates

### Step 1: Import Test Collection

**File**: `Customer-Reservations-WebSocket-Test.json`

This collection includes:
- Setup requests to find restaurant and slots
- 3 customer reservation creation requests
- Verification request

### Step 2: Run Setup Requests (In Order)

```
1. Setup - Get Restaurant Test@Test.It
   ↓ (Extracts: restaurantId, restaurantName)
   
2. Setup - Get Services for Restaurant
   ↓ (Extracts: serviceId)
   
3. Setup - Get Time Slots for Today
   ↓ (Extracts: slotId)
```

These setup requests will automatically extract and store:
- `restaurantId` → Used in all subsequent requests
- `serviceId` → Service type (e.g., "Dinner")
- `slotId` → Available time slot

### Step 3: Create Reservations from 3 Different Customers

Run these requests in sequence:

```
1. Customer 1 - Create Reservation (Marco Rossi)
2. Customer 2 - Create Reservation (Luca Bianchi)
3. Customer 3 - Create Reservation (Giulia Neri)
```

Each will create a PENDING reservation that should trigger a WebSocket event.

### Step 4: Connect to WebSocket as Restaurant Staff

In **Postman WebSocket Client**:

```
URL: ws://localhost:8080/ws

Subscribe to topic:
/user/{{restaurantUserId}}/queue/notifications

OR

/topic/restaurants/{{restaurantId}}/reservations
```

**Expected Messages** (when customers create reservations):

```json
{
  "type": "RESERVATION_CREATED",
  "reservation": {
    "id": 123,
    "userName": "Marco Rossi",
    "email": "marco.rossi@example.it",
    "phone": "3471234567",
    "pax": 4,
    "kids": 1,
    "status": "PENDING",
    "date": "2025-01-15",
    ...
  },
  "timestamp": "2025-01-15T14:30:00Z"
}
```

### Step 5: Verify Reservations

Run verification request:
```
Verification - Get All Reservations
GET /customer/reservation/all
```

Should return all 3 created reservations with status: "PENDING"

## Restaurant Staff Actions

Once reservations are created, restaurant staff (logged in as test@test.it) can:

### Accept Reservation

**Request**:
```
PUT /restaurant/reservation/{reservationId}/accept

Header: Authorization: Bearer {{restaurantToken}}

Body:
{
  "tableNumber": 5,
  "notes": "Customer requested window seat"
}
```

**WebSocket Event Sent**:
```json
{
  "type": "RESERVATION_ACCEPTED",
  "reservation": { ... },
  "timestamp": "..."
}
```

### Reject Reservation

**Request**:
```
PUT /restaurant/reservation/{reservationId}/reject

Header: Authorization: Bearer {{restaurantToken}}

Body:
{
  "reason": "No availability for requested time"
}
```

**WebSocket Event Sent**:
```json
{
  "type": "RESERVATION_REJECTED",
  "reservation": { ... },
  "timestamp": "..."
}
```

## Testing Checklist

- [ ] Customer 1 creates reservation → See RESERVATION_CREATED on WebSocket
- [ ] Customer 2 creates reservation → See RESERVATION_CREATED on WebSocket
- [ ] Customer 3 creates reservation → See RESERVATION_CREATED on WebSocket
- [ ] Restaurant staff sees all 3 reservations in pending list
- [ ] Restaurant staff accepts reservation → See RESERVATION_ACCEPTED event
- [ ] Restaurant staff rejects reservation → See RESERVATION_REJECTED event
- [ ] Customer receives notification about reservation status change
- [ ] All WebSocket messages arrive in real-time (<1 second)

## REST API Endpoints Tested

### Customer Endpoints
```
GET  /customer/restaurant/all
GET  /customer/restaurant/{id}/services
GET  /customer/restaurant/{id}/service/{serviceId}/day/{date}
POST /customer/reservation/ask
GET  /customer/reservation/all
```

### Restaurant Endpoints
```
GET  /restaurant/reservation/pending/get
GET  /restaurant/reservation/reservations?start=&end=
PUT  /restaurant/reservation/{id}/accept
PUT  /restaurant/reservation/{id}/reject
GET  /restaurant/notifications/badge
POST /restaurant/notifications/menu-open
GET  /restaurant/notifications?page=0&size=20
```

## WebSocket Endpoints

### Notification Subscription
```
STOMP: /ws
Subscribe: /user/{userId}/queue/notifications
```

### Reservation Subscription
```
STOMP: /ws
Subscribe: /topic/restaurants/{restaurantId}/reservations
```

## Debugging

### Check if WebSocket is connected
```javascript
// In Postman Console
console.log('Connected to WebSocket');
```

### Check if Events are being published
```bash
# Monitor RabbitMQ
docker logs greedysgroup-rabbitmq

# Check for messages in:
# - event_outbox (L0)
# - notification_outbox (L1)
# - notification_channel_send (L2)
```

### Database Queries
```sql
-- Check created reservations
SELECT * FROM reservation WHERE status = 'PENDING';

-- Check notification events
SELECT * FROM notification WHERE created_at > NOW() - INTERVAL 5 MINUTE;

-- Check WebSocket sends
SELECT * FROM notification_channel_send WHERE channel = 'WEBSOCKET';
```

## Common Issues

### Issue: WebSocket messages not received

**Solutions**:
1. Ensure restaurant staff is logged in with proper token
2. Check that restaurant ID matches in topic subscription
3. Verify RabbitMQ container is running: `docker ps | grep rabbitmq`
4. Check application logs: `docker logs greedysgroup-api | grep WebSocket`

### Issue: Reservation not created

**Solutions**:
1. Ensure customer token is valid (check expiration)
2. Verify restaurant exists and has available slots
3. Check that slotId is valid for the date
4. Ensure restaurantId matches

### Issue: Accept/Reject returns 403 Forbidden

**Solutions**:
1. Verify restaurant staff token is being used (not customer token)
2. Check that reservation belongs to authenticated restaurant
3. Verify user has PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE permission

## Performance Metrics

Expected timing:
- Reservation creation: < 100ms
- WebSocket event delivery: < 1 second
- Database update: < 50ms
- RabbitMQ processing: < 500ms

## Next Steps

1. Run the reservation creation requests
2. Connect to WebSocket topic in Postman
3. Observe real-time updates
4. Test accept/reject actions
5. Verify notification badges update in real-time
