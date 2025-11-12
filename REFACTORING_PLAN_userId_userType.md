# 🔄 REFACTORING PLAN: recipientId → userId, recipientType → user_type

**Data:** Novembre 12, 2025  
**Obiettivo:** Unificare nomenclatura in tutto il codebase

---

## ✅ COMPLETED

- [x] ANotification.java - Aggiunto `userId` + `userType`
- [x] NotificationEvent.java - Rinominato `recipientId` → `userId`, `recipientType` → `userType`

---

## ⏳ TODO - Files da aggiornare (80+ occorrenze)

### 1. **Messaging/Publishing**
- [ ] NotificationPublisherService.java
- [ ] RabbitNotificationEventPublisher.java
- [ ] OutboxPublisher.java

### 2. **Listeners**
- [ ] AdminNotificationListener.java
- [ ] CustomerNotificationListener.java
- [ ] RestaurantNotificationListener.java
- [ ] NotificationListener.java (deprecated)

### 3. **Orchestrator**
- [ ] NotificationOrchestrator.java
- [ ] NotificationOrchestratorFactory.java
- [ ] AbstractNotificationOrchestrator.java

### 4. **Channels**
- [ ] EmailNotificationChannel.java
- [ ] FirebaseNotificationChannel.java
- [ ] WebSocketNotificationChannel.java
- [ ] NotificationChannel.java

### 5. **Documentation**
- [ ] ARCHITECTURE_DESIGN.md
- [ ] NOTIFICATION_FLOW_DETAILED.md

---

## 🔧 Find & Replace Pattern

**IMPORTANTE:** Applicare in questo ordine per evitare conflitti:

### Step 1: Campi Java (getters/setters)
```
recipientId → userId
recipientType → userType
```

### Step 2: Commenti e Log
```
recipientType → userType
recipientId → userId
```

### Step 3: Database Columns (commenti)
```
recipient_id → user_id
recipient_type → user_type
```

---

## 📝 Sample replacements già fatte

In `ANotification.java`:
```java
@Column(name = "user_id", nullable = false)
private Long userId;

@Column(name = "user_type", nullable = false, length = 50)
private String userType;
```

In `NotificationEvent.java`:
```java
private Long userId;
private String userType;
```

---

## ⚠️ ATTENZIONE

Alcuni metodi hanno pattern che vanno preservati:
- `getRecipientId(T recipient)` → RINOMINARE A `getUserId(T recipient)`
- `checkIdempotency(eventId, recipientId)` → RINOMINARE A `checkIdempotency(eventId, userId)`

---

## 🎯 Next Step

Usare questo piano per completare il refactoring file by file.
