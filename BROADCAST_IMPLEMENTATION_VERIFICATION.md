# ❌ BROADCAST A MULTIPLI RECIPIENT - VERIFICA IMPLEMENTAZIONE
**Data:** 13 Novembre 2025  
**Status:** ⚠️ **PARZIALMENTE IMPLEMENTATO - INCOMPLETO**

---

## 🎯 COSA HAI CHIESTO

1. ✅ Mandare notifica a **TUTTI gli utenti del ristorante** (staff)?
2. ✅ Mandare notifica a **tutti gli utenti di un'agenzia**?
3. ✅ Concetto di **lettura condivisa** (sharedRead)?

---

## ✅ COSA È IMPLEMENTATO

### 1️⃣ Entità con "Lettura Condivisa" (sharedRead)
**File:** `AEventNotification.java`
```java
@Column(name = "shared_read")
@Builder.Default
private Boolean sharedRead = false;

@Column(name = "read_by_user_id")
private Long readByUserId;
```

**Pattern:** Se `sharedRead=true`:
- Primo user che legge → Segna `readByUserId = user_id`
- Tutti altri vedono "Gestito da [Nome]"
- Update query:
```sql
UPDATE notification SET read=true, read_by_user_id=50, read_at=NOW
WHERE id=123 AND sharedRead=true AND read=false
```

✅ **Implementato in:** `AEventNotification` + DAO methods

---

### 2️⃣ RestaurantNotification con sharedRead=true
**File:** `RestaurantNotificationListener.java` (riga 217)
```java
return RestaurantNotification.builder()
        .title(title)
        .body(body)
        .properties(properties)
        .userId(staffUserId)
        .userType("RESTAURANT_USER")
        .read(false)
        .sharedRead(true)  // ✅ Primo staff che agisce, tutti vedono
        .creationTime(Instant.now())
        .build();
```

✅ **Implementato:** Flag è presente + Database field `shared_read`

---

### 3️⃣ AgencyNotification con markAsReadShared()
**File:** `AgencyNotificationDAO.java` (riga 52-55)
```java
@Modifying(clearAutomatically = true)
@Query("UPDATE AgencyNotification a SET a.read = true, a.readByUserId = :readByUserId, a.readAt = :readAt " +
       "WHERE a.id = :notificationId AND a.sharedRead = true AND a.read = false")
void markAsReadShared(@Param("notificationId") Long notificationId, @Param("readByUserId") Long readByUserId, @Param("readAt") Instant readAt);
```

✅ **Implementato:** Metodo di update per shared read

---

## ❌ COSA MANCA (CRITICO!)

### 🔴 1. RestaurantNotificationListener - TODO COMMENT
**File:** `RestaurantNotificationListener.java` (riga 118-121)
```java
// Step 2: TODO - Query per trovare tutti gli staff di questo ristorante
// List<RUser> staffList = restaurantUserDAO.findByRestaurantId(restaurantId);

// Step 3: Per ogni staff, crea una RestaurantNotification
// (Placeholder: crea per staff_id=1)
Long staffUserId = 1L;  // ❌ HARDCODED! Crea SOLO per un utente
```

**Impatto:**
- ❌ Crea notifica SOLO per staff_id=1
- ❌ Non itera su TUTTI gli staff
- ❌ Non guarda `Restaurant.getRUsers()`

**Dovrebbe essere:**
```java
// Step 2: Query per trovare TUTTI gli staff di questo ristorante
List<RUser> staffList = restaurantDAO.findById(restaurantId)
        .orElseThrow()
        .getRUsers();  // Relazione 1:N

// Step 3: Per OGNI staff, crea RestaurantNotification
for (RUser staff : staffList) {
    RestaurantNotification notification = createNotificationFromEvent(
        eventType, eventData, restaurantId, staff.getId()
    );
    // Persist notification per ogni staff
    restaurantNotificationDAO.save(notification);
    // Crea notification_outbox
    notificationOutboxDAO.save(outbox);
}
```

---

### 🔴 2. AdminNotificationListener - PLACEHOLDER VUOTO
**File:** `AdminNotificationListener.java` (riga 129-145)
```java
private void createAdminNotifications(String eventId, String eventType, Map<String, Object> eventData) {
    /*
     * TODO: Questa è una placeholder - implementa secondo il tuo dominio
     * 
     * Logica suggerita:
     * - Estrai informazioni dall'evento (reservation, customer, etc)
     * - Query admin table per trovare admin responsabili
     * - Per ogni admin: CREATE AdminNotification
     * - INSERT in notification_outbox
     */
    // ❌ NON IMPLEMENTATO!
}
```

**Impatto:**
- ❌ **NON crea nessuna AdminNotification**
- ❌ Tutti gli admin NON ricevono notifiche

**Dovrebbe essere:**
```java
private void createAdminNotifications(String eventId, String eventType, Map<String, Object> eventData) {
    // Trova TUTTI gli admin del sistema
    List<Admin> allAdmins = adminDAO.findAll();
    
    for (Admin admin : allAdmins) {
        AdminNotification notification = createNotificationFromEvent(
            eventType, eventData
        );
        adminNotificationDAO.save(notification);
        
        NotificationOutbox outbox = NotificationOutbox.builder()
            .notificationId(notification.getId())
            .notificationType("ADMIN")
            // ...
            .build();
        notificationOutboxDAO.save(outbox);
    }
}
```

---

### 🔴 3. AgencyNotificationListener - TODO per loop
**File:** `AgencyNotificationListener.java` (presumibilmente simile)
```java
// Per OGNI agency user: crea AgencyNotification
// (Placeholder: crea per agency_user_id=1)
Long agencyUserId = 1L;  // ❌ HARDCODED!
```

**Impatto:**
- ❌ Crea notifica SOLO per agency_user_id=1
- ❌ Non itera su TUTTI gli utenti dell'agenzia

---

### 🔴 4. CustomerNotificationListener - NO BROADCAST
**File:** `CustomerNotificationListener.java` (riga 130)
```java
// ⭐ DIFFERENZA: Notifiche PERSONALI al cliente
// Una sola CustomerNotification per evento (non multipli)

// Step 2: Estrai il customerId dall'evento
Long customerId = ...;

// Crea UNA SOLA notifica (non loop)
CustomerNotification notification = createNotificationFromEvent(
    eventType, eventData, customerId
);
```

**✅ Corretto:** Per customer è giusto, una sola notifica personale.

---

### 🔴 5. Manca Query per RUsers del Ristorante
**NON ESISTE:** `restaurantUserDAO.findByRestaurantId(restaurantId)`

Dovresti avere accesso a:
```java
// Via Restaurant entity
Restaurant restaurant = restaurantDAO.findById(restaurantId).orElseThrow();
List<RUser> staff = restaurant.getRUsers();  // Relazione 1:N
```

---

## 📊 RIEPILOGO IMPLEMENTAZIONE

| Componente | Stato | Problema |
|-----------|-------|----------|
| `sharedRead` flag | ✅ 100% | Base di dati presente |
| `readByUserId` field | ✅ 100% | Traccia chi ha agito per primo |
| RestaurantNotification.sharedRead=true | ✅ 100% | Flag correttamente impostato |
| RestaurantListener crea per TUTTI staff | ❌ 0% | **TODO - Hardcoded staff_id=1** |
| AdminNotificationListener | ❌ 0% | **TODO - Placeholder vuoto** |
| AgencyNotificationListener | ❌ 0% | **TODO - Hardcoded agency_user_id=1** |
| `markAsReadShared()` DAO | ✅ 100% | Metodo presente |
| **TOTALE** | **⚠️ 50%** | **Manca iterazione su multipli recipient** |

---

## 🔧 COSA SERVE PER FUNZIONARE

### Passo 1: RestaurantNotificationListener - Ciclo su staff
```java
private void createRestaurantNotifications(String eventId, String eventType, Map<String, Object> eventData) {
    try {
        // Estrai restaurantId
        Long restaurantId = ...;
        
        // ⭐ QUERY tutti gli staff
        Restaurant restaurant = restaurantDAO.findById(restaurantId)
            .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        List<RUser> staffList = restaurant.getRUsers();
        
        if (staffList.isEmpty()) {
            log.warn("Restaurant {} has no staff members", restaurantId);
            return;
        }
        
        // ⭐ PER OGNI STAFF - crea notifica
        for (RUser staff : staffList) {
            RestaurantNotification notification = createNotificationFromEvent(
                eventType, eventData, restaurantId, staff.getId()
            );
            
            if (notification == null) continue;
            
            RestaurantNotification saved = restaurantNotificationDAO.save(notification);
            
            // Crea entry in notification_outbox
            NotificationOutbox outbox = NotificationOutbox.builder()
                .notificationId(saved.getId())
                .notificationType("RESTAURANT")
                .aggregateType(eventData.getOrDefault("aggregateType", "RESERVATION").toString())
                .aggregateId(restaurantId)
                .eventType(eventType)
                .payload(objectMapper.writeValueAsString(eventData))
                .status(NotificationOutbox.Status.PENDING)
                .retryCount(0)
                .build();
            
            notificationOutboxDAO.save(outbox);
            
            log.debug("Created notification for staff {}: {}", staff.getId(), saved.getId());
        }
        
        // Mark event as processed
        eventOutboxDAO.updateProcessedBy(eventId, "RESTAURANT_LISTENER", Instant.now());
        
    } catch (Exception e) {
        log.error("Error creating restaurant notifications", e);
        throw new RuntimeException(e);
    }
}
```

### Passo 2: AdminNotificationListener - Ciclo su admin
```java
private void createAdminNotifications(String eventId, String eventType, Map<String, Object> eventData) {
    try {
        // ⭐ QUERY tutti gli admin
        List<Admin> adminList = adminDAO.findAll();  // O findAllEnabled()
        
        if (adminList.isEmpty()) {
            log.warn("No admin users found");
            return;
        }
        
        // ⭐ PER OGNI ADMIN - crea notifica
        for (Admin admin : adminList) {
            Map<String, Object> notifData = createNotificationFromEvent(eventType, eventData);
            
            AdminNotification notification = AdminNotification.builder()
                .title((String) notifData.get("title"))
                .body((String) notifData.get("body"))
                .properties((Map<String, String>) notifData.get("properties"))
                .userId(admin.getId())
                .userType("ADMIN_USER")
                .read(false)
                .sharedRead(true)  // Primo admin che agisce, tutti vedono
                .creationTime(Instant.now())
                .build();
            
            AdminNotification saved = adminNotificationDAO.save(notification);
            
            NotificationOutbox outbox = NotificationOutbox.builder()
                .notificationId(saved.getId())
                .notificationType("ADMIN")
                .aggregateType(eventData.getOrDefault("aggregateType", "RESERVATION").toString())
                .aggregateId(0L)  // Admin non legato a aggregato specifico
                .eventType(eventType)
                .payload(objectMapper.writeValueAsString(eventData))
                .status(NotificationOutbox.Status.PENDING)
                .retryCount(0)
                .build();
            
            notificationOutboxDAO.save(outbox);
        }
        
        eventOutboxDAO.updateProcessedBy(eventId, "ADMIN_LISTENER", Instant.now());
        
    } catch (Exception e) {
        log.error("Error creating admin notifications", e);
        throw new RuntimeException(e);
    }
}
```

### Passo 3: AgencyNotificationListener - Ciclo su agency users
```java
private void createAgencyNotifications(String eventId, String eventType, Map<String, Object> eventData) {
    try {
        // Se l'evento è legato a un'agenzia specifica
        Long agencyId = eventData.get("agencyId") != null 
            ? ((Number) eventData.get("agencyId")).longValue() 
            : null;
        
        if (agencyId == null) {
            log.warn("Event {} missing agencyId, skipping agency notifications", eventId);
            return;
        }
        
        // ⭐ QUERY tutti gli utenti dell'agenzia
        List<AgencyUser> agencyUsers = agencyUserDAO.findByAgencyId(agencyId);
        
        if (agencyUsers.isEmpty()) {
            log.warn("Agency {} has no users", agencyId);
            return;
        }
        
        // ⭐ PER OGNI UTENTE AGENZIA - crea notifica
        for (AgencyUser agencyUser : agencyUsers) {
            AgencyNotification notification = createNotificationFromEvent(
                eventType, eventData, agencyUser.getId()
            );
            
            if (notification == null) continue;
            
            AgencyNotification saved = agencyNotificationDAO.save(notification);
            
            NotificationOutbox outbox = NotificationOutbox.builder()
                .notificationId(saved.getId())
                .notificationType("AGENCY")
                .aggregateType(eventData.getOrDefault("aggregateType", "RESERVATION").toString())
                .aggregateId(agencyId)
                .eventType(eventType)
                .payload(objectMapper.writeValueAsString(eventData))
                .status(NotificationOutbox.Status.PENDING)
                .retryCount(0)
                .build();
            
            notificationOutboxDAO.save(outbox);
        }
        
        eventOutboxDAO.updateProcessedBy(eventId, "AGENCY_LISTENER", Instant.now());
        
    } catch (Exception e) {
        log.error("Error creating agency notifications", e);
        throw new RuntimeException(e);
    }
}
```

---

## 🎯 ESEMPIO COMPLETO: RESERVATION_REQUESTED

```
1. Customer crea prenotazione
   └─ Restaurant ID = 10
   └─ Restaurant ha 3 staff: [user_id=50, user_id=51, user_id=52]
   └─ Tutti admin: [user_id=100, user_id=101, user_id=102]

2. EventOutboxPoller pubblica evento

3. RestaurantNotificationListener riceve:
   ├─ For staff_id=50: CREATE RestaurantNotification (sharedRead=true)
   ├─ For staff_id=51: CREATE RestaurantNotification (sharedRead=true)
   └─ For staff_id=52: CREATE RestaurantNotification (sharedRead=true)
   
   Risultato: 3 righe in notification_restaurant

4. AdminNotificationListener riceve:
   ├─ For admin_id=100: CREATE AdminNotification (sharedRead=true)
   ├─ For admin_id=101: CREATE AdminNotification (sharedRead=true)
   └─ For admin_id=102: CREATE AdminNotification (sharedRead=true)
   
   Risultato: 3 righe in notification_admin

5. ChannelPoller invia:
   ├─ notification_id=1000 (staff_50): SMS + Email + Push
   ├─ notification_id=1001 (staff_51): SMS + Email + Push
   ├─ notification_id=1002 (staff_52): SMS + Email + Push
   ├─ notification_id=1003 (admin_100): SMS + Email
   ├─ notification_id=1004 (admin_101): SMS + Email
   └─ notification_id=1005 (admin_102): SMS + Email

6. Staff #50 legge prima → readByUserId=50
   └─ Tutti altri staff vedono "Gestito da Staff #50"
   
7. Admin #100 legge prima → readByUserId=100
   └─ Tutti altri admin vedono "Gestito da Admin #100"
```

---

## ✅ COSA FUNZIONA GIÀ

1. **Database schema:** `shared_read`, `read_by_user_id` fields presenti
2. **DAO methods:** `markAsReadShared()` per update condiviso
3. **Flag setting:** RestaurantNotification.sharedRead=true impostato
4. **First-to-act logic:** Implementato in `AEventNotification`

## ❌ COSA MANCA

1. **Iterazione staff:** Loop for su `restaurant.getRUsers()`
2. **Iterazione admin:** Loop for su `adminDAO.findAll()`
3. **Iterazione agency:** Loop for su `agencyUserDAO.findByAgencyId()`
4. **Creazione in batch:** N notifiche per evento (non 1)

---

## ⏱️ TEMPO PER COMPLETARE

- RestaurantNotificationListener: 30 min
- AdminNotificationListener: 30 min
- AgencyNotificationListener: 30 min
- Testing: 1-2 ore

**Totale: ~2.5-3 ore**
