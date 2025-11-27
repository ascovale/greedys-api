# 🎯 TEMPLATE METHOD PATTERN - NOTIFICATION LISTENERS

## OVERVIEW

Il sistema di notifiche usa il **Template Method Pattern** (Gang of Four) per evitare duplicazione di codice tra i 4 listener specifici (Restaurant, Customer, Agency, Admin).

---

## 🏗️ STRUTTURA DELL'ARCHITETTURA

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ TEMPLATE METHOD PATTERN (Gang of Four)                                       │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│ IDEA PRINCIPALE:                                                              │
│ Definisci la struttura di un algoritmo nella classe base (BaseListener)      │
│ ma delega i DETTAGLI SPECIFICI alle sottoclassi.                            │
│                                                                               │
│ COSA VA NEL BASE (logica comune):                                            │
│ ✅ SEMPRE UGUALE per Restaurant, Customer, Agency, Admin:                    │
│   - Parsing del messaggio RabbitMQ                                           │
│   - Idempotency check (esiste già questo eventId?)                           │
│   - Orchestrator call (disaggregazione)                                      │
│   - LOOP: persist + attemptWebSocketSend per OGNI notifica                  │
│   - ACK/NACK a RabbitMQ                                                      │
│   - Gestione transazioni (@Transactional)                                    │
│   - Exception handling (try/catch con retry logic)                           │
│                                                                               │
│ COSA VA NELLE SUBCLASSI (logica specifica):                                 │
│ ❌ DIVERSA per Restaurant vs Customer vs Agency:                             │
│   - existsByEventId()         → quale DAO? RestaurantUserNotificationDAO?    │
│   - persistNotification()      → salva Restaurant vs Customer vs Admin?      │
│   - attemptWebSocketSend()     → quale sender? Da quale classe?             │
│   - getTypeSpecificOrchestrator() → quale orchestrator? RESTAURANT vs TEAM?  │
│   - enrichMessageWithTypeSpecificFields() → quali campi aggiungere?         │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 📊 PERCHÉ IL LOOP (persist + WebSocket) STA NEL BASE?

La risposta è: **IL LOOP È UGUALE PER TUTTI**, solo i METODI CHIAMATI sono diversi.

### ❌ **APPROCCIO SBAGLIATO** (mettere il loop in ogni Restaurant/Customer/Agency):

```java
// RestaurantTeamNotificationListener
@Override
protected void processNotificationMessage(...) {
    // DUPLICATO: restaurare il loop
    List<RestaurantUserNotification> disaggregated = orchestrator.disaggregateAndProcess(message);
    
    for (RestaurantUserNotification notif : disaggregated) {  // ← DUPLICATO
        persistNotification(notif);                            // ← DUPLICATO
        attemptWebSocketSend(notif);                           // ← DUPLICATO
    }
    
    channel.basicAck(deliveryTag, false);
}

// CustomerNotificationListener
@Override
protected void processNotificationMessage(...) {
    // ANCORA LO STESSO LOOP - è identico!
    List<CustomerNotification> disaggregated = orchestrator.disaggregateAndProcess(message);
    
    for (CustomerNotification notif : disaggregated) {  // ← DUPLICATO
        persistNotification(notif);                      // ← DUPLICATO (ma chiama DAO Customer!)
        attemptWebSocketSend(notif);                     // ← DUPLICATO (ma chiama sender Customer!)
    }
    
    channel.basicAck(deliveryTag, false);
}

// AgencyUserNotificationListener
@Override
protected void processNotificationMessage(...) {
    // E ANCORA LO STESSO LOOP - è ancora identico!
    List<AgencyUserNotification> disaggregated = orchestrator.disaggregateAndProcess(message);
    
    for (AgencyUserNotification notif : disaggregated) {  // ← DUPLICATO
        persistNotification(notif);                        // ← DUPLICATO (ma chiama DAO Agency!)
        attemptWebSocketSend(notif);                       // ← DUPLICATO (ma chiama sender Agency!)
    }
    
    channel.basicAck(deliveryTag, false);
}
```

**PROBLEMI**:
- ❌ Codice duplicato in 4 posti
- ❌ Se cambi il loop (es. aggiungi un log, cambi strategia), devi modificare 4 file!
- ❌ Risk di inconsistenza tra i listener
- ❌ Violazione del DRY principle (Don't Repeat Yourself)

### ✅ **APPROCCIO GIUSTO** (Template Method nel Base):

```java
// BaseNotificationListener - DEFINISCE la struttura
public abstract class BaseNotificationListener<T extends ANotification> {
    
    @Transactional
    protected void processNotificationMessage(...) {
        try {
            // ... parsing, idempotency check, orchestrator call ...
            
            List<T> disaggregated = orchestrator.disaggregateAndProcess(message);
            
            // ⭐ IL LOOP QUI - una sola volta
            for (T notification : disaggregated) {
                persistNotification(notification);      // ← Delegato astratto
                attemptWebSocketSend(notification);     // ← Delegato astratto
            }
            
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, true);
            throw new RuntimeException(...);
        }
    }
    
    // ⭐ METODI ASTRATTI - implementazione specifica per ogni tipo
    protected abstract boolean existsByEventId(String eventId);
    protected abstract void persistNotification(T notification);
    protected abstract void attemptWebSocketSend(T notification);
    protected abstract NotificationOrchestrator<T> getTypeSpecificOrchestrator(Map<String, Object> message);
    protected abstract void enrichMessageWithTypeSpecificFields(Map<String, Object> message, NotificationEventPayloadDTO payload);
}
```

### 🍴 **COME USARLO** (Subclassi implementano i dettagli):

```java
// RestaurantTeamNotificationListener - IMPLEMENTA i dettagli
@Service
@RequiredArgsConstructor
public class RestaurantTeamNotificationListener 
    extends BaseNotificationListener<RestaurantUserNotification> {
    
    private final RestaurantUserNotificationDAO notificationDAO;
    private final NotificationOrchestratorFactory orchestratorFactory;
    private final NotificationWebSocketSender webSocketSender;
    
    @RabbitListener(queues = "notification.restaurant.reservations", ackMode = "MANUAL")
    public void onTeamNotificationMessage(
        @Payload NotificationEventPayloadDTO payload,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        Channel channel
    ) {
        // Chiama il TEMPLATE METHOD del base - il loop è già lì!
        processNotificationMessage(payload, deliveryTag, channel);
    }
    
    @Override
    protected boolean existsByEventId(String eventId) {
        return notificationDAO.existsByEventId(eventId);  // ← DAO specifico
    }
    
    @Override
    protected void persistNotification(RestaurantUserNotification notification) {
        notificationDAO.save(notification);  // ← Salva in tabella restaurant
    }
    
    @Override
    protected void attemptWebSocketSend(RestaurantUserNotification notification) {
        if (notification.getChannel() != null && 
            notification.getChannel().toString().equals("WEBSOCKET")) {
            webSocketSender.sendRestaurantNotification(notification);
        }
    }
    
    @Override
    protected NotificationOrchestrator<RestaurantUserNotification> getTypeSpecificOrchestrator(
        Map<String, Object> message
    ) {
        return orchestratorFactory.getOrchestrator("RESTAURANT_TEAM");
    }
    
    @Override
    protected void enrichMessageWithTypeSpecificFields(
        Map<String, Object> message,
        NotificationEventPayloadDTO payload
    ) {
        // Restaurant-specific enrichment
        if (payload.getRecipientId() != null) {
            message.put("restaurant_id", payload.getRecipientId());
        }
    }
}

// CustomerNotificationListener - IMPLEMENTA i dettagli (diversi!)
@Service
@RequiredArgsConstructor
public class CustomerNotificationListener 
    extends BaseNotificationListener<CustomerNotification> {
    
    private final CustomerNotificationDAO notificationDAO;  // ← DAO diverso!
    private final NotificationOrchestratorFactory orchestratorFactory;
    private final NotificationWebSocketSender webSocketSender;
    
    @RabbitListener(queues = "notification.customer", ackMode = "MANUAL")
    public void onNotificationMessage(
        @Payload NotificationEventPayloadDTO payload,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        Channel channel
    ) {
        // Stessa chiamata! Il loop è nel base
        processNotificationMessage(payload, deliveryTag, channel);
    }
    
    @Override
    protected boolean existsByEventId(String eventId) {
        return notificationDAO.existsByEventId(eventId);  // ← DAO Customer
    }
    
    @Override
    protected void persistNotification(CustomerNotification notification) {
        notificationDAO.save(notification);  // ← Salva in tabella customer!
    }
    
    @Override
    protected void attemptWebSocketSend(CustomerNotification notification) {
        if (notification.getChannel() != null && 
            notification.getChannel().toString().equals("WEBSOCKET")) {
            webSocketSender.sendCustomerNotification(notification);  // ← Sender diverso!
        }
    }
    
    @Override
    protected NotificationOrchestrator<CustomerNotification> getTypeSpecificOrchestrator(
        Map<String, Object> message
    ) {
        return orchestratorFactory.getOrchestrator("CUSTOMER");
    }
    
    @Override
    protected void enrichMessageWithTypeSpecificFields(
        Map<String, Object> message,
        NotificationEventPayloadDTO payload
    ) {
        // Customer-specific enrichment
        if (payload.getRecipientId() != null) {
            message.put("customer_id", payload.getRecipientId());
        }
    }
}
```

---

## ✅ VANTAGGI DEL DESIGN PATTERN

| Vantaggio | Spiegazione |
|-----------|------------|
| **DRY Principle** | Codice comune centralizzato nel base, riutilizzato da tutti i listener |
| **Manutenzione facile** | Se cambi il loop (es. aggiungi log), cambi UN file (BaseListener) |
| **Logica comune centralizzata** | Parse, idempotency check, ACK/NACK - tutti in un posto |
| **Facile aggiungere nuovi listener** | Crea una nuova classe che estende BaseListener, implementa 5 metodi astratti |
| **Type safety con Generics** | `<T extends ANotification>` - compile-time type checking |
| **Invariante mantenuto** | Il loop è SEMPRE lo stesso, solo i DAO/sender cambiano |
| **Polimorfismo** | Ogni subclasse implementa dettagli specifici |
| **Testabilità** | Puoi mockare gli abstract methods per testare il loop |
| **Atomic transactions** | `@Transactional` nel base garantisce atomicità per tutto il flusso |
| **Centralized error handling** | try/catch, NACK, @Retryable - tutto gestito in un posto |

---

## 🔍 VALUTAZIONE FINALE

```
┌─────────────────────────────────────────────────────────────────┐
│ DESIGN PATTERN CORRETTO ✅                                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ ✅ TEMPLATE METHOD PATTERN:                                      │
│    - Algoritmo generale nel base                                │
│    - Passi specifici delegati alle subclassi                    │
│    - Permette polimorfismo e DRY                                │
│                                                                  │
│ ✅ GENERICS <T extends ANotification>:                           │
│    - Tipizzazione forte                                         │
│    - Compile-time type safety                                   │
│    - Evita cast e ClassCastException                            │
│                                                                  │
│ ✅ ABSTRACT METHODS:                                             │
│    - Force alle subclassi di implementare i dettagli           │
│    - Compiler controlla che tutti i metodi siano implementati  │
│                                                                  │
│ ✅ @Transactional nel base:                                      │
│    - Transazione UNICA per TUTTO il flusso                      │
│    - Atomicità: o TUTTO passa o TUTTO rollback                 │
│    - Se persist fallisce, ACK non viene fatto                   │
│                                                                  │
│ ✅ Exception handling centralizzato:                             │
│    - try/catch nel base per tutti i listener                    │
│    - NACK automatico su errore                                  │
│    - @Retryable automatico                                      │
│                                                                  │
│ È IL MODO CORRETTO DI FARE IN SPRING ENTERPRISE! 💪            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📋 CHECKLIST - VERIFICARE CHE I 4 LISTENER SIANO CORRETTI

```
RESTAURANT LISTENER:
☑️ Extends BaseNotificationListener<RestaurantUserNotification>
☑️ @RabbitListener(queues = "notification.restaurant.reservations")
☑️ Implementa: existsByEventId() → RestaurantUserNotificationDAO
☑️ Implementa: persistNotification() → DAO.save()
☑️ Implementa: attemptWebSocketSend() → webSocketSender.sendRestaurantNotification()
☑️ Implementa: getTypeSpecificOrchestrator() → factory.getOrchestrator("RESTAURANT_TEAM")

CUSTOMER LISTENER:
☑️ Extends BaseNotificationListener<CustomerNotification>
☑️ @RabbitListener(queues = "notification.customer")
☑️ Implementa: existsByEventId() → CustomerNotificationDAO
☑️ Implementa: persistNotification() → DAO.save()
☑️ Implementa: attemptWebSocketSend() → webSocketSender.sendCustomerNotification()
☑️ Implementa: getTypeSpecificOrchestrator() → factory.getOrchestrator("CUSTOMER")

AGENCY LISTENER:
☑️ Extends BaseNotificationListener<AgencyUserNotification>
☑️ @RabbitListener(queues = "notification.agency")
☑️ Implementa: existsByEventId() → AgencyUserNotificationDAO
☑️ Implementa: persistNotification() → DAO.save()
☑️ Implementa: attemptWebSocketSend() → webSocketSender.sendAgencyNotification()
☑️ Implementa: getTypeSpecificOrchestrator() → factory.getOrchestrator("AGENCY")

ADMIN LISTENER:
☑️ Extends BaseNotificationListener<AdminNotification>
☑️ @RabbitListener(queues = "notification.admin")
☑️ Implementa: existsByEventId() → AdminNotificationDAO
☑️ Implementa: persistNotification() → DAO.save()
☑️ Implementa: attemptWebSocketSend() → webSocketSender.sendAdminNotification()
☑️ Implementa: getTypeSpecificOrchestrator() → factory.getOrchestrator("ADMIN")
```

---

## 🎓 CONCLUSIONE

**È CORRETTISSIMO.** Questo è lo standard in **enterprise Java/Spring** per evitare code duplication quando hai algoritmi simili ma con variazioni specifiche di tipo.

Se dovessi aggiungere un nuovo listener (es. `SupplierNotificationListener`), semplicemente:
1. Crei una nuova classe che estende `BaseNotificationListener<SupplierNotification>`
2. Implementi i 5 metodi astratti
3. FINE! Il loop è già nel base, tutto funziona.

**ZERO code duplication, massima maintainability.** ✅
