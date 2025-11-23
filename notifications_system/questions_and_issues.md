# Questions and Issues - Notification System

## Outstanding Questions

### 1. **What happens if EventOutboxOrchestrator crashes after publishing to RabbitMQ but before marking event as PROCESSED?**

**Current Behavior:**
- Event remains in EventOutbox with status=PENDING
- Next cycle: EventOutboxOrchestrator attempts to publish again
- RabbitMQ receives duplicate message

**Idempotency Handle:**
- ProcessedEvent table has UNIQUE(eventId)
- Second publish attempt triggers UNIQUE constraint violation
- Listener catches exception and skips duplicate

**Concern:**
- Is UNIQUE constraint violation gracefully handled in all cases?
- What if ProcessedEvent insert fails for other reasons (DB down)?

### 2. **How does the system behave during RabbitMQ outage?**

**Scenario:** RabbitMQ is down for 2 hours.

**Expected Flow:**
1. EventOutboxOrchestrator attempts publish every 1 second
2. RabbitTemplate throws exception (connection refused)
3. Event remains in EventOutbox with status=PENDING
4. After RabbitMQ restarts: EventOutboxOrchestrator retries and publishes

**Potential Issue:**
- Are there circuit breaker patterns to prevent continuous failed publish attempts?
- Will EventOutbox table grow unboundedly if RabbitMQ is down for days?
- Suggested: Add max retry count + move to status=DEAD_LETTER after N failures

### 3. **What is the guaranteed maximum latency for WebSocket notifications?**

**Current Implementation:**
- Synchronous WebSocket send happens immediately after DB persist in listener
- ~100-500ms typical latency from event creation to client receive
- Best-effort: if client offline → no retry

**Edge Cases:**
- What if SimpMessagingTemplate is overwhelmed with many concurrent sends?
- Is there rate limiting or backpressure handling?
- What about connection timeouts?

### 4. **How does read status propagation work across time zones?**

**Scenario:** Multiple restaurant staff in different time zones see same RESERVATION event.

**Current Implementation:**
- `read_at` timestamp is server-side NOW() when user marks as read
- Shared read updates all staff in same restaurant/channel
- No time zone awareness in read propagation

**Question:**
- Is NOW() always server time (correct)?
- Or is there potential for client-side timestamp to affect read_at?
- Should UI show "read by John at 2:30 PM EST"?

### 5. **What happens if a listener crashes after disaggregating but before persisting?**

**Scenario:** RestaurantNotificationListener disaggregates 20 records, crashes on batch insert to DB.

**Expected Flow:**
- Transaction rolls back (no persist)
- RabbitMQ message is NACK'd
- Message requeued

**Issue:**
- Is @Transactional configured correctly to roll back on exception?
- Will NACK + requeue work as expected?
- How many times will the message be retried before being sent to dead letter?

### 6. **Shared read scope - what if event_id collides?**

**Current:** Shared read uses eventId prefix matching: `eventId LIKE 'evt-res-123_%'`

**Concern:**
- What if two events have similar event IDs?
- Example: "evt-123" and "evt-123-extra"
- LIKE pattern might match both

**Recommendation:**
- Use exact event ID + restaurant ID + channel in WHERE clause
- Not just LIKE prefix

### 7. **How does the system handle notification preferences conflicts?**

**Scenario:**
- Global setting: "Email enabled"
- User setting: "Email disabled"
- Event rule: "Email is mandatory for CRITICAL"

**Current Behavior:**
- RestaurantUserOrchestrator does: Group ∩ User ∩ Event
- Final channels = intersection of all three

**Question:**
- What is the precedence? (Event > User > Group, or Group > Event > User?)
- Is this precedence documented / configurable?
- Should CRITICAL events override user's "disable email" preference?

### 8. **What prevents multiple listeners from processing same message?**

**RabbitMQ Configuration:**
- Single queue: notification.restaurant
- Multiple listener instances possible (scaled)

**Current Handling:**
- EventOutbox status=PENDING → multiple EventOutboxOrchestrator instances
- ProcessedEvent UNIQUE constraint prevents duplicate publishing

**Concern:**
- What if two listener instances consume same message from queue?
- Both check idempotency, both call persistNotification()
- Notification table UNIQUE(eventId) saves us from duplicates

**Question:**
- Is UNIQUE constraint index performance acceptable under load?
- What's the expected behavior under concurrent listener scaling?

### 9. **How does read status synchronization work across multiple WebSocket connections from same user?**

**Scenario:** User has 2 browser tabs open, same restaurant staff ID.

**Expected:**
- User marks notification as read in Tab 1
- Tab 2 should show it as read immediately

**Current Implementation:**
- ReadStatusService broadcasts to /topic/notifications/{userId}/RESTAURANT
- Both tabs subscribed to same topic
- Both should receive update

**Question:**
- Is this actually tested / verified to work?
- What if one tab connects, other doesn't?
- Are there any race conditions?

### 10. **SharedReadStrategy - what if restaurantId is NULL?**

**Current:**
```java
UPDATE RestaurantUserNotification
SET status='READ'
WHERE event_id LIKE '...' AND restaurant_id = ?
```

**Edge Case:**
- If restaurantId is NULL or not passed in params
- Query still executes but matches nothing
- Silent failure?

**Concern:**
- Should there be validation before calling strategy?
- What's the error message if params are invalid?

---

## Potential Implementation Issues

### Issue #1: EventOutbox Cleanup Strategy Missing

**Problem:**
- EventOutbox keeps growing indefinitely
- Processed events never deleted
- After 1 year: millions of PROCESSED rows in DB

**Impact:**
- Slow backups
- Migration complexity
- Query performance (even with status index)

**Recommendation:**
```
Add cleanup job:
- Keep last 30 days of events
- Archive older events to separate table
- Or implement TTL on EventOutbox
```

### Issue #2: No Dead Letter Queue (DLQ) for Failed Messages

**Problem:**
- If notification listener permanently fails on a message
- Message retried forever (or up to RabbitMQ limit)
- No visibility into permanently broken messages

**Recommendation:**
```
Implement DLQ:
- If message fails N times → send to notification.dlq
- Monitor DLQ for alerts
- Admin interface to replay DLQ messages
```

### Issue #3: WebSocket Session Leaks Possible

**Problem:**
- If client disconnects ungracefully (kill browser)
- Session might persist in memory
- Long-lived connections could consume resources

**Recommendation:**
- Implement WebSocketEventListener.afterConnectionClosed()
- Clean up session attributes explicitly
- Monitor active connections

### Issue #4: Notification Archival Not Automated

**Problem:**
- Old notifications pile up in DB
- No automatic cleanup after read/delivered
- CustomerNotification mentions >30 day archive but unclear if implemented

**Recommendation:**
- Add @Scheduled job to archive old notifications
- Move to archive table monthly
- Or implement TTL in database (MySQL 5.7+)

### Issue #5: ChannelPoller Concurrency Not Addressed

**Problem:**
- If ChannelPoller is scaled horizontally (multiple instances)
- Both instances could pick same notification
- Both attempt send

**Impact:**
- Duplicate sends (moderate issue, idempotency helps)
- Wasted resources

**Recommendation:**
- Use SELECT ... FOR UPDATE (pessimistic lock)
- Or use distributed lock (Redis/Zookeeper)
- Single ChannelPoller instance per channel type (deploy strategy)

### Issue #6: No Monitoring/Alerting for Slow Deliveries

**Problem:**
- Email takes 5 minutes to send (unusual)
- No alert
- Users confused why they haven't received notification

**Recommendation:**
- Track delivery time per channel
- Alert if P95 latency exceeds threshold
- Dashboard showing notification pipeline health

### Issue #7: Unclear How to Handle CRITICAL / TIME-SENSITIVE Events

**Problem:**
- Current implementation is generic
- Doesn't distinguish "order must arrive in 30 seconds" vs "daily digest"

**Recommendation:**
- Add priority field to EventOutbox
- HIGH priority → poll ChannelPoller every 1s (not 30s)
- CRITICAL → direct send (not queued)

### Issue #8: Customer Notification Archive Not Actually Implemented

**Problem:**
- Code comments mention "Archive cleanup (>30 days)" in CustomerOrchestrator
- But no actual implementation visible
- Unclear if this happens automatically

**Recommendation:**
- Clarify if archival is done
- If not: implement scheduled job to clean up old notifications

### Issue #9: Broadcast Notifications Scalability Question

**Problem:**
- RESTAURANT_HUB_ALL scope: mark ALL staff as read for hub
- What if hub has 1000 staff?
- Single UPDATE query might be slow

**Recommendation:**
- Test performance with large UPDATE (1000+ rows)
- Consider batch updates if needed
- Add query timeout protection

### Issue #10: No Documentation of EventOutbox Message Format

**Problem:**
- Message structure defined in code but not in docs
- Listener implementations assume specific fields
- Hard to understand without reading source

**Recommendation:**
- Document JSON schema for messages
- Example per event type
- Version messages for future compatibility

---

## Ambiguities Requiring Clarification

### A1. When is "readByAll" set to true vs false?

**Code shows:**
- RestaurantUserNotification can have readByAll=true/false
- CustomerNotification always readByAll=false

**Question:**
- Which event types trigger readByAll=true for restaurant notifications?
- Is this configurable per event type?
- Who sets this value?

### A2. How are recipients determined for BROADCAST vs TARGETED?

**Code mentions recipientType="BROADCAST" or "TARGETED"**

**But:**
- How does EventOutboxOrchestrator know which events are broadcast?
- Is this stored in EventOutbox.eventType?
- Is there a lookup table?

### A3. What happens to notification records if user is deleted?

**Scenario:** Staff member leaves company (user deleted).

**Questions:**
- Are their notification records cascaded deleted?
- Left as orphans in DB?
- Is there a soft-delete strategy?

### A4. How do notification preferences override work?

**System seems to have:**
- Global notification settings (per restaurant/agency)
- User notification preferences
- Event-type-specific rules

**But relationship unclear:**
- If user disables WebSocket, but event is critical WebSocket
- Does critical event force-enable WebSocket?
- Or respect user's disable preference?

### A5. What is the exact EventOutbox idempotency guarantee?

**Current:**
- ProcessedEvent UNIQUE(eventId)
- Prevents duplicate RabbitMQ publish

**But:**
- If ProcessedEvent insert fails (deadlock, constraint error)
- Does orchestrator give up?
- Or keep trying?
- Circular dependency: need to insert ProcessedEvent to know if already processed

---

## Database Schema Questions

### Q1: Event ID Generation Strategy

**Current:**
- Comments show: "evt-3-order-12345"
- But actual generation logic not visible

**Questions:**
- Is eventId generated by EventOutbox?
- Or passed in from business service?
- What format should AI assume for documentation?

### Q2: Notification Table Indexes

**Visible:**
- UNIQUE(eventId)
- Foreign key to EventOutbox (?)

**Missing:**
- Index on (user_id, channel, status)?
- Index on (restaurant_id, status)?
- Index on created_at (for cleanup queries)?

**Recommendation:**
- Document all necessary indexes
- Verify they're created in migrations

### Q3: Is "shared_read_scope" column actually created?

**Documentation mentions:**
- New column on AEventNotification
- Default value: "NONE"

**Questions:**
- Is this actually deployed?
- Or still in planning phase?
- How to verify in production DB?

---

## Performance Questions

### P1: What's the maximum throughput?

**Scenarios:**
- 1000 events/second
- 10 restaurants × 100 staff each = 1000 staff
- Disaggregation: 1000 events × 1000 staff × 2 channels = 2M notifications/sec

**Questions:**
- Is the system designed for this scale?
- What's the breaking point?
- Need load testing results

### P2: Database connection pool exhaustion

**Current:**
- Unknown if connection pool size is configured
- Unknown if there are connection leak prevention mechanisms

**Risk:**
- Slow queries hold connections
- ChannelPoller polls 4 channels in parallel
- All 4 might exhaust pool

### P3: RabbitMQ Memory

**With 1M events/day:**
- Each message ~500 bytes
- Queue retention? Minutes? Hours?
- Is there risk of RabbitMQ memory exhaustion?

---

## Testing Questions

### T1: Idempotency Testing

**Questions:**
- Are there integration tests for duplicate event publishing?
- Is UNIQUE constraint violation tested?
- What about concurrent listener processing same message?

### T2: Failover Testing

**Scenarios not tested (assumed):**
- RabbitMQ down → EventOutbox recovery
- Listener crash → message requeue
- Partial disaggregation failure → transaction rollback
- WebSocket connection drop → message persistence

### T3: Load Testing

**Missing:**
- Throughput benchmarks
- Latency P50/P95/P99
- Concurrent connection limits
- Memory/CPU usage under load

---

## Recommendations for Resolution

### Immediate (Blocking Issues)

1. **Clarify EventOutbox cleanup strategy** → implement or schedule
2. **Add monitoring for delivery latencies** → dashboard required
3. **Test failover scenarios** → integration tests
4. **Document event ID generation** → schema needed

### Short Term (Within Sprint)

1. **Implement DLQ for RabbitMQ** → operational necessity
2. **Add WebSocket session cleanup** → memory leak prevention
3. **Verify read status synchronization** → multi-tab testing
4. **Performance testing under load** → capacity planning

### Long Term (Next Quarter)

1. **Implement distributed notification deduplication** → horizontal scaling
2. **Add admin UI for notification debugging** → observability
3. **Implement notification template system** → content management
4. **Add A/B testing support** → feature experimentation

---

**Document Version**: 1.0  
**Last Updated**: November 23, 2025  
**Status**: Initial Assessment  
**Severity**: Mixed (mostly informational, some operational concerns)
