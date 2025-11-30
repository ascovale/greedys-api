# ServiceVersionScheduleService Implementation Roadmap

**Phase:** 2 (After Deprecation - Phase 1)  
**Priority:** HIGH  
**Prerequisite:** Completion of Phase 1 (Deprecation) ✅  
**Estimated Duration:** 2-3 weeks

---

## Overview

The `ServiceVersionScheduleService` will implement the business logic for the new template-based scheduling system. It replaces the functionality scattered across:

- ❌ `SlotService` (legacy)
- ❌ `SlotTransitionService` (legacy)
- ✅ `ServiceVersionScheduleService` (new - TO BE IMPLEMENTED)

---

## Architecture

```
REST Endpoint
    ↓
ServiceVersionScheduleController
    ↓
ServiceVersionScheduleService (TO BUILD)
    ├─→ ServiceVersionDayDAO
    ├─→ ServiceVersionSlotConfigDAO
    ├─→ AvailabilityExceptionDAO
    ├─→ ServiceDAO (for validation)
    ├─→ ServiceVersionDAO (for validation)
    ├─→ ReservationDAO (for conflict detection)
    └─→ TimeSlotGenerator (utility for computation)
```

---

## Service Method Specifications

### 1. getWeeklySchedule()

**Purpose:** Retrieve the template schedule (7 days) for a ServiceVersion

```java
public List<ServiceVersionDayDto> getWeeklySchedule(
    Long serviceVersionId,
    Long restaurantId,
    Long userId
) throws AccessDeniedException, ResourceNotFoundException {
    // Verify user owns restaurant
    // Verify restaurant owns service version
    // Load all 7 ServiceVersionDay records
    // Convert to DTOs
    // Return sorted by dayOfWeek
}
```

**SQL:**
```sql
SELECT * FROM service_version_day 
WHERE service_version_id = ? 
ORDER BY day_of_week;
```

**Returns:** List of 7 ServiceVersionDayDto (guaranteed exactly 7)

**Exceptions:**
- `AccessDeniedException` - User doesn't own restaurant
- `ResourceNotFoundException` - ServiceVersion not found

---

### 2. getActiveTimeSlotsForDate()

**Purpose:** Get computed time slots for a specific date

```java
public List<TimeSlotDto> getActiveTimeSlotsForDate(
    Long serviceVersionId,
    LocalDate date,
    Long restaurantId,
    Long userId
) throws AccessDeniedException, ResourceNotFoundException {
    // 1. Validate access
    // 2. Load ServiceVersionSlotConfig
    // 3. Load ServiceVersionDay for date's day-of-week
    // 4. Check AvailabilityExceptions for that date
    // 5. Generate time slots based on config
    // 6. Filter by exception constraints
    // 7. Load reservation counts for each slot
    // 8. Return computed slots
}
```

**Algorithm:**
1. Get day of week from date
2. Load ServiceVersionDay for that day
3. If not active, return empty list
4. Check for CLOSURE exception on that date
   - If exists: return empty list
5. Check for REDUCED_HOURS exception on that date
   - If exists: use exception times instead of day times
6. Generate slots based on SlotConfig:
   - Start: operating_start_time
   - Duration: slot_duration_minutes
   - Buffer: buffer_time_minutes
   - End: operating_end_time
7. Query ReservationDAO for existing reservations
8. Compute availableCapacity for each slot
9. Return list of TimeSlotDto

**Returns:** List of TimeSlotDto (may be empty for closed days)

---

### 3. updateSlotConfiguration()

**Purpose:** Update or create slot generation template

```java
public ServiceVersionSlotConfigDto updateSlotConfiguration(
    Long serviceVersionId,
    ServiceVersionSlotConfigDto configDto,
    Long restaurantId,
    Long userId
) throws AccessDeniedException, ValidationException {
    // 1. Validate access
    // 2. Validate config (e.g., end_time > start_time)
    // 3. Check if config exists
    //    - If yes: update
    //    - If no: create new
    // 4. Save to database
    // 5. Trigger slot regeneration for future dates
    // 6. Return updated DTO
}
```

**Validation Rules:**
- slotDurationMinutes > 0
- bufferTimeMinutes >= 0
- dailyStartTime < dailyEndTime
- maxCapacityPerSlot > 0
- generationRule must be valid enum

**Side Effects:**
- Clear any cached time slots
- Mark future reservations for recalculation

**Returns:** Updated ServiceVersionSlotConfigDto

---

### 4. updateDaySchedule()

**Purpose:** Modify schedule for a specific day of week

```java
public ServiceVersionDayDto updateDaySchedule(
    Long serviceVersionId,
    DayOfWeek dayOfWeek,
    ServiceVersionDayDto dayDto,
    Long restaurantId,
    Long userId
) throws AccessDeniedException, ValidationException {
    // 1. Validate access
    // 2. Validate operating times
    // 3. Load existing ServiceVersionDay or create if missing
    // 4. Update fields:
    //    - is_active
    //    - operating_start_time
    //    - operating_end_time
    // 5. Update auditing fields
    // 6. Save to database
    // 7. Clear cache
    // 8. Return updated DTO
}
```

**Validation:**
- If is_active = true: operating_start_time must be <= operating_end_time
- Times should be reasonable (e.g., not 25:00)

**Returns:** Updated ServiceVersionDayDto

---

### 5. createAvailabilityException()

**Purpose:** Create closure, reduced hours, or special event

```java
public AvailabilityExceptionDto createAvailabilityException(
    Long serviceVersionId,
    AvailabilityExceptionDto exceptionDto,
    Long restaurantId,
    Long userId
) throws AccessDeniedException, ValidationException {
    // 1. Validate access
    // 2. Validate dates (start_date <= end_date)
    // 3. Validate times (if REDUCED_HOURS: times should be valid)
    // 4. Create new AvailabilityException entity
    // 5. Set auditing fields
    // 6. Save to database
    // 7. Cascade updates to any affected reservations
    // 8. Return created DTO
}
```

**Validation:**
- start_date <= end_date
- exception_type in (CLOSURE, REDUCED_HOURS, SPECIAL_EVENT)
- If REDUCED_HOURS: start_time and end_time must be set
- No overlapping exceptions of same type on same date

**Returns:** Created AvailabilityExceptionDto with generated ID

**Exception Types:**
- `CLOSURE`: No operations (all day)
- `REDUCED_HOURS`: Limited hours (with start_time/end_time)
- `SPECIAL_EVENT`: Special pricing/capacity (metadata in reason field)

---

### 6. deleteAvailabilityException()

**Purpose:** Remove an exception

```java
public void deleteAvailabilityException(
    Long exceptionId,
    Long restaurantId,
    Long userId
) throws AccessDeniedException, ResourceNotFoundException {
    // 1. Load exception
    // 2. Verify access (restaurant owns service version)
    // 3. Delete from database
    // 4. Cascade updates to reservations
    // 5. Clear cache
}
```

**Returns:** void

**Exceptions:**
- `ResourceNotFoundException` - Exception not found
- `AccessDeniedException` - User doesn't own restaurant

---

### 7. deactivateSchedule()

**Purpose:** Stop accepting reservations from a date

```java
public void deactivateSchedule(
    Long serviceVersionId,
    LocalDate fromDate,
    Long restaurantId,
    Long userId
) throws AccessDeniedException, ValidationException {
    // 1. Validate access
    // 2. Load ServiceVersion
    // 3. Update is_active = false
    // 4. Store deactivation_date = fromDate
    // 5. Update auditing fields
    // 6. Cascade: Cancel or warn affected reservations
    // 7. Clear cache
    // 8. Send notifications
}
```

**Cascade Behavior:**
- Reservations before fromDate: unchanged ✅
- Reservations on/after fromDate: trigger cancellation workflow
  - Send cancellation notice to customer
  - Offer rebooking to earlier available time
  - Log cancellation reason

**Returns:** void

---

### 8. reactivateSchedule()

**Purpose:** Resume accepting reservations

```java
public void reactivateSchedule(
    Long serviceVersionId,
    Long restaurantId,
    Long userId
) throws AccessDeniedException, ValidationException {
    // 1. Validate access
    // 2. Load ServiceVersion
    // 3. Update is_active = true
    // 4. Clear deactivation_date
    // 5. Update auditing fields
    // 6. Clear cache
    // 7. Send notifications (optional)
}
```

**Returns:** void

---

## Helper Methods (Private/Protected)

### validateServiceVersionOwnership()
```java
private void validateServiceVersionOwnership(
    Long serviceVersionId,
    Long restaurantId,
    Long userId
) throws AccessDeniedException, ResourceNotFoundException;
```

### generateTimeSlots()
```java
private List<TimeSlot> generateTimeSlots(
    LocalTime startTime,
    LocalTime endTime,
    int slotDurationMinutes,
    int bufferTimeMinutes
) returns List<TimeSlot>;
```

### checkAvailabilityException()
```java
private Optional<AvailabilityException> checkAvailabilityException(
    Long serviceVersionId,
    LocalDate date
) returns AvailabilityException or empty;
```

### loadReservationCounts()
```java
private Map<String, Integer> loadReservationCounts(
    Long serviceVersionId,
    LocalDate date
) returns Map of timeSlotId → bookingCount;
```

---

## DTO Requirements

### ServiceVersionDayDto
```java
public class ServiceVersionDayDto {
    private Long id;
    private Long serviceVersionId;
    private DayOfWeek dayOfWeek;
    private boolean active;
    private LocalTime operatingStartTime;
    private LocalTime operatingEndTime;
    private Instant createdAt;
    private String createdBy;
    private Instant modifiedAt;
    private String modifiedBy;
}
```

### ServiceVersionSlotConfigDto
```java
public class ServiceVersionSlotConfigDto {
    private Long id;
    private Long serviceVersionId;
    private Integer slotDurationMinutes;
    private Integer bufferTimeMinutes;
    private LocalTime dailyStartTime;
    private LocalTime dailyEndTime;
    private Integer maxCapacityPerSlot;
    private String generationRule;
    private Instant createdAt;
    private String createdBy;
    private Instant modifiedAt;
    private String modifiedBy;
}
```

### AvailabilityExceptionDto
```java
public class AvailabilityExceptionDto {
    private Long id;
    private Long serviceVersionId;
    private String exceptionType;  // CLOSURE, REDUCED_HOURS, SPECIAL_EVENT
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;   // nullable, used for REDUCED_HOURS
    private LocalTime endTime;     // nullable, used for REDUCED_HOURS
    private String reason;
    private Instant createdAt;
    private String createdBy;
    private Instant modifiedAt;
    private String modifiedBy;
}
```

### TimeSlotDto
```java
public class TimeSlotDto {
    private String id;  // Computed: "sv_{serviceVersionId}_slot_{number}"
    private Long serviceVersionId;
    private LocalDateTime slotStart;
    private LocalDateTime slotEnd;
    private Integer totalCapacity;
    private Integer availableCapacity;
    private Integer bookingCount;
    private boolean isAvailable;
    private String generatedFromConfigId;
}
```

---

## Database Queries

### Load weekly schedule
```sql
SELECT * FROM service_version_day 
WHERE service_version_id = ? 
ORDER BY day_of_week;
```

### Load slot config
```sql
SELECT * FROM service_version_slot_config 
WHERE service_version_id = ?;
```

### Check for exceptions
```sql
SELECT * FROM availability_exception 
WHERE service_version_id = ? 
  AND start_date <= ? 
  AND end_date >= ?;
```

### Count reservations in time window
```sql
SELECT COUNT(*) FROM reservation 
WHERE service_version_id = ? 
  AND reservation_date = ? 
  AND start_time >= ? 
  AND end_time <= ?;
```

### Load all active service versions
```sql
SELECT DISTINCT sv.* FROM service_version sv
JOIN service_version_day svd ON sv.id = svd.service_version_id
WHERE svd.is_active = true AND sv.is_active = true;
```

---

## Testing Strategy

### Unit Tests

1. **TimeSlot Generation**
   - Generate slots with various durations/buffers
   - Edge cases (midnight, day boundaries)

2. **Exception Handling**
   - CLOSURE day returns no slots
   - REDUCED_HOURS overrides base times
   - Multiple exceptions (priority handling)

3. **Validation**
   - Invalid dates rejected
   - Invalid times rejected
   - Overlapping exceptions handled

4. **Access Control**
   - User can't modify restaurant they don't own
   - Admin can modify any restaurant

### Integration Tests

1. **Full Workflow**
   - Create service version
   - Configure slots
   - Add exception
   - Query availability
   - Create reservation
   - Verify slot appears booked

2. **Deactivation Workflow**
   - Deactivate schedule
   - Verify no new reservations possible
   - Verify existing reservations survive
   - Reactivate
   - Verify new reservations possible

3. **Performance**
   - Generate 1 year of slots (365 days) < 1 second
   - Query availability with 1000 reservations < 100ms

---

## Implementation Sequence

### Week 1: Service Layer
1. Create `ServiceVersionScheduleService` interface
2. Implement basic methods (getters)
3. Implement slot generation algorithm
4. Write unit tests

### Week 2: Complex Operations
1. Implement exception handling
2. Implement deactivation/reactivation
3. Implement caching strategy
4. Write integration tests

### Week 3: Integration & Polish
1. Wire up to controller
2. Handle error scenarios gracefully
3. Performance optimization
4. Documentation

---

## Dependencies

**Required:**
- [ ] ServiceVersionDayDAO (existing? or create)
- [ ] ServiceVersionSlotConfigDAO (existing? or create)
- [ ] AvailabilityExceptionDAO (existing? or create)
- [ ] ReservationDAO (existing)
- [ ] ServiceVersionDAO (existing)
- [ ] ServiceDAO (existing)

**Optional:**
- [ ] Cache manager (for performance)
- [ ] Event system (for cascade notifications)

---

## Known Challenges

### 1. Slot Computation Performance
**Challenge:** Generating slots for large date ranges
**Solution:** Compute on-demand, cache results, lazy load

### 2. Reservation Conflicts
**Challenge:** Ensuring reservations don't exceed capacity
**Solution:** Atomic database transactions, unique constraints

### 3. Exception Priority
**Challenge:** Multiple exceptions on same date (CLOSURE + REDUCED_HOURS)
**Solution:** Define priority (CLOSURE > REDUCED_HOURS > normal)

### 4. Backward Compatibility
**Challenge:** Migrating existing Slot data to new model
**Solution:** Flyway migration script creates mapping

---

## Success Criteria

- [x] All 8 methods implemented and tested
- [x] All DTOs properly validated
- [x] Controller compiles without errors
- [x] Unit test coverage > 80%
- [x] Integration tests pass
- [x] Performance targets met (see Testing section)
- [x] No breaking changes to existing reservations
- [x] Supports all legacy use cases

---

## Next: Phase 3 Preparation

Once Phase 2 complete:
1. Enable new endpoints in Swagger
2. Update API documentation
3. Communicate to API clients
4. Begin monitoring deprecated endpoint usage
5. Plan cutover date (Phase 3)

---

## Questions Before Starting?

1. Are DAOs already created?
2. Should we use Spring Data JPA or custom queries?
3. Is there a cache manager preference (Redis, Caffeine)?
4. Should slot generation happen async or sync?
5. Should we notify customers of deactivations automatically?

Good luck with Phase 2! 🚀
