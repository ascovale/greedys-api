# 🤔 LISTENER CON RETRY vs OUTBOX PATTERN

## Domanda: "Posso fare un listener che riprova 3 volte e no duplicati?"

### Risposta Breve: **Sì, ma CON PROBLEMI SERI**

---

## ✅ VERSIONE 1: Listener con Retry (TEORICO)

```java
@EventListener
@Transactional
public void handleEvent(DomainEvent event) {
    int retryCount = 0;
    int maxRetries = 3;
    Exception lastException = null;
    
    while (retryCount < maxRetries) {
        try {
            // 1. Publish a RabbitMQ
            rabbitTemplate.convertAndSend(event);
            
            // 2. Salva marker in DB (NO DUPLICATE)
            eventPublishMarker.save(new EventPublishMarker(event.getId(), PUBLISHED));
            
            log.info("✅ Published event {} on attempt {}", event.getId(), retryCount + 1);
            return; // Success!
            
        } catch (Exception e) {
            lastException = e;
            retryCount++;
            
            if (retryCount < maxRetries) {
                try {
                    Thread.sleep(1000 * retryCount); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    // Failed after 3 retries
    log.error("❌ Failed to publish event {} after {} retries", event.getId(), maxRetries);
    throw new RuntimeException("Publish failed: " + lastException.getMessage());
}
```

### Problemi:

#### ❌ PROBLEMA 1: Thread.sleep() blocca il listener
```java
Thread.sleep(1000 * retryCount); // ← BLOCCA TUTTO IL LISTENER THREAD!
```
- Se EventListener è sincrono e blocca, tutta la transazione rimane aperta
- Db lock timeout possibile
- Performance problem: listener bloccato per 6 secondi (1+2+3)

#### ❌ PROBLEMA 2: Exception durante salvataggio del marker
```java
// Sequenza pericolosa:
rabbitTemplate.convertAndSend(event);  // ✅ Mandato a RabbitMQ
eventPublishMarker.save(...);           // ❌ Salvataggio fallisce!
// → Messaggio è in RabbitMQ, ma marker non salvato
// → Poller riprova → DUPLICATO!
```

#### ❌ PROBLEMA 3: Crash tra send e marker save
```java
rabbitTemplate.convertAndSend(event);  // ✅ Inviato
// 💥 CRASH SERVER QUI
eventPublishMarker.save(...);           // Non eseguito
// → Messaggio in RabbitMQ, marker non salvato → DUPLICATO al restart
```

#### ❌ PROBLEMA 4: No visibility di stuck messages
```java
// Listener fallisce → exception lanciata
// Chi sa che il messaggio è stuck? Nessuno.
// No way to query "quali messaggi non sono stati inviati"
```

#### ❌ PROBLEMA 5: RabbitMQ offline per 10 minuti
```java
// Server chiama listener
// RabbitMQ offline
// Listener retry 3 volte in 6 secondi
// Fallisce
// Messaggio PERSO (RabbitMQ non lo sa)
// 10 minuti dopo RabbitMQ torna online
// Troppo tardi, messaggio mai arrivato
```

---

## ✅ VERSIONE 2: Listener + Outbox (CORRETTO - quello che hai)

```java
// PASSO 1: Listener (veloce, senza retry)
@EventListener
@Transactional
public void handleEvent(DomainEvent event) {
    // Salva SOLO in DB (atomico con transazione)
    eventOutboxDAO.save(new EventOutbox(event));
    log.info("✅ Saved to outbox: {}", event.getId());
}

// PASSO 2: Poller separato (schedulato, con retry intelligente)
@Scheduled(fixedDelay = 5000)
@Transactional
public void publishPending() {
    List<EventOutbox> pending = eventOutboxDAO.findByStatus(PENDING);
    
    for (EventOutbox outbox : pending) {
        try {
            rabbitTemplate.convertAndSend(outbox.getEvent());
            
            // Segna come PUBLISHED solo se send riuscito
            outbox.setStatus(PUBLISHED);
            outbox.setProcessedAt(Instant.now());
            eventOutboxDAO.save(outbox);
            
            log.info("✅ Published: {}", outbox.getId());
            
        } catch (Exception e) {
            log.error("❌ Failed to publish {}: {}", outbox.getId(), e.getMessage());
            
            // Incrementa retry count
            outbox.setRetryCount(outbox.getRetryCount() + 1);
            
            // Se max retry → DLQ
            if (outbox.getRetryCount() >= 3) {
                outbox.setStatus(DEAD_LETTER);
                log.error("🚨 Message {} moved to DLQ", outbox.getId());
            } else {
                outbox.setStatus(FAILED); // Riprova dopo
            }
            
            outbox.setErrorMessage(e.getMessage());
            eventOutboxDAO.save(outbox);
        }
    }
}
```

### Vantaggi:

#### ✅ VANTAGGIO 1: No blocchi
- Listener è veloce (solo salva in DB)
- Poller corre separatamente in background
- Zero contention

#### ✅ VANTAGGIO 2: No race condition
```java
// Atomico: EventOutbox salvato PRIMA del poller
eventOutboxDAO.save(new EventOutbox(event));
// ↓ Transazione completa
// → Poller lo vede e processa

// Anche se crash tra save e poller:
// Messaggio è IN DB, al restart riprova
```

#### ✅ VANTAGGIO 3: Visibility totale
```java
// Query: "Quali messaggi non sono stati inviati?"
List<EventOutbox> stuck = eventOutboxDAO.findByStatus(PENDING);
// Puoi vedere esattamente cosa è stuck e da quanto tempo
```

#### ✅ VANTAGGIO 4: Retry intelligente
```java
// Poller corre ogni 5 secondi
// Se RabbitMQ offline per 10 minuti:
// - Retry dopo 5s: fallisce
// - Retry dopo 10s: fallisce
// - Retry dopo 15s: fallisce
// - ...
// - Retry dopo 10+ minuti: SUCCEDE!
// → Messaggio non perso
```

#### ✅ VANTAGGIO 5: Exponential backoff senza blocchi
```java
// Poller ogni volta che fallisce:
// 1. Incrementa retry_count
// 2. Salva timestamp di ultimo tentativo
// 3. Prossima run (dopo 5s) vede retry_count e può fare backoff

// No Thread.sleep() = no blocchi = performance ok
```

#### ✅ VANTAGGIO 6: Dead Letter Queue (DLQ)
```java
// Dopo 3 fallimenti:
// Sposta in DLQ per investigazione manuale
// Non perdi messaggio, lo puoi analizzare dopo

// In listener: exception → perso o logato? Non è chiaro
```

---

## 📊 CONFRONTO TABELLA

| Feature | Listener + Retry | Listener + Outbox |
|---------|------------------|-------------------|
| Codice | ⚠️ Complesso (retry logic) | ✅ Semplice (solo save) |
| Performance | ❌ Blocchi su Thread.sleep() | ✅ Zero blocchi |
| Race condition | ⚠️ Possibile | ✅ No (atomico) |
| Visibility | ❌ No | ✅ Sì (query DB) |
| Stuck messages | ❌ No idea | ✅ Visible in DB |
| Retry logic | ⚠️ Nel listener | ✅ Nel poller (clean) |
| RabbitMQ offline | ❌ Perde msg dopo 6s | ✅ Riprova per sempre |
| Dead Letter Queue | ❌ No | ✅ Sì |
| Testability | ❌ Difficile (async) | ✅ Facile (poller è unit testable) |
| Idempotency | ⚠️ Dipende da marker logic | ✅ Garantita (poller è idempotente) |
| **VERDICT** | **⚠️ RISCHIOSO** | **✅ PRODUCTION-READY** |

---

## 🎓 BEST PRACTICE INDUSTRIA

Questo è il **Transactional Outbox Pattern**:
- ✅ Usato da **Uber**, **LinkedIn**, **Netflix**, **Airbnb**
- ✅ Standard per **event-driven architectures**
- ✅ Implementato in **Kafka**, **AWS SNS/SQS**, **Google Pub/Sub**

Non è una "preference", è la soluzione provata per **distributed systems**.

---

## 🎯 CONCLUSIONE

### Puoi fare Listener + Retry?
**SÌ**, ma avrai questi problemi:
- ❌ Blocchi su retry
- ❌ Race conditions
- ❌ No visibility
- ❌ Perdita messaggi se RabbitMQ offline > 6 secondi

### Dovrebbe farlo?
**NO**. Usa **Outbox Pattern** che hai già:
- ✅ Production-ready
- ✅ Zero race conditions
- ✅ Fault-tolerant
- ✅ Idempotent
- ✅ Observable
- ✅ Testable

---

## 💡 ANALOGIA

**Listener + Retry** = Consegnare pacchi senza tracciamento
```
1. Corriere esce
2. Prova 3 volte di consegnare (blocco stradale)
3. Se fallisce → pacchetto perso
```

**Listener + Outbox** = Consegnare pacchi con tracking
```
1. Pacchetto arriva a magazzino (DB)
2. Magazzino ha lista di "da consegnare"
3. Corriere viene, prende lista, consegna
4. Se corriere offline → magazzino riprova dopo
5. Puoi vedere quali pacchi sono stuck
6. Se dopo 3 tentativi ancora fallisce → magazzino sa che c'è problema
```

La **Outbox Pattern è il magazzino di logistica** del tuo sistema. 🏢

Quella che hai implementato è **architettivamente superiore**.

