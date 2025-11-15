# WebSocket & Reservations - Complete Documentation Index

## 📚 Documentation Files

### 🟢 **START HERE**
- **[WEBSOCKET_STEP_BY_STEP.md](WEBSOCKET_STEP_BY_STEP.md)** (⭐ RECOMMENDED)
  - Step-by-step guide with exact screenshots and expected outputs
  - Takes ~8 minutes to complete
  - Beginner-friendly with clear checkpoints

### 📖 **Guides**

1. **[WEBSOCKET_QUICK_START.md](WEBSOCKET_QUICK_START.md)** (5 minutes)
   - Quick reference for experienced testers
   - Key concepts and flow diagrams
   - Quick setup and testing

2. **[WEBSOCKET_TEST_GUIDE.md](WEBSOCKET_TEST_GUIDE.md)** (Detailed)
   - Comprehensive technical guide
   - Architecture deep-dive
   - Troubleshooting and debugging
   - Database verification queries

3. **[TESTING_WEBSOCKET_README.md](TESTING_WEBSOCKET_README.md)** (Overview)
   - High-level summary
   - Expected JSON messages
   - Success criteria checklist

### 📋 **Implementation Details**
- **[WEBSOCKET_IMPLEMENTATION_COMPLETE.md](WEBSOCKET_IMPLEMENTATION_COMPLETE.md)**
  - Complete list of all files modified
  - Code changes explained
  - Architecture diagrams
  - Feature checklist

---

## 🧪 Postman Collections

### **Customer Phase**
📄 **Customer-Reservations-WebSocket-Test.json**

```
Setup Phase
├─ Get Restaurant Test@Test.It
├─ Get Services for Restaurant
└─ Get Time Slots for Today

Customer Reservations
├─ Customer 1 - Marco Rossi (4 pax, 1 kid)
├─ Customer 2 - Luca Bianchi (2 pax)
└─ Customer 3 - Giulia Neri (3 pax, 2 kids)

Verification
└─ Get All Reservations
```

### **Restaurant Phase**
📄 **Restaurant-Reservations-WebSocket-Test.json**

```
Authentication
└─ Restaurant Login (test@test.it)

Reservations - View
├─ Get All Pending Reservations
├─ Get Reservations by Date Range
└─ Get Paginated Reservations

Reservations - Actions
├─ Accept Reservation 1
├─ Accept Reservation 2
├─ Reject Reservation 3
├─ Mark as Seated
└─ Mark as No-Show

Notifications
├─ Get Notification Badge Count
├─ Mark Menu as Opened
└─ Get Notifications List
```

---

## 🚀 How to Use This Documentation

### For Quick Testing (8 minutes)
1. Read: **WEBSOCKET_STEP_BY_STEP.md**
2. Import: Both Postman collections
3. Execute: Step-by-step as described
4. Done! ✅

### For Understanding Architecture (15 minutes)
1. Read: **WEBSOCKET_QUICK_START.md** → Overview section
2. Review: **WEBSOCKET_TEST_GUIDE.md** → Architecture section
3. Understand: The 3-level outbox pattern

### For Complete Knowledge (30 minutes)
1. Read: **TESTING_WEBSOCKET_README.md**
2. Deep-dive: **WEBSOCKET_TEST_GUIDE.md**
3. Review: **WEBSOCKET_IMPLEMENTATION_COMPLETE.md**
4. Understand: All components and how they interact

### For Troubleshooting Issues
1. Check: **WEBSOCKET_TEST_GUIDE.md** → Debugging section
2. Run: Verification queries
3. Check: Docker logs
4. Search: Common Issues section

---

## 📊 Quick Reference

### REST Endpoints Tested

**Customer Endpoints**:
```
GET  /customer/restaurant/all
GET  /customer/restaurant/{id}/services
GET  /customer/restaurant/{id}/service/{serviceId}/day/{date}
POST /customer/reservation/ask
GET  /customer/reservation/all
```

**Restaurant Endpoints**:
```
GET  /restaurant/reservation/pending/get
GET  /restaurant/reservation/reservations?start=&end=
GET  /restaurant/reservation/pageable
PUT  /restaurant/reservation/{id}/accept
PUT  /restaurant/reservation/{id}/reject
PUT  /restaurant/reservation/{id}/seated
PUT  /restaurant/reservation/{id}/no_show
GET  /restaurant/notifications/badge
POST /restaurant/notifications/menu-open
GET  /restaurant/notifications?page=0&size=20
```

### WebSocket Topic
```
STOMP: /ws
Topic: /topic/restaurants/{restaurantId}/reservations
Events:
  - RESERVATION_CREATED
  - RESERVATION_ACCEPTED
  - RESERVATION_REJECTED
```

---

## ✅ Testing Checklist

Before you start, ensure:
- [ ] Docker services running: `docker-compose ps`
- [ ] API health check: `curl http://localhost:8080/health`
- [ ] Postman installed (v9.0+ for WebSocket support)
- [ ] Both JSON collections imported
- [ ] Environment variables configured

After testing:
- [ ] All 3 reservations created
- [ ] 3 WebSocket CREATED events received
- [ ] 2 reservations accepted
- [ ] 1 reservation rejected
- [ ] 2 WebSocket ACCEPTED events received
- [ ] 1 WebSocket REJECTED event received
- [ ] Badge count working
- [ ] Menu open resets badge

---

## 🎯 Test Flow Overview

```
┌─────────────────┐
│  STEP 1: SETUP  │ Import collections, set environment
└────────┬────────┘
         ▼
┌──────────────────────────┐
│ STEP 2: CUSTOMER PHASE   │ Create 3 reservations
└────────┬─────────────────┘
         ▼
┌──────────────────────────┐
│ STEP 3: WEBSOCKET PHASE  │ Connect & subscribe to topic
└────────┬─────────────────┘
         ▼
┌──────────────────────────┐
│ STEP 4: OBSERVE EVENTS   │ Watch CREATED messages arrive
└────────┬─────────────────┘
         ▼
┌──────────────────────────┐
│ STEP 5: RESTAURANT PHASE │ Accept/reject reservations
└────────┬─────────────────┘
         ▼
┌──────────────────────────┐
│ STEP 6: OBSERVE UPDATES  │ Watch ACCEPTED/REJECTED events
└────────┬─────────────────┘
         ▼
┌──────────────────────────┐
│ STEP 7: VERIFY NOTIF     │ Check badge and notifications
└──────────────────────────┘
         ▼
     ✅ SUCCESS!
```

---

## 🔗 File Locations

```
greedys_api/
├─ WEBSOCKET_STEP_BY_STEP.md ⭐ START HERE
├─ WEBSOCKET_QUICK_START.md
├─ WEBSOCKET_TEST_GUIDE.md
├─ TESTING_WEBSOCKET_README.md
├─ WEBSOCKET_IMPLEMENTATION_COMPLETE.md
├─ DOCUMENTATION_INDEX.md ← You are here
│
└─ test-postman/
   ├─ Customer-Reservations-WebSocket-Test.json
   └─ Restaurant-Reservations-WebSocket-Test.json
```

---

## 💡 Key Concepts

### WebSocket STOMP Protocol
- **CONNECT**: Establish WebSocket connection
- **SUBSCRIBE**: Listen to a topic
- **SEND**: Publish a message
- **DISCONNECT**: Close connection

### Real-Time Architecture
- Server publishes messages to topics
- All subscribed clients receive instantly
- No polling needed
- Persistent connection

### 3-Level Outbox Pattern
- **L0**: Domain events published immediately
- **L1**: Notifications created by listeners
- **L2**: Channel-specific sends (WebSocket, SMS, Email)

---

## 🆘 Need Help?

1. **WebSocket not connecting?**
   → Check WEBSOCKET_TEST_GUIDE.md → Debugging → "WebSocket Connection Fails"

2. **No WebSocket messages?**
   → Check WEBSOCKET_TEST_GUIDE.md → Debugging → "WebSocket Connected but No Messages"

3. **Reservations not creating?**
   → Check WEBSOCKET_TEST_GUIDE.md → Debugging → "Reservations Created but Not Visible"

4. **Accept/Reject returns 403?**
   → Check WEBSOCKET_TEST_GUIDE.md → Debugging → "Accept/Reject Returns 403 Forbidden"

5. **Want to understand architecture?**
   → Read WEBSOCKET_QUICK_START.md → System Architecture section

---

## 📈 Expected Performance

| Operation | Expected | Max |
|-----------|----------|-----|
| HTTP Request | 100ms | 500ms |
| WebSocket Delivery | 500ms | 1s |
| Database Update | 50ms | 200ms |
| **Total e2e** | **~850ms** | **2s** |

---

## ✨ What You'll Accomplish

After following this documentation, you'll have:

✅ Verified customer reservation creation  
✅ Confirmed WebSocket real-time delivery  
✅ Tested restaurant staff actions  
✅ Validated event streaming  
✅ Checked notification system  
✅ Understood the complete architecture  

---

**Documentation Updated**: 15 Novembre 2025  
**Status**: ✅ Complete and Production-Ready
