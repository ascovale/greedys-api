# WebSocket Real-Time Reservations - Complete Testing Guide

## 🎯 Objective

Test the complete flow of WebSocket real-time notifications where:
1. **Customers** create reservations
2. **Restaurant staff** receives INSTANT WebSocket notifications
3. **Restaurant staff** accepts/rejects reservations
4. **WebSocket events** are sent to all connected staff members

## 📦 Required Files

- `Customer-Reservations-WebSocket-Test.json` - Customer reservation creation
- `Restaurant-Reservations-WebSocket-Test.json` - Restaurant staff actions
- `WEBSOCKET_TEST_GUIDE.md` - Detailed technical guide

## 🚀 Quick Start (5 minutes)

### 1. Ensure Services Are Running

```bash
cd /home/valentino/workspace/greedysgroup/greedys_api

# Check all services
docker-compose ps

# Should show: mysql, rabbitmq, redis, api containers RUNNING
```

If not running:
```bash
docker-compose up -d
```

### 2. Import Postman Collections

1. Open Postman
2. Click "Import" → "Upload Files"
3. Select both JSON files:
   - `Customer-Reservations-WebSocket-Test.json`
   - `Restaurant-Reservations-WebSocket-Test.json`

### 3. Set Environment Variables

In Postman, create or update environment with:

```json
{
  "baseUrl": "http://localhost:8080",
  "customerToken": "",
  "restaurantToken": "",
  "today": "2025-01-15"
}
```

### 4. Customer Phase - Create 3 Reservations

**Collection**: `Customer-Reservations-WebSocket-Test`

Execute these requests **IN ORDER**:

```
✅ Setup - Get Restaurant Test@Test.It
   → Extracts: restaurantId

✅ Setup - Get Services for Restaurant
   → Extracts: serviceId

✅ Setup - Get Time Slots for Today
   → Extracts: slotId

✅ Customer 1 - Create Reservation
   → Marco Rossi, 4 people, 1 kid

✅ Customer 2 - Create Reservation
   → Luca Bianchi, 2 people, 0 kids

✅ Customer 3 - Create Reservation
   → Giulia Neri, 3 people, 2 kids
```

**Expected Result**: 3 reservations created with status PENDING

### 5. Restaurant Phase - Accept/Reject

**Collection**: `Restaurant-Reservations-WebSocket-Test`

Execute these requests **IN ORDER**:

```
✅ Restaurant Login (test@test.it)
   → Extracts: restaurantToken

✅ Get All Pending Reservations
   → Lists the 3 newly created reservations
   → Extracts: reservationId1, reservationId2, reservationId3

✅ Accept Reservation 1 - Marco Rossi (Table 5)
✅ Accept Reservation 2 - Luca Bianchi (Table 2)
✅ Reject Reservation 3 - Giulia Neri (reason: Time slot not available)
```

## 🔌 WebSocket Testing

### Prerequisites

- **Postman WebSocket Client** (Postman v9.0+)
- Restaurant staff logged in (have restaurantToken)
- Know your restaurantId (from setup requests)

### Connect to WebSocket

1. **New Request** → **WebSocket** (not HTTP)
2. **Enter URL**:
   ```
   ws://localhost:8080/ws
   ```

3. Click **Connect**

### Subscribe to Reservation Topic

Once connected, send a STOMP SUBSCRIBE frame:

```
SUBSCRIBE
id:sub-1
destination:/topic/restaurants/{{restaurantId}}/reservations
receipt:123
```

**Or in Postman WebSocket tab**:
- Go to "Message" tab
- Type:
```
SUBSCRIBE
id:sub-1
destination:/topic/restaurants/RESTAURANT_ID_HERE/reservations
```

Replace `RESTAURANT_ID_HERE` with actual ID (e.g., `/topic/restaurants/1/reservations`)

### Expected WebSocket Messages

When customers create reservations, you'll receive:

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
    "date": "2025-01-15"
  },
  "timestamp": "2025-01-15T14:30:00Z"
}
```

When restaurant accepts:

```json
{
  "type": "RESERVATION_ACCEPTED",
  "reservation": {
    "id": 123,
    "userName": "Marco Rossi",
    "status": "ACCEPTED",
    "tableNumber": 5
  },
  "timestamp": "2025-01-15T14:31:00Z"
}
```

When restaurant rejects:

```json
{
  "type": "RESERVATION_REJECTED",
  "reservation": {
    "id": 125,
    "userName": "Giulia Neri",
    "status": "REJECTED",
    "rejectionReason": "Time slot not available"
  },
  "timestamp": "2025-01-15T14:32:00Z"
}
```

## 📊 Complete Test Flow Diagram

```
┌─────────────────────────────────────────────────────────┐
│              CUSTOMER CREATES RESERVATION                │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
         ┌──────────────────┐
         │  HTTP POST       │
         │  /customer/      │
         │  reservation/ask │
         └────────┬─────────┘
                  │
                  ▼
        ┌──────────────────────┐
        │  Reservation Entity  │
        │  Status: PENDING     │
        │  Created in DB       │
        └────────┬─────────────┘
                 │
                 ▼
     ┌──────────────────────────────┐
     │ event_outbox [L0]           │
     │ ReservationCreatedEvent     │
     │ Published to RabbitMQ       │
     └────────┬─────────────────────┘
              │
              ▼
     ┌──────────────────────────────┐
     │ ReservationEventListener     │
     │ (RabbitMQ Listener)          │
     │ Creates notification         │
     └────────┬─────────────────────┘
              │
              ▼
    ┌────────────────────────────────┐
    │ ReservationWebSocketPublisher  │
    │ publishReservationCreated()    │
    └────────┬──────────────────────┘
             │
             ▼
   ┌──────────────────────────────────┐
   │ SimpMessagingTemplate.convertAndSend() │
   │ /topic/restaurants/1/reservations      │
   └────────┬───────────────────────────┘
            │
            ▼
   ┌──────────────────────────────────┐
   │ ALL Connected Restaurant Staff   │
   │ Receive Message INSTANTLY        │
   │ <1 second latency                │
   └──────────────────────────────────┘
```

## ✅ Testing Checklist

### Phase 1: Customer Reservation Creation
- [ ] All 3 setup requests execute successfully
- [ ] `restaurantId` is extracted (≠ empty)
- [ ] `serviceId` is extracted (≠ empty)
- [ ] `slotId` is extracted (≠ empty)
- [ ] Customer 1 reservation created (status 200/201)
- [ ] Customer 2 reservation created (status 200/201)
- [ ] Customer 3 reservation created (status 200/201)

### Phase 2: Restaurant WebSocket Connection
- [ ] WebSocket connects: `ws://localhost:8080/ws`
- [ ] SUBSCRIBE message sent successfully
- [ ] No connection errors in console

### Phase 3: WebSocket Message Reception
- [ ] 3 RESERVATION_CREATED messages received (one per customer)
- [ ] Each message contains proper customerName and pax
- [ ] Messages arrive in real-time (<1 second after POST)

### Phase 4: Restaurant Staff Actions
- [ ] Restaurant login successful (restaurantToken extracted)
- [ ] "Get All Pending Reservations" returns 3 items
- [ ] Accept Reservation 1: Status changes to ACCEPTED, tableNumber=5
- [ ] Accept Reservation 2: Status changes to ACCEPTED, tableNumber=2
- [ ] Reject Reservation 3: Status changes to REJECTED, reason populated

### Phase 5: WebSocket Event Updates
- [ ] 2 RESERVATION_ACCEPTED messages received
- [ ] 1 RESERVATION_REJECTED message received
- [ ] Messages match restaurant actions

### Phase 6: Notification System
- [ ] Badge count reflects unread notifications
- [ ] Menu-open resets badge to 0
- [ ] Notifications list shows all events

## 🔍 Verification Queries

### Check Pending Reservations in Database

```sql
-- From MySQL
docker exec greedysgroup-mysql mysql -u greedys_user -p'greedys_password' greedys_db

SELECT id, user_name, email, phone, pax, kids, status, created_at 
FROM reservation 
WHERE status = 'PENDING' 
ORDER BY created_at DESC 
LIMIT 5;
```

### Check WebSocket Events in Database

```sql
-- Check notification_channel_send for WebSocket entries
SELECT * 
FROM notification_channel_send 
WHERE channel = 'WEBSOCKET' 
ORDER BY created_at DESC 
LIMIT 10;
```

### Check RabbitMQ Messages

```bash
# Monitor RabbitMQ logs
docker logs greedysgroup-rabbitmq | tail -50

# Or access RabbitMQ UI
# http://localhost:15672 (admin/password)
```

### Check API Logs

```bash
# Monitor application logs
docker logs greedysgroup-api | grep -i "websocket\|reservation" | tail -30
```

## 🆘 Troubleshooting

### Problem: WebSocket Connection Fails

**Error**: `WebSocket is closed before the connection is established`

**Solutions**:
1. Ensure API is running: `docker-compose ps | grep api`
2. Check port 8080 is accessible: `curl http://localhost:8080/health`
3. Try `ws://` instead of `wss://` (not HTTPS)

### Problem: WebSocket Connected but No Messages

**Error**: Subscribed but receiving nothing

**Solutions**:
1. Verify SUBSCRIBE destination format: `/topic/restaurants/{restaurantId}/reservations`
2. Check that restaurantId is correct (not placeholder)
3. Ensure customers creating reservations target same restaurantId
4. Check RabbitMQ is running: `docker logs greedysgroup-rabbitmq`

### Problem: Reservations Created but Not Visible

**Error**: Customer POST returns 200 but reservation not in GET list

**Solutions**:
1. Check customer token validity: Run "Customer Login" again
2. Verify restaurant exists: GET `/customer/restaurant/all`
3. Check DB permissions: `docker exec greedysgroup-mysql mysql ...`
4. Check application logs: `docker logs greedysgroup-api`

### Problem: Accept/Reject Returns 403 Forbidden

**Error**: `Forbidden - User does not have permission`

**Solutions**:
1. Ensure using restaurantToken (not customerToken)
2. Verify restaurant staff user has write permission
3. Check reservation belongs to authenticated restaurant
4. Re-login: Run "Restaurant Login" request again

## 📈 Performance Expectations

| Operation | Expected Time | Max Allowed |
|-----------|---------------|------------|
| Reservation creation (HTTP) | 100ms | 500ms |
| WebSocket message delivery | 500ms | 1s |
| Database update | 50ms | 200ms |
| RabbitMQ processing | 200ms | 1s |
| **Total end-to-end** | **850ms** | **2s** |

## 🎓 Learning Outcomes

After this test, you should understand:

1. ✅ How customer reservations flow through the system
2. ✅ How events are published to RabbitMQ (event_outbox pattern)
3. ✅ How events are consumed by listeners (EventListener)
4. ✅ How WebSocket messages are published (ReservationWebSocketPublisher)
5. ✅ How STOMP subscriptions receive real-time messages
6. ✅ How restaurant staff can act on reservations
7. ✅ How all actions trigger new WebSocket events
8. ✅ How the 3-level outbox pattern ensures reliability

## 📝 Next Steps

1. Test with multiple restaurant staff connected (verify broadcast)
2. Test with failed reservations (network issues, etc.)
3. Implement customer notification when reservation accepted/rejected
4. Add SMS/Email notifications for reservation events
5. Build restaurant management UI with real-time updates

## 📞 Support

For issues or questions:
1. Check application logs: `docker logs greedysgroup-api`
2. Check RabbitMQ: `docker logs greedysgroup-rabbitmq`
3. Check database: Query notification tables
4. Review WEBSOCKET_TEST_GUIDE.md for technical details
