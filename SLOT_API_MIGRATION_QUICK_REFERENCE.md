# Legacy Slot API → New ServiceVersion Schedule API: Quick Reference

**Use this as a cheat sheet for migration**

---

## Endpoint Mapping Quick Reference

### Customer Viewing Availability

| Old Endpoint | New Endpoint | Migration Effort |
|---|---|---|
| `GET /customer/restaurant/{restaurantId}/slots` | `GET /restaurant/schedule/service-version/{serviceVersionId}/availability?date=...` | 🟡 Medium - Response structure different |
| `GET /customer/restaurant/slot/{slotId}` | `GET /restaurant/schedule/time-slot/{timeSlotId}` | 🟡 Medium - Field names changed |

---

### Restaurant Managing Slots

| Old Endpoint | New Endpoint | Migration Effort |
|---|---|---|
| `POST /restaurant/slot/new` | `PUT /restaurant/schedule/slot-config/{serviceVersionId}` | 🔴 High - Complete logic change |
| `DELETE /restaurant/slot/cancel/{slotId}` | `POST /restaurant/schedule/deactivate/{serviceVersionId}?fromDate=...` | 🟡 Medium - Different semantics |

---

### Advanced Management

| Old Endpoint | New Endpoint | Migration Effort |
|---|---|---|
| `POST /api/restaurant/slot-transitions/change-schedule` | `PUT /restaurant/schedule/slot-config/{serviceVersionId}` | 🔴 High - Request format completely different |
| `GET /api/restaurant/slot-transitions/active-slots/service/{serviceId}` | `GET /restaurant/schedule/active-slots/service-version/{serviceVersionId}?date=...` | 🟡 Medium - Response structure different |
| `GET /api/restaurant/slot-transitions/can-modify/{slotId}` | Implicit in new design | 🟢 Low - Check reservation count instead |
| `POST /api/restaurant/slot-transitions/deactivate/{slotId}?fromDate=...` | `POST /restaurant/schedule/deactivate/{serviceVersionId}?fromDate=...` | 🟡 Medium - Semantics slightly different |
| `POST /api/restaurant/slot-transitions/reactivate/{slotId}` | `POST /restaurant/schedule/reactivate/{serviceVersionId}` | 🟡 Medium - Same semantics, different params |

---

## Data Model Migration

### Old: Slot Table

```sql
SELECT 
  id,              -- Unique slot ID
  service_id,      -- Which service (Lunch/Dinner)
  slot_date,       -- Date this slot is for
  start_time,      -- When it starts
  end_time,        -- When it ends
  capacity,        -- Max reservations
  is_active
FROM slot;
```

### New: ServiceVersionDay + ServiceVersionSlotConfig

```sql
-- Template: how to generate slots
SELECT 
  sc.slot_duration_minutes,
  sc.buffer_time_minutes,
  sc.daily_start_time,
  sc.daily_end_time,
  sc.max_capacity_per_slot
FROM service_version_slot_config sc;

-- Schedule: which days are active
SELECT 
  svd.day_of_week,        -- MON-SUN
  svd.operating_start_time,
  svd.operating_end_time,
  svd.is_active
FROM service_version_day svd;

-- Exceptions: dates that differ from template
SELECT 
  start_date,
  end_date,
  exception_type,  -- CLOSURE, REDUCED_HOURS, SPECIAL_EVENT
  start_time,
  end_time
FROM availability_exception;
```

---

## Code Migration Examples

### Example 1: Get Available Slots for a Date

**OLD CODE:**
```java
@RestController
public class SlotController {
    @Autowired SlotService slotService;
    
    @GetMapping("/restaurant/{restaurantId}/available")
    public List<SlotDTO> getAvailable(@PathVariable Long restaurantId) {
        return slotService.findSlotsByRestaurantId(restaurantId);
    }
}
```

**NEW CODE:**
```java
@RestController
public class ScheduleController {
    @Autowired ServiceVersionScheduleService scheduleService;
    
    @GetMapping("/schedule/restaurant/{restaurantId}")
    public List<TimeSlotDto> getAvailable(
            @PathVariable Long restaurantId,
            @RequestParam LocalDate date,
            @RequestParam(defaultValue = "Europe/Rome") String timeZone) {
        return scheduleService.getActiveTimeSlotsForDate(restaurantId, date, timeZone);
    }
}
```

---

### Example 2: Create Slots (BIGGEST CHANGE)

**OLD CODE - Create Individual Slots:**
```java
@PostMapping("/slots")
public SlotDTO createSlot(@RequestBody RestaurantNewSlotDTO dto) {
    SlotDTO newSlot = slotService.addSlot(rUser.getId(), dto);
    return newSlot;
}

// Called with:
{
  "serviceId": 1,
  "startTime": "12:00",
  "endTime": "13:00",
  "capacity": 4
}

// Creates 1 slot for 1 time. Need to repeat for each time slot!
```

**NEW CODE - Define Template Once:**
```java
@PutMapping("/schedule/{serviceVersionId}")
public ServiceVersionSlotConfigDto updateConfig(
        @PathVariable Long serviceVersionId,
        @RequestBody ServiceVersionSlotConfigDto dto) {
    return scheduleService.updateSlotConfiguration(serviceVersionId, dto);
}

// Called with:
{
  "slotDurationMinutes": 60,
  "bufferTimeMinutes": 15,
  "dailyStartTime": "12:00",
  "dailyEndTime": "22:00",
  "maxCapacityPerSlot": 4,
  "generationRule": "GENERATE_ON_DEMAND"
}

// Configuration AUTOMATICALLY generates all daily slots! ✨
// One request replaces 10+ old requests!
```

---

### Example 3: Deactivate Slots

**OLD CODE - Deactivate 1 Slot:**
```java
@PostMapping("/slots/{slotId}/cancel")
public void cancelSlot(@PathVariable Long slotId) {
    slotService.cancelSlot(slotId);  // Cancels 1 specific slot
}

// To deactivate a whole day? Loop 12+ times for each slot!
```

**NEW CODE - Deactivate Template:**
```java
@PostMapping("/schedule/{serviceVersionId}/deactivate")
public void deactivate(
        @PathVariable Long serviceVersionId,
        @RequestParam LocalDate fromDate) {
    scheduleService.deactivateSchedule(serviceVersionId, fromDate);
}

// Deactivates ENTIRE schedule from that date.
// All future slots auto-deactivated. 1 request! ✨
```

---

## Response Format Comparison

### GET Available Slots

**OLD Response:**
```json
[
  {
    "id": 1,
    "serviceId": 100,
    "startTime": "12:00",
    "endTime": "13:00",
    "capacity": 4,
    "createdAt": "2024-01-01T10:00:00Z"
  }
]
```

**NEW Response:**
```json
[
  {
    "id": "sv_1_slot_001",                    // NEW: Computed ID
    "serviceVersionId": 1,                    // NEW: Uses version not service
    "startTime": "12:00",
    "endTime": "13:00",
    "totalCapacity": 4,
    "availableCapacity": 3,                   // NEW: Current availability
    "bookingCount": 1,                        // NEW: Show how many booked
    "generatedFromConfig": true               // NEW: Indicates templated
  }
]
```

**Key Differences:**
- `serviceId` → `serviceVersionId` (multi-tenancy aware)
- New `availableCapacity` field (real-time)
- New `bookingCount` field (transparency)
- ID format changed to reflect computed nature

---

## Common Mistakes to Avoid

### ❌ DON'T: Try to create individual slots

```java
// WRONG - Old way, won't work well with new API
for (LocalTime time = start; time.isBefore(end); time = time.plusMinutes(60)) {
    RestaurantNewSlotDTO dto = new RestaurantNewSlotDTO();
    dto.setStartTime(time);
    dto.setEndTime(time.plusMinutes(60));
    slotService.addSlot(dto);  // Creates 1 slot at a time
}
```

### ✅ DO: Define config once, slots auto-generate

```java
// RIGHT - New way, define template
ServiceVersionSlotConfigDto config = new ServiceVersionSlotConfigDto();
config.setDailyStartTime("12:00");
config.setDailyEndTime("22:00");
config.setSlotDurationMinutes(60);
config.setBufferTimeMinutes(15);
scheduleService.updateSlotConfiguration(serviceVersionId, config);
// Done! All slots auto-generated ✨
```

---

### ❌ DON'T: Query by slotId for operations

```java
// WRONG - Treating each slot as unique entity
slotService.cancelSlot(12345);  // What if there are 365 slots a year?
```

### ✅ DO: Query by serviceVersion for bulk operations

```java
// RIGHT - Template-level operations
scheduleService.deactivateSchedule(serviceVersionId, fromDate);
// All slots affected automatically ✨
```

---

### ❌ DON'T: Hard-code slot times

```java
// WRONG - If times change, must rewrite code
List<SlotDTO> slots = slotService.getSlotsForDate(2025-01-15, "12:00", "22:00");
```

### ✅ DO: Let configuration handle times

```java
// RIGHT - Configuration is source of truth
LocalDate date = LocalDate.of(2025, 1, 15);
List<TimeSlotDto> slots = scheduleService.getActiveTimeSlotsForDate(
    serviceVersionId, date);
// Times read from ServiceVersionSlotConfig ✨
```

---

## Migration Phases Checklist

### ✅ Phase 1: Preparation (Complete)
- [x] Understand new architecture
- [x] Review this migration guide
- [x] Read `SLOT_DEPRECATION_MIGRATION_GUIDE.md`

### ⏳ Phase 2: Development
- [ ] Update API clients to new endpoints
- [ ] Change response parsing logic
- [ ] Test with new ServiceVersionScheduleController
- [ ] Verify reservations still work

### ⏳ Phase 3: Testing
- [ ] Unit tests for new integration
- [ ] Integration tests end-to-end
- [ ] Load testing (new arch should be faster)
- [ ] Backward compatibility testing

### ⏳ Phase 4: Deployment
- [ ] Deploy v2.1 with new endpoints
- [ ] Monitor deprecated endpoint usage
- [ ] Collect client feedback

### ⏳ Phase 5: Sunset (v3.0)
- [ ] Remove deprecated endpoints
- [ ] Delete old Slot code
- [ ] Update documentation

---

## Performance Implications

| Operation | Old (Per-Slot) | New (Template) | Improvement |
|-----------|---|---|---|
| Create schedule for 1 year | 365 requests | 1 request | **365x faster** 🚀 |
| Query slots for month | N/A (generated on-fly) | 1 query + computation | **Efficient** ✨ |
| Add exception (holiday) | Manually delete 10+ slots | 1 request | **10x faster** 🚀 |
| Deactivate for month | Delete 30+ slots | 1 request | **30x faster** 🚀 |
| Database queries | O(slots) | O(1) | **Linear improvement** ✨ |

---

## Support & Examples

**Full Examples:**
- See `SLOT_DEPRECATION_MIGRATION_GUIDE.md` → Complete Code Examples section

**API Documentation:**
- Swagger UI: `/swagger-ui.html`
- Search: "ServiceVersionScheduleController"

**Integration Tests:**
- Location: `ServiceVersionScheduleControllerTest.java`
- Shows real working examples

**Questions:**
- Open issue in project
- Include: What you're trying to do + current code
- Reference: This guide + migration guide

---

## Final Tips

1. **Read the full migration guide** - This is just a quick reference
2. **Test both old and new simultaneously** - Gradual migration is safer
3. **Monitor logs** - Deprecated warnings show usage patterns
4. **Plan for 6 months** - Don't rush the migration
5. **Update clients incrementally** - Batch updates risk breaking things

**Most Important:** The new architecture is **much simpler** once you understand the template-based approach. One template replaces dozens of individual slots!

Good luck with your migration! 🎉
