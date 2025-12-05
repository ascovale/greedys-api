# 🗺️ Notification Architecture Roadmap

## Stato Attuale
- **Data analisi**: 5 Dicembre 2025
- **Branch**: main
- **Ultimo aggiornamento**: 5 Dicembre 2025 - **TUTTE LE FASI COMPLETATE** ✅

---

## 📊 Riepilogo Fasi

| Fase | Descrizione | Stato | Data Completamento |
|------|-------------|-------|-------------------|
| 1 | Estensione routing EventOutboxOrchestrator | ✅ DONE | 5 Dic 2025 |
| 2 | Verifica/Fix SocialOrchestrator | ✅ DONE | 5 Dic 2025 |
| 3 | Verifica/Fix ChatOrchestrator | ✅ DONE | 5 Dic 2025 |
| 4 | Fix NotificationOrchestrator base class | ✅ DONE | 5 Dic 2025 |
| 5 | Fix tutti gli Orchestrator (null-safe) | ✅ DONE | 5 Dic 2025 |
| 6 | Support Events in AdminOrchestrator | ✅ DONE | 5 Dic 2025 |

---

## ✅ IMPLEMENTAZIONE COMPLETATA

### Modifiche Applicate

#### FASE 1: EventOutboxOrchestrator - Routing Esteso
**File**: `EventOutboxOrchestrator.java`

Aggiunto routing per tutti i 75 EventType:
- `isSocialEvent()` + `determineSocialQueue()` → notification.social.feed / notification.social.events
- `isChatEvent()` + `determineChatQueue()` → notification.chat.direct / group / reservation
- `isRestaurantEventEvent()` + `determineRestaurantEventQueue()` → notification.restaurant / customer
- `isGamificationEvent()` + `determineGamificationQueue()` → notification.restaurant / customer
- `isSupportEvent()` → notification.admin

#### FASE 2: SocialOrchestrator - Null-Safe + Snake_case
**File**: `SocialOrchestrator.java`

- Aggiunto `extractEventType()`, `extractEventId()` con supporto snake_case/camelCase
- Aggiunto `extractPayloadSafe()`, `extractLongSafe()` per null-safety
- Tutti i metodi ora gestiscono payload null senza NullPointerException

#### FASE 3: ChatOrchestrator - Null-Safe + Snake_case
**File**: `ChatOrchestrator.java`

- Stesso pattern di SocialOrchestrator
- `loadRecipients()`, `loadGroupSettings()`, `createNotificationRecord()` ora null-safe
- `generateTitle()`, `generateBody()`, `generateWebSocketDestination()` ora null-safe

#### FASE 4: NotificationOrchestrator (Classe Base) - Helper Null-Safe
**File**: `NotificationOrchestrator.java`

Modificati helper methods ereditati da tutte le sottoclassi:
- `extractString()` → ora restituisce null invece di throw, supporta snake_case/camelCase
- `extractLong()` → ora restituisce null invece di throw, supporta snake_case/camelCase
- `extractPayload()` → ora restituisce null invece di throw
- Nuovo: `toCamelCase()` per conversione `event_type` → `eventType`
- Nuovo: `toSnakeCase()` per conversione `eventType` → `event_type`

#### FASE 5: Tutti gli Orchestrator - createNotificationRecord Null-Safe
**Files**:
- `CustomerOrchestrator.java`
- `RestaurantUserOrchestrator.java`
- `AgencyUserOrchestrator.java`
- `AdminOrchestrator.java`

Tutti i metodi `createNotificationRecord()` ora:
- Controllano `payload != null` prima di accedere
- Usano `payload.getOrDefault()` con fallback a HashMap vuota
- Estraggono title/body in modo null-safe

#### FASE 6: AdminOrchestrator - Support Events
**File**: `AdminOrchestrator.java`

- Aggiunte regole per `SUPPORT_TICKET_CREATED`, `SUPPORT_TICKET_ESCALATED`, `SUPPORT_TICKET_RESOLVED`, `SUPPORT_TICKET_REOPENED`
- Aggiornato `determinePriority()` per eventi SUPPORT

---

## 📋 Routing Finale Completo

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        EVENT ROUTING TABLE                              │
├──────────────────────────────┬──────────────────────────────────────────┤
│ EventType Pattern            │ Target Queue                             │
├──────────────────────────────┼──────────────────────────────────────────┤
│ SOCIAL_NEW_POST              │ notification.social.feed                 │
│ SOCIAL_NEW_STORY             │ notification.social.feed                 │
│ SOCIAL_* (altri)             │ notification.social.events               │
├──────────────────────────────┼──────────────────────────────────────────┤
│ CHAT_MESSAGE_RECEIVED        │ notification.chat.direct                 │
│ CHAT_GROUP_MESSAGE           │ notification.chat.group                  │
│ CHAT_RESERVATION_MESSAGE     │ notification.chat.reservation            │
│ CHAT_* (altri)               │ notification.chat.direct                 │
├──────────────────────────────┼──────────────────────────────────────────┤
│ EVENT_NEW_RSVP               │ notification.restaurant                  │
│ EVENT_* (altri)              │ notification.customer                    │
├──────────────────────────────┼──────────────────────────────────────────┤
│ CHALLENGE_REGISTRATION_*     │ notification.restaurant                  │
│ TOURNAMENT_REGISTRATION_*    │ notification.restaurant                  │
│ CHALLENGE/TOURNAMENT (altri) │ notification.customer                    │
├──────────────────────────────┼──────────────────────────────────────────┤
│ SUPPORT_*                    │ notification.admin                       │
├──────────────────────────────┼──────────────────────────────────────────┤
│ aggregateType: RESTAURANT    │ notification.restaurant.user             │
│ aggregateType: CUSTOMER      │ notification.customer                    │
│ aggregateType: AGENCY        │ notification.agency                      │
│ aggregateType: ADMIN         │ notification.admin                       │
└──────────────────────────────┴──────────────────────────────────────────┘
```

---

## 🎯 Benefici Ottenuti

1. **Zero NullPointerException** - Tutti gli orchestrator gestiscono messaggi con payload null/mancante
2. **Compatibilità snake_case/camelCase** - RabbitMQ messages possono usare entrambi i formati
3. **Routing completo** - Tutti i 75 EventType vengono instradati correttamente
4. **Codice consistente** - Pattern uniforme in tutti gli orchestrator
5. **Facilità debug** - Log dettagliati con emoji per ogni fase di routing

---

## � Dettagli Implementazione per Riferimento

<details>
<summary>📌 FASE 1: EventOutboxOrchestrator Routing (click per espandere)</summary>

### Mapping Routing Implementato

```
eventType.startsWith("SOCIAL_")
  ├─ SOCIAL_NEW_POST, SOCIAL_NEW_STORY → notification.social.feed
  └─ Altri (LIKED, COMMENTED, etc.) → notification.social.events

eventType.startsWith("CHAT_")
  ├─ CHAT_MESSAGE_RECEIVED → notification.chat.direct
  ├─ CHAT_GROUP_MESSAGE → notification.chat.group
  ├─ CHAT_RESERVATION_MESSAGE → notification.chat.reservation
  └─ CHAT_TYPING_INDICATOR → SKIP (WebSocket diretto, no outbox)

eventType.startsWith("EVENT_")
  ├─ EVENT_NEW_RSVP → notification.restaurant
  └─ Altri → notification.customer

eventType.startsWith("CHALLENGE_") || eventType.startsWith("TOURNAMENT_")
  ├─ *_REGISTRATION_* → notification.restaurant
  └─ Altri → notification.customer

eventType.startsWith("SUPPORT_")
  └─ → notification.admin

aggregateType fallback:
  RESTAURANT → notification.restaurant.user
  CUSTOMER → notification.customer
  AGENCY → notification.agency
  ADMIN → notification.admin
  SOCIALPOST, CHAT, CHALLENGE, etc. → notification.customer (default)
```

</details>

<details>
<summary>📌 FASE 2-3: Social/Chat Orchestrator Pattern (click per espandere)</summary>

### Helper Methods Null-Safe

```java
// Supporto snake_case e camelCase
private String extractEventType(Map<String, Object> message) {
    Object eventType = message.get("event_type");
    if (eventType == null) {
        eventType = message.get("eventType");
    }
    return (eventType instanceof String) ? (String) eventType : null;
}

// Null-safe extraction
private Map<String, Object> extractPayloadSafe(Map<String, Object> message) {
    if (message == null) return null;
    Object payload = message.get("payload");
    return (payload instanceof Map) ? (Map<String, Object>) payload : null;
}

private Long extractLongSafe(Map<String, Object> map, String key) {
    if (map == null) return null;
    Object value = map.get(key);
    return (value instanceof Number) ? ((Number) value).longValue() : null;
}
```

</details>

<details>
<summary>📌 FASE 4: NotificationOrchestrator Base Class (click per espandere)</summary>

### Helper Methods nella Classe Base

```java
// Tutti gli orchestrator ereditano questi metodi
protected String extractString(Map<String, Object> map, String key) {
    if (map == null) return null;
    // Try snake_case first, then camelCase
    Object value = map.get(key);
    if (value == null) value = map.get(toCamelCase(key));
    if (value == null) value = map.get(toSnakeCase(key));
    return (value instanceof String) ? (String) value : null;
}

protected Long extractLong(Map<String, Object> map, String key) {
    // Same pattern...
}

protected String toCamelCase(String snakeCase) {
    // event_type -> eventType
}

protected String toSnakeCase(String camelCase) {
    // eventType -> event_type
}
```

</details>

<details>
<summary>📌 FASE 6: AdminOrchestrator Support Events (click per espandere)</summary>

### Event Rules per Support Tickets

```java
case "SUPPORT_TICKET_CREATED" -> Map.of(
    "mandatory", List.of("EMAIL", "WEBSOCKET"),
    "optional", List.of("PUSH", "SLACK")
);
case "SUPPORT_TICKET_ESCALATED" -> Map.of(
    "mandatory", List.of("EMAIL", "SMS"),
    "optional", List.of("PUSH", "SLACK", "WEBSOCKET")
);
case "SUPPORT_TICKET_RESOLVED" -> Map.of(
    "mandatory", List.of("EMAIL"),
    "optional", List.of("PUSH", "WEBSOCKET")
);
```

### Priority per Support Events

```java
case "SUPPORT_TICKET_ESCALATED" -> NotificationPriority.HIGH;
case "SUPPORT_TICKET_CREATED", "SUPPORT_TICKET_REOPENED" -> NotificationPriority.NORMAL;
case "SUPPORT_TICKET_RESOLVED" -> NotificationPriority.LOW;
```

</details>
---

## 📋 Checklist Implementazione

- [x] Analisi architettura attuale
- [x] Mapping eventi → code
- [x] Identificazione file da modificare
- [x] **FASE 1**: Routing EventOutboxOrchestrator ✅ 5 Dic 2025
- [x] **FASE 2**: SocialOrchestrator null-safe ✅ 5 Dic 2025
- [x] **FASE 3**: ChatOrchestrator null-safe ✅ 5 Dic 2025
- [x] **FASE 4**: NotificationOrchestrator base class ✅ 5 Dic 2025
- [x] **FASE 5**: Tutti gli orchestrator null-safe ✅ 5 Dic 2025
- [x] **FASE 6**: Support events in AdminOrchestrator ✅ 5 Dic 2025
- [x] **FASE 7**: Documentazione aggiornata ✅ 5 Dic 2025

---

## � File Modificati

| File | Modifiche |
|------|-----------|
| `EventOutboxOrchestrator.java` | +150 righe routing, +10 helper methods |
| `NotificationOrchestrator.java` | Helper null-safe, snake_case/camelCase |
| `SocialOrchestrator.java` | Null-safe, snake_case support |
| `ChatOrchestrator.java` | Null-safe, snake_case support |
| `CustomerOrchestrator.java` | createNotificationRecord null-safe |
| `RestaurantUserOrchestrator.java` | createNotificationRecord null-safe |
| `AgencyUserOrchestrator.java` | createNotificationRecord null-safe |
| `AdminOrchestrator.java` | createNotificationRecord null-safe, SUPPORT events |

---

## �🚀 Comandi Utili

```bash
# Avviare app in dev-minimal
./dev-minimal.sh

# Testare routing (dopo implementazione)
# Creare un post social e verificare che vada nella coda corretta
```

---

*Documento creato: 5 Dicembre 2025*
*Ultimo aggiornamento: 5 Dicembre 2025 - IMPLEMENTAZIONE COMPLETATA*
