# Slot Architecture Deprecation & Migration Guide

**Last Updated:** 2025  
**Status:** Legacy Slot API marked as **DEPRECATED** since v2.0  
**Removal Timeline:** Will be removed in v3.0 (planned for Q2 2025)  
**Deprecation Period:** 6 months (v2.0 → v3.0)

## Overview

The legacy Slot-based scheduling system has been replaced with a more flexible and maintainable **ServiceVersion schedule architecture** using:

- **ServiceVersionDay**: Weekly schedule (7 days per ServiceVersion)
- **ServiceVersionSlotConfig**: Configurable slot generation parameters
- **AvailabilityException**: Closures, reduced hours, special events
- **TimeSlot**: Computed available time slots

### Why the Migration?

| Aspect | Legacy Slot | New ServiceVersionDay |
|--------|-------------|----------------------|
| **Flexibility** | Fixed schema | Hierarchical & extensible |
| **Multi-tenancy** | Service-level only | ServiceVersion temporal isolation |
| **Schedule Changes** | Manual slot creation | Template-based generation |
| **Exceptions** | Limited closure support | Full availability control |
| **Performance** | Individual slot lookups | Efficient batch operations |
| **Maintenance** | High complexity | Clean separation of concerns |

---

## Controller Migration Map

### 1. Customer Reading Slots

**Old Endpoints (DEPRECATED):**
```
GET  /customer/restaurant/{restaurantId}/slots
GET  /customer/restaurant/slot/{slotId}
```

**New Endpoints:**
```
GET  /customer/schedule/service-version/{serviceVersionId}/availability
  ?date=2025-01-15
  &timeZone=Europe/Rome

GET  /customer/schedule/time-slot/{timeSlotId}
```

**Code Example - Before:**
```java
// DEPRECATED
RestTemplate template = new RestTemplate();
List<SlotDTO> slots = template.getForObject(
    "/customer/restaurant/{restaurantId}/slots",
    List.class, restaurantId
);
```

**Code Example - After:**
```java
// NEW
RestTemplate template = new RestTemplate();
List<TimeSlotDto> slots = template.getForObject(
    "/customer/schedule/service-version/{serviceVersionId}/availability" +
    "?date={date}&timeZone={timeZone}",
    List.class, serviceVersionId, LocalDate.now(), "Europe/Rome"
);
```

---

### 2. Restaurant Managing Slots

#### 2.1 Creating Slots

**Old Endpoint (DEPRECATED):**
```
POST /restaurant/slot/new
Body: RestaurantNewSlotDTO
```

**Old Request:**
```json
{
  "serviceId": 1,
  "startTime": "12:00",
  "endTime": "13:00",
  "capacity": 4,
  "bufferTime": 15
}
```

**New Approach:**
Use `ServiceVersionSlotConfig` to define generation rules instead of creating individual slots.

**New Endpoint:**
```
PUT /restaurant/schedule/slot-config/{serviceVersionId}
Body: ServiceVersionSlotConfigDto
```

**New Request:**
```json
{
  "slotDurationMinutes": 60,
  "bufferTimeMinutes": 15,
  "dailyStartTime": "12:00",
  "dailyEndTime": "22:00",
  "maxCapacityPerSlot": 4,
  "generationRule": "GENERATE_ON_DEMAND"
}
```

#### 2.2 Canceling Slots

**Old Endpoint (DEPRECATED):**
```
DELETE /restaurant/slot/cancel/{slotId}
```

**Old Behavior:** Remove individual slot

**New Endpoint:**
```
POST /restaurant/schedule/deactivate/{serviceVersionId}
?fromDate=2025-02-01
```

**New Behavior:** Deactivate entire schedule template from a date

---

### 3. Advanced Slot Management (SlotTransitionController)

#### 3.1 Changing Slot Times

**Old Endpoint (DEPRECATED):**
```
POST /api/restaurant/slot-transitions/change-schedule
Body: SlotScheduleChangeRequest
```

**Old Request:**
```json
{
  "slotId": 1,
  "newStartTime": "12:30",
  "newEndTime": "13:30",
  "effectiveDate": "2025-01-15",
  "changePolicy": "CANCEL_FUTURE_RESERVATIONS"
}
```

**New Endpoint:**
```
PUT /restaurant/schedule/slot-config/{serviceVersionId}
Body: ServiceVersionSlotConfigDto
```

**New Request:**
```json
{
  "dailyStartTime": "12:30",
  "dailyEndTime": "22:30",
  "slotDurationMinutes": 60,
  "bufferTimeMinutes": 15
}
```

**Key Difference:** Template-based approach means the change applies to all future slots automatically (no need for change policies).

#### 3.2 Getting Active Slots

**Old Endpoint (DEPRECATED):**
```
GET /api/restaurant/slot-transitions/active-slots/service/{serviceId}
?date=2025-01-15
```

**New Endpoint:**
```
GET /restaurant/schedule/active-slots/service-version/{serviceVersionId}
?date=2025-01-15&restaurantId={restaurantId}
```

**Response Change:**

Old Response:
```json
[
  {
    "id": 1,
    "startTime": "12:00",
    "endTime": "13:00",
    "capacity": 4
  }
]
```

New Response:
```json
[
  {
    "id": "sv_1_slot_001",
    "serviceVersionId": 1,
    "startTime": "12:00",
    "endTime": "13:00",
    "availableCapacity": 3,
    "totalCapacity": 4,
    "bookingCount": 1
  }
]
```

#### 3.3 Checking Slot Modifiability

**Old Endpoint (DEPRECATED):**
```
GET /api/restaurant/slot-transitions/can-modify/{slotId}
```

**Old Response:**
```json
{
  "canModify": true,
  "futureReservationsCount": 0,
  "message": "Slot can be modified safely"
}
```

**New Approach:**

Slot modification is now implicit. Templates can be modified at any time:
- **Future slots only affected** if configuration changes (automatic regeneration)
- **Existing reservations preserved** in their assigned time slots
- **No conflict checking needed** (system handles overlaps)

**New Check Endpoint (Optional):**
```
GET /restaurant/schedule/reservation-count/{serviceVersionId}
?fromDate=2025-02-01&toDate=2025-02-28
```

Response:
```json
{
  "totalReservations": 42,
  "upcomingReservations": 15,
  "pastReservations": 27,
  "canModifyConfig": true
}
```

#### 3.4 Deactivating Slots

**Old Endpoint (DEPRECATED):**
```
POST /api/restaurant/slot-transitions/deactivate/{slotId}
?fromDate=2025-02-01
```

**New Endpoint:**
```
POST /restaurant/schedule/deactivate/{serviceVersionId}
?fromDate=2025-02-01&restaurantId={restaurantId}
```

**Semantics:**
- **Old**: Deactivate specific slot instance
- **New**: Deactivate the entire schedule template (all future slots)

#### 3.5 Reactivating Slots

**Old Endpoint (DEPRECATED):**
```
POST /api/restaurant/slot-transitions/reactivate/{slotId}
```

**New Endpoint:**
```
POST /restaurant/schedule/reactivate/{serviceVersionId}
?restaurantId={restaurantId}
```

---

## Data Structure Comparisons

### TimeSlot Query

**Old Way (Individual Slots):**
```sql
SELECT s.* FROM slot s
WHERE s.service_id = ?
  AND s.slot_date = ?
  AND s.is_active = true
ORDER BY s.start_time;
```

**New Way (Template + Generation):**
```sql
-- Template configuration
SELECT sc.* FROM service_version_slot_config sc
WHERE sc.service_version_id = ?;

-- Day schedule
SELECT svd.* FROM service_version_day svd
WHERE svd.service_version_id = ?
  AND svd.day_of_week = ?;

-- Exceptions on that date
SELECT ae.* FROM availability_exception ae
WHERE ae.service_version_id = ?
  AND ae.start_date <= ?
  AND ae.end_date >= ?;
```

### Schedule Definition

**Old Schema:**
```
Slot Table:
├── id (PK)
├── service_id (FK)
├── start_time
├── end_time
├── capacity
├── is_active
└── created_at
```

**New Schema:**
```
ServiceVersionDay (7 records per ServiceVersion):
├── id (PK)
├── service_version_id (FK)
├── day_of_week (ENUM: MON-SUN)
├── is_active (boolean)
├── operating_start_time
├── operating_end_time
└── [auditing fields]

ServiceVersionSlotConfig (per ServiceVersion):
├── id (PK)
├── service_version_id (FK)
├── slot_duration_minutes
├── buffer_time_minutes
├── max_capacity_per_slot
├── generation_rule
└── [auditing fields]

AvailabilityException (dynamic):
├── id (PK)
├── service_version_id (FK)
├── exception_type (ENUM: CLOSURE, REDUCED_HOURS, SPECIAL_EVENT)
├── start_date
├── end_date
├── start_time (nullable)
├── end_time (nullable)
├── reason
└── [auditing fields]
```

---

## Migration Checklist

### Phase 1: Preparation (Week 1-2)
- [ ] Upgrade to v2.0 (legacy endpoints still work with @Deprecated warnings)
- [ ] Review API logs for deprecated endpoint usage
- [ ] Audit your API clients for calls to:
  - `GET /customer/restaurant/{restaurantId}/slots`
  - `POST /restaurant/slot/new`
  - `DELETE /restaurant/slot/cancel/{slotId}`
  - `POST /api/restaurant/slot-transitions/*`

### Phase 2: Integration (Week 3-6)
- [ ] Update client code to use new `ServiceVersionScheduleController` endpoints
- [ ] Map your slot creation logic → ServiceVersionSlotConfig
- [ ] Update UI to show new response format
- [ ] Test with new endpoints alongside old ones (dual-mode)
- [ ] Run integration tests

### Phase 3: Validation (Week 7-8)
- [ ] Verify all reservations still work with new scheduling
- [ ] Performance testing (batch operations should be faster)
- [ ] Load testing (new architecture handles concurrent requests)
- [ ] Backup existing data (Slot table) for rollback if needed

### Phase 4: Cutover (Week 9-10)
- [ ] Remove calls to deprecated endpoints from production
- [ ] Monitor logs for any remaining deprecated API usage
- [ ] Document any custom extensions of legacy SlotService

### Phase 5: Cleanup (After v3.0 Release)
- [ ] Delete legacy Slot-related tables (Slot, ClosedSlot)
- [ ] Remove deprecated controller classes
- [ ] Remove SlotService, SlotDAO, SlotMapper

---

## Troubleshooting

### Issue: "Cannot find TimeSlots for date X"
**Cause:** ServiceVersionDay not configured for that day of week  
**Solution:** Check ServiceVersionDay records for the serviceVersionId; ensure is_active=true for target day

### Issue: "Reservation conflicts with availability exception"
**Cause:** AvailabilityException overlaps with reservation time  
**Solution:** Review AvailabilityException.exception_type; adjust exception date range or reservation date

### Issue: "Old endpoints return 404"
**Cause:** May have already removed deprecated endpoints  
**Solution:** Check if you're on v3.0+; migrate all clients to new endpoints

### Issue: "Response structure changed"
**Cause:** Old SlotDTO vs new TimeSlotDto have different fields  
**Solution:** Update JSON parsing; use new field names (see examples above)

---

## Support & Questions

For migration support:
1. **API Documentation**: Swagger UI at `/swagger-ui.html` shows all new endpoints
2. **Code Examples**: See `com.application.restaurant.controller.restaurant.ServiceVersionScheduleController`
3. **Database Schema**: See Flyway migrations `V7__create_service_version_schedule_tables.sql`
4. **Integration Tests**: Check `ServiceVersionScheduleControllerTest.java`

---

## Timeline

| Date | Milestone | Action |
|------|-----------|--------|
| Now (v2.0) | Deprecation Announced | Old endpoints marked @Deprecated; warnings in logs |
| v2.1 | Migration Period | New endpoints fully functional; old still supported |
| v2.5 | Hard Cutover | Stop supporting legacy Slot endpoints (errors instead of warnings) |
| v3.0 (Q2 2025) | Final Removal | Legacy Slot tables/classes deleted |

---

## Version Compatibility Matrix

```
Client Version | v2.0 | v2.1 | v2.5 | v3.0
Old Endpoints  | ✅*  | ✅*  | ❌   | ❌
New Endpoints  | ✅   | ✅   | ✅   | ✅
Legend: ✅ = Works, ❌ = Removed, * = Warnings in logs
```

---

## Complete Example: Migrating Reservation Query

### Old Way (v1.x):
```java
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    @Autowired SlotService slotService;
    
    @GetMapping("/by-slot/{slotId}")
    public List<Reservation> getReservationsBySlot(@PathVariable Long slotId) {
        Slot slot = slotService.findById(slotId);
        return reservationRepository.findBySlot(slot);
    }
}
```

### New Way (v2.0+):
```java
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    @Autowired ServiceVersionScheduleService scheduleService;
    
    @GetMapping("/by-time-slot/{timeSlotId}")
    public List<Reservation> getReservationsByTimeSlot(@PathVariable String timeSlotId) {
        // TimeSlot is computed from ServiceVersionDay + SlotConfig
        // Reservations reference serviceVersion + time window
        return reservationRepository.findByServiceVersionAndTimeWindow(
            timeSlotId.getServiceVersionId(),
            timeSlotId.getStartTime(),
            timeSlotId.getEndTime()
        );
    }
}
```

---

## Next Steps

1. ✅ **Read this guide completely**
2. ✅ **Test new endpoints in development**
3. ✅ **Update your client integrations**
4. ✅ **Schedule migration for your system**
5. ✅ **Remove old endpoint calls before v3.0**

Welcome to the new scheduling architecture! 🎉
