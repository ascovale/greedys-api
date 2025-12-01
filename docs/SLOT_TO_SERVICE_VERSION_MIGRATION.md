# Slot → ServiceVersion Migration Guide

## Overview

La vecchia architettura **Slot** è stata sostituita dalla nuova architettura **ServiceVersion** che offre:
- Configurazione template-based (configura una volta, slot generati automaticamente)
- Isolamento temporale (ogni service version ha schedule indipendente)
- Eccezioni flessibili (chiusure, orari ridotti, eventi speciali)
- Slot calcolati on-demand (non pre-stored)

---

## 📋 Endpoint Migration Table

### RestaurantSlotController.java → ServiceVersionScheduleController.java

| # | Vecchio Endpoint | Nuovo Endpoint | Parametri Vecchi | Parametri Nuovi | Note |
|---|------------------|----------------|------------------|-----------------|------|
| 1 | `POST /restaurant/slot/new` | `PUT /restaurant/schedule/slot-config/{serviceVersionId}` | `@RequestBody RestaurantNewSlotDTO slotDto, @AuthenticationPrincipal RUser` | `@PathVariable serviceVersionId, @RequestBody ServiceVersionSlotConfigDto, @RequestParam restaurantId, @AuthenticationPrincipal RUser` | Nuovo approccio: configura slot tramite SlotConfig invece di creare singoli slot. ServiceVersionId identifica la versione del servizio. |
| 2 | `DELETE /restaurant/slot/cancel/{slotId}` | `POST /restaurant/schedule/deactivate/{serviceVersionId}` | `@PathVariable slotId, @AuthenticationPrincipal RUser` | `@PathVariable serviceVersionId, @RequestParam fromDate, @RequestParam restaurantId, @AuthenticationPrincipal RUser` | Nuovo approccio: disattiva l'intero schedule da una data, non singoli slot. Richiede fromDate per specificare da quando. |

---

### SlotTransitionController.java → ServiceVersionScheduleController.java

| # | Vecchio Endpoint | Nuovo Endpoint | Parametri Vecchi | Parametri Nuovi | Note |
|---|------------------|----------------|------------------|-----------------|------|
| 3 | `POST /api/restaurant/slot-transitions/change-schedule` | `PUT /restaurant/schedule/slot-config/{serviceVersionId}` | `@RequestBody SlotScheduleChangeRequest (slotId, newStartTime, newEndTime, effectiveDate, changePolicy)` | `@PathVariable serviceVersionId, @RequestBody ServiceVersionSlotConfigDto (slotDurationMinutes, slotBufferMinutes, defaultStartTime, defaultEndTime), @RequestParam restaurantId, @AuthenticationPrincipal RUser` | Nuovo approccio: aggiorna configurazione slot invece di modificare singoli slot. changePolicy non più necessaria - gestita tramite AvailabilityException. |
| 4 | `GET /api/restaurant/slot-transitions/active-slots/service/{serviceId}` | `GET /restaurant/schedule/active-slots/service-version/{serviceVersionId}` | `@PathVariable serviceId, @RequestParam LocalDate date` | `@PathVariable serviceVersionId, @RequestParam LocalDate date, @RequestParam restaurantId, @AuthenticationPrincipal RUser` | Cambio da serviceId a serviceVersionId. Aggiunta verifica ownership tramite restaurantId e userId. |
| 5 | `GET /api/restaurant/slot-transitions/can-modify/{slotId}` | **RIMOSSO** | `@PathVariable slotId` | N/A | Non più necessario. La nuova architettura gestisce le modifiche tramite template + eccezioni, senza bisogno di verificare singoli slot. |
| 6 | `POST /api/restaurant/slot-transitions/deactivate/{slotId}` | `POST /restaurant/schedule/deactivate/{serviceVersionId}` | `@PathVariable slotId, @RequestParam LocalDate fromDate` | `@PathVariable serviceVersionId, @RequestParam LocalDate fromDate, @RequestParam restaurantId, @AuthenticationPrincipal RUser` | Cambio da slotId a serviceVersionId. Aggiunta ownership check. |
| 7 | `POST /api/restaurant/slot-transitions/reactivate/{slotId}` | `POST /restaurant/schedule/reactivate/{serviceVersionId}` | `@PathVariable slotId` | `@PathVariable serviceVersionId, @RequestParam restaurantId, @AuthenticationPrincipal RUser` | Cambio da slotId a serviceVersionId. Aggiunta ownership check. |

---

### CustomerSlotController.java → CustomerServiceVersionScheduleController.java (DA CREARE)

| # | Vecchio Endpoint | Nuovo Endpoint | Parametri Vecchi | Parametri Nuovi | Note |
|---|------------------|----------------|------------------|-----------------|------|
| 8 | `GET /customer/restaurant/{restaurantId}/slots` | `GET /customer/restaurant/{restaurantId}/schedule` | `@PathVariable restaurantId` | `@PathVariable restaurantId, @RequestParam(required=false) LocalDate date` | Ritorna TimeSlotDto[] invece di SlotDTO[]. Opzionale: filtra per data specifica. |
| 9 | `GET /customer/restaurant/slot/{slotId}` | `GET /customer/restaurant/schedule/timeslot/{serviceVersionId}` | `@PathVariable slotId` | `@PathVariable serviceVersionId, @RequestParam LocalDate date, @RequestParam LocalTime time` | Nuovo approccio: identifica slot tramite serviceVersion + data + ora invece di slotId. Gli slot sono calcolati dinamicamente. |

---

### RestaurantSlotManagementController.java → ServiceVersionScheduleController.java

| # | Vecchio Endpoint | Nuovo Endpoint | Parametri Vecchi | Parametri Nuovi | Note |
|---|------------------|----------------|------------------|-----------------|------|
| 10 | `GET /restaurant/slot/day-slots` | `GET /restaurant/schedule/active-slots/service-version/{serviceVersionId}` | `@RequestParam(required=false) LocalDate date, @AuthenticationPrincipal RUser` | `@PathVariable serviceVersionId, @RequestParam LocalDate date, @RequestParam restaurantId, @AuthenticationPrincipal RUser` | Richiede serviceVersionId esplicito invece di dedurlo dal restaurant. Per ottenere tutti i servizi, iterare sui serviceVersion attivi. |
| 11 | `GET /restaurant/slot/all` | `GET /restaurant/schedule/service-version/{serviceVersionId}` | `@AuthenticationPrincipal RUser` | `@PathVariable serviceVersionId, @RequestParam restaurantId, @AuthenticationPrincipal RUser` | Ritorna weekly schedule (7 giorni) invece di lista slot. Per tutti i servizi usare findActiveByRestaurantId() e iterare. |
| 12 | `GET /restaurant/slot/{slotId}` | **RIMOSSO** | `@PathVariable slotId` | N/A | Non più necessario. Slot sono calcolati dinamicamente senza ID persistente. Usare getActiveTimeSlots con data specifica o getTimeSlotDetails(serviceVersionId, date, time). |

---

## 📋 Complete Controller Status

| Controller | Status | Replacement | Notes |
|------------|--------|-------------|-------|
| `RestaurantSlotController.java` | ✅ DEPRECATED | `ServiceVersionScheduleController.java` | 2 endpoints migrati |
| `SlotTransitionController.java` | ✅ DEPRECATED | `ServiceVersionScheduleController.java` | 5 endpoints migrati, 1 rimosso (can-modify) |
| `CustomerSlotController.java` | ✅ DEPRECATED | `CustomerServiceVersionScheduleController.java` | 2 endpoints migrati |
| `RestaurantSlotManagementController.java` | ✅ DEPRECATED | `ServiceVersionScheduleController.java` | 2 endpoints migrati, 1 rimosso (getById) |

---

## 🗂️ DTO Migration

| Vecchio DTO | Nuovo DTO | Note |
|-------------|-----------|------|
| `SlotDTO` | `TimeSlotDto` | TimeSlotDto rappresenta uno slot calcolato (startTime, endTime, available, capacity). Non ha ID. |
| `RestaurantNewSlotDTO` | `ServiceVersionSlotConfigDto` | Configura come generare slot (durata, buffer, orari default). |
| `SlotScheduleChangeRequest` | `ServiceVersionSlotConfigDto` + `AvailabilityExceptionDto` | Modifiche tramite config update + eccezioni per date specifiche. |
| `SlotScheduleChangeResponse` | Standard `ResponseEntity<ServiceVersionSlotConfigDto>` | Risposta standard con oggetto aggiornato. |
| `SlotModificationCheckResponse` | **RIMOSSO** | Non più necessario nella nuova architettura. |
| `ServiceSlotsDto` | `ServiceVersionDayDto` | Rappresenta schedule di un giorno specifico. |

---

## 🔧 Entity Migration

| Vecchia Entity | Nuova Entity | Note |
|----------------|--------------|------|
| `Slot` | `ServiceVersionSlotConfig` | Config per generazione slot. Non più entity per singolo slot. |
| `SlotChangePolicy` | **RIMOSSO** | Gestito tramite `AvailabilityException.ExceptionType`. |
| `Service.slots` | `ServiceVersion.slotConfig` | Relazione cambiata. Service ha ServiceVersion, che ha SlotConfig. |

---

## 🏗️ Architecture Comparison

### OLD: Slot-based
```
Restaurant
└── Service
    └── Slot[] (pre-created, stored in DB)
        ├── Slot 1: Mon 12:00-12:30
        ├── Slot 2: Mon 12:30-13:00
        └── ... (hundreds of records)
```

### NEW: ServiceVersion-based
```
Restaurant
└── Service
    └── ServiceVersion (template)
        ├── ServiceVersionDay[7] (weekly template)
        │   ├── Monday: active=true, 11:00-15:00, 18:00-22:00
        │   ├── Tuesday: active=true, 11:00-15:00, 18:00-22:00
        │   └── ...
        ├── ServiceVersionSlotConfig
        │   └── duration=30min, buffer=5min
        └── AvailabilityException[] (date-specific overrides)
            ├── Dec 25: type=CLOSED
            └── Dec 31: type=REDUCED_HOURS, 18:00-00:00
                ↓
TimeSlot[] (computed on-demand, NOT stored)
```

---

## 🚨 Breaking Changes

1. **Slot IDs no longer exist** - Use serviceVersionId + date + time to identify a slot
2. **Pre-created slots removed** - Slots are computed dynamically
3. **SlotChangePolicy removed** - Use AvailabilityException instead
4. **Restaurant context required** - All new endpoints require restaurantId for ownership verification

---

## 📅 Migration Timeline

- **v2.0** (Current): New endpoints available, old endpoints deprecated with warnings
- **v3.0** (Q2 2025): Old endpoints removed, Slot entity removed from DB

---

## 🔄 Files to Remove After Migration Complete

### Controllers (DEPRECATED) - Ready for Removal
- [x] `RestaurantSlotController.java` - Replaced by ServiceVersionScheduleController ✅
- [x] `SlotTransitionController.java` - Replaced by ServiceVersionScheduleController ✅
- [x] `CustomerSlotController.java` - Replaced by CustomerServiceVersionScheduleController ✅
- [x] `RestaurantSlotManagementController.java` - Replaced by ServiceVersionScheduleController ✅

### Services - Ready for Removal
- [ ] `SlotService.java` - Replaced by ServiceVersionScheduleService
- [ ] `SlotTransitionService.java` - Replaced by ServiceVersionScheduleService

### Entities & DAOs - Ready for Removal
- [ ] `Slot.java` - Replaced by ServiceVersionSlotConfig
- [ ] `SlotDAO.java` - Replaced by ServiceVersionSlotConfigDAO
- [ ] `SlotChangePolicy.java` - Replaced by AvailabilityException.ExceptionType

### DTOs & Mappers - Ready for Removal
- [ ] `SlotDTO.java` - Replaced by TimeSlotDto
- [ ] `SlotMapper.java` - Replaced by ServiceVersionMapper
- [ ] `ServiceSlotsDto.java` - Replaced by ServiceVersionDayDto
- [ ] `NewSlotDTO.java` - Replaced by ServiceVersionSlotConfigDto
- [ ] `RestaurantNewSlotDTO.java` - Replaced by ServiceVersionSlotConfigDto

### Updates Required Before Removal
- [ ] `Service.java` - Remove `Set<Slot> slots` field
- [ ] `ServiceService.java` - Remove Slot-related methods
- [ ] `RUserPermissionService.java` - Remove Slot permission check
- [ ] `RestaurantDataLoader.java` - Remove Slot creation logic
- [ ] `RestaurantService.java` - Remove getDaySlots() method
