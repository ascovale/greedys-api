# Hybrid Service Scheduling Architecture - Complete Design

## Problem Statement

The old implementation used **two JSON fields** to store scheduling configuration:

1. **`openingHours`**: `{"monday": "09:00-23:00", "tuesday": "09:00-23:00", ...}`
2. **`slotGenerationParams`**: `{start_time, end_time, interval_minutes, buffer_minutes, max_concurrent}`

### Issues with JSON Approach

- ❌ **Not queryable** - Required JSON parsing in application code
- ❌ **Not type-safe** - No compile-time validation
- ❌ **No database constraints** - Invalid data could persist
- ❌ **Hard to index** - Queries were inefficient
- ❌ **Inflexible** - Couldn't express breaks, siesta, partial closures
- ❌ **No partial-day closures** - Could only close entire days

---

## New Architecture: Three Layers

### Layer 1: ServiceVersion (Container)
- **Validity Range**: `effectiveFrom` → `effectiveTo` (date range when this version applies)
- **Purpose**: Version management (e.g., "Summer Schedule 2025", "Winter Schedule 2025")
- **Database Table**: `service_version`

### Layer 2A: ServiceVersionDay (Weekly Schedule) ⭐
**REPLACES**: `openingHours` JSON

Represents recurring weekly schedule with **7 records per version** (one per day):

```
service_version_day:
- id: 1, service_version_id: 1, day_of_week: MONDAY (1)
  opening_time: 09:00, closing_time: 23:00, is_closed: FALSE
  max_reservations: 50, slot_duration: 30
  break_start: 14:00, break_end: 15:30 (siesta)

- id: 2, service_version_id: 1, day_of_week: TUESDAY (2)
  opening_time: 09:00, closing_time: 23:00, is_closed: FALSE
  ... (same pattern)

- id: 3, service_version_id: 1, day_of_week: SUNDAY (0)
  opening_time: NULL, closing_time: NULL, is_closed: TRUE
  (Closed on Sundays)
```

**Table Schema**:
```sql
CREATE TABLE service_version_day (
    id BIGINT PRIMARY KEY,
    service_version_id BIGINT (FK),
    day_of_week TINYINT (0=SUN, 1=MON, ..., 6=SAT),
    opening_time TIME,           -- e.g., 09:00
    closing_time TIME,           -- e.g., 23:00
    is_closed BOOLEAN,           -- Override: closed all day
    max_reservations INT,        -- Max covers per day
    slot_duration INT,           -- Duration in minutes
    break_start TIME,            -- e.g., 14:00 (siesta start)
    break_end TIME,              -- e.g., 15:30 (siesta end)
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE(service_version_id, day_of_week)
);
```

**Advantages**:
- ✅ **Queryable** - Direct columns, proper indexes
- ✅ **Type-safe** - DayOfWeek enum
- ✅ **Breaks support** - breakStart/breakEnd for siesta
- ✅ **Partial closure** - Can express "closed Sundays" with is_closed flag
- ✅ **Max capacity** - Per-day reservation limits
- ✅ **Efficient** - No JSON parsing

### Layer 2B: ServiceVersionSlotConfig (Slot Generation) ⭐
**REPLACES**: `slotGenerationParams` JSON

Defines how available time slots are generated:

```
service_version_slot_config:
- id: 1, service_version_id: 1
  start_time: 09:00         (when to START generating slots)
  end_time: 23:00           (when to STOP generating slots)
  slot_duration_minutes: 30 (each slot is 30 minutes)
  buffer_minutes: 0         (back-to-back slots)
  max_concurrent_reservations: 10 (10 covers per slot)
```

**Table Schema**:
```sql
CREATE TABLE service_version_slot_config (
    id BIGINT PRIMARY KEY,
    service_version_id BIGINT (FK),
    start_time TIME NOT NULL,                    -- e.g., 09:00
    end_time TIME NOT NULL,                      -- e.g., 23:00
    slot_duration_minutes INT NOT NULL,          -- e.g., 30
    buffer_minutes INT NOT NULL DEFAULT 0,       -- e.g., 5
    max_concurrent_reservations INT NOT NULL,    -- e.g., 10
    created_at DATETIME,
    updated_at DATETIME
);
```

**Slot Generation Algorithm**:
```
For a given date:
1. Get ServiceVersionDay for that date's day-of-week
2. Apply ServiceVersionSlotConfig to generate slots
3. Exclude breaks (breakStart → breakEnd)
4. Exclude closed times
5. Apply AvailabilityException overrides

Example: Monday, 09:00-23:00, 30min slots, 0min buffer
Generated slots: 09:00, 09:30, 10:00, 10:30, ..., 22:30

With buffer: 5 minutes between slots
Generated slots: 09:00-09:30 (slot) + 09:30-09:35 (buffer)
                 09:35-10:05 (slot) + 10:05-10:10 (buffer)
                 etc.

With max_concurrent=10:
- Each slot can accept up to 10 reservations simultaneously
- System tracks: (slot_time, count of reservations in that slot)
```

**Advantages**:
- ✅ **Queryable** - Direct columns, no JSON parsing
- ✅ **Type-safe** - LocalTime fields
- ✅ **Buffer support** - Gap between slots for payment/setup
- ✅ **Concurrent control** - Max per-slot bookings
- ✅ **Efficient** - No computation at query time
- ✅ **Flexible** - Can support multiple configs per version if needed (e.g., lunch vs dinner mode)

### Layer 3: AvailabilityException (Date-Specific Overrides) ⭐
**Enhanced**: Now supports partial-day closures

Represents date-specific exceptions to the weekly schedule:

```
availability_exception:
- id: 1, service_version_id: 1, exception_date: 2025-12-25 (Christmas)
  exception_type: CLOSURE, is_fully_closed: TRUE
  start_time: NULL, end_time: NULL
  (Fully closed - no reservations accepted)

- id: 2, service_version_id: 1, exception_date: 2025-02-14 (Valentine's)
  exception_type: SPECIAL_EVENT, is_fully_closed: FALSE
  start_time: 19:00, end_time: 23:00
  override_opening_time: 19:00, override_closing_time: 23:00
  (Only 19:00-23:00 available - special event)

- id: 3, service_version_id: 1, exception_date: 2025-03-01 (Maintenance)
  exception_type: MAINTENANCE, is_fully_closed: FALSE
  start_time: 12:00, end_time: 14:00
  (Maintenance window 12:00-14:00, but open before and after)
```

**Table Schema** (Enhanced):
```sql
CREATE TABLE availability_exception (
    id BIGINT PRIMARY KEY,
    service_version_id BIGINT (FK),
    exception_date DATE NOT NULL,
    exception_type ENUM ('CLOSURE', 'MAINTENANCE', 'SPECIAL_EVENT', 'STAFF_SHORTAGE', 'FULLY_BOOKED', 'CUSTOM'),
    
    -- Partial-day closure support:
    start_time TIME,                    -- NULL = whole day affected
    end_time TIME,                      -- NULL = whole day affected
    override_opening_time TIME,         -- Override opening hours for this date
    override_closing_time TIME,         -- Override closing hours for this date
    is_fully_closed BOOLEAN NOT NULL,   -- TRUE = entire day closed
    
    -- Other fields:
    notes VARCHAR(500),
    created_at DATETIME,
    updated_at DATETIME,
    
    INDEX(service_version_id, exception_date)
);
```

**Exception Types**:
- **CLOSURE**: Full-day or partial closure
- **MAINTENANCE**: Maintenance window (e.g., 12:00-14:00)
- **SPECIAL_EVENT**: Special operating hours (e.g., only 19:00-23:00)
- **STAFF_SHORTAGE**: Limited availability due to staffing
- **FULLY_BOOKED**: All slots are reserved
- **CUSTOM**: Custom exception

**Example Queries** (New DAOs):

```java
// Check if fully closed on a date
availabilityExceptionDAO.findFullDayClosureByDate(versionId, date)

// Get all exceptions for a specific date
availabilityExceptionDAO.findByServiceVersionAndDate(versionId, date)

// Get all partial-day maintenance windows
availabilityExceptionDAO.findMaintenanceWindows(versionId, startDate, endDate)

// Get all hours overrides
availabilityExceptionDAO.findHoursOverrides(versionId, startDate, endDate)

// Get closures in next 30 days
availabilityExceptionDAO.findFullDayClosuresByDateRange(versionId, today, today+30)
```

---

## Data Flow: Availability Check

**Query**: "What slots are available on March 15, 2025 at Italian Restaurant?"

```
1. Find ServiceVersion valid for 2025-03-15
   → Found: "Spring Menu 2025" (effective 2025-03-01 to 2025-06-30)

2. Get ServiceVersionDay for March 15 (Saturday = 6)
   → opening_time: 09:00, closing_time: 23:00, break_start: 14:00, break_end: 15:30
   → max_reservations: 50 covers

3. Generate slots using ServiceVersionSlotConfig
   → start_time: 09:00, end_time: 23:00, slot_duration: 30, buffer: 0, max_concurrent: 10
   → Slots: 09:00, 09:30, 10:00, ..., 22:30
   → Exclude break: 14:00-15:30 (siesta)
   → Result: [09:00, 09:30, ..., 13:30, 15:30, 16:00, ..., 22:30] (52 slots total)

4. Check AvailabilityException for March 15
   → No full-day closure found
   → Maintenance window: 12:00-14:00 found
   → Special event: 19:00-23:00 found
   
5. Apply exceptions
   → Remove slots in 12:00-14:00 range: [11:30, 12:00, 12:30, 13:00, 13:30]
   → Change 19:00-23:00 to special event (maybe mark as "premium pricing")
   
6. Filter by current reservations
   → Count reservations per slot
   → Remove slots with max_concurrent_reservations (10) already booked
   
7. Return final available slots to customer
   → [09:00, 09:30, 10:00, 10:30, 11:00, 11:30 (just before maintenance), 14:00, 14:30, ...]
   → Grouped by availability type: "Normal service", "Maintenance window (limited availability)", "Special event"
```

---

## Comparison: Old vs New

| Aspect | OLD (JSON) | NEW (JOINED) |
|--------|-----------|------------|
| **Opening Hours** | `{"monday": "09:00-23:00"}` JSON in 1 column | 7 ServiceVersionDay records, queryable columns |
| **Queryability** | Need JSON parsing in app | Direct SQL queries with indexes |
| **Type Safety** | String keys, no validation | Enums (DayOfWeek), compile-time checks |
| **Breaks/Siesta** | Not expressible | breakStart/breakEnd fields |
| **Slots Config** | JSON `{start_time, interval_minutes, ...}` | ServiceVersionSlotConfig entity |
| **Partial Closures** | Only full-day exceptions | Full-day + partial-day with start/end times |
| **Hours Overrides** | Not supported | override_opening_time/override_closing_time |
| **Performance** | Slow (JSON parsing per query) | Fast (indexed columns) |
| **Maintenance** | Error-prone manual JSON editing | UI forms with validation |
| **Data Integrity** | No constraints | Foreign keys, unique constraints |

---

## Database Schema Summary

### Tables Created/Modified

1. **service_version_day** (NEW)
   - Purpose: Weekly recurring schedules
   - Records per version: 7 (one per day)
   - Size: ~100 bytes per record

2. **service_version_slot_config** (NEW)
   - Purpose: Slot generation parameters
   - Records per version: Typically 1 (could be >1 for lunch/dinner modes)
   - Size: ~50 bytes per record

3. **availability_exception** (MODIFIED)
   - Added: start_time, end_time, override_opening_time, override_closing_time, is_fully_closed
   - Existing records: Preserved (is_fully_closed defaults to TRUE for backward compat)

4. **service_version** (MODIFIED)
   - Removed: slotGenerationParams JSON column (migrate data first!)
   - Removed: openingHours JSON column (migrate data first!)
   - Relationships: Now has OneToMany to serviceDays and slotConfigs

### Indexes Created

```sql
KEY idx_service_version_day_service_version_id (service_version_id)
KEY idx_service_version_day_dayofweek (day_of_week)
KEY idx_availability_exception_date_type (exception_date, exception_type)
KEY idx_availability_exception_service_version_date (service_version_id, exception_date)
```

---

## Java Entity Relationships

```
ServiceVersion (1)
├─ OneToMany → ServiceVersionDay (7) [One per day of week]
│  └─ Each has: dayOfWeek, openingTime, closingTime, isClosed, breaks, maxReservations
├─ OneToMany → ServiceVersionSlotConfig (1+) [Typically 1, could be >1]
│  └─ Each has: startTime, endTime, slotDurationMinutes, bufferMinutes, maxConcurrentReservations
├─ OneToMany → AvailabilityException (N) [Date-specific overrides]
│  └─ Each has: exceptionDate, startTime, endTime, overrideOpeningTime, overrideClosingTime, isFullyClosed
└─ OneToMany → Reservation (N) [Bookings using this version]
```

---

## Next Steps

1. **Execute V7 Migration**
   - Creates all three tables with proper constraints and indexes

2. **Create SchedulingService**
   - Business logic: `isAvailableOnDate()`, `getAvailableSlotsForDate()`, etc.
   - Orchestrates querying ServiceVersionDay + SlotConfig + AvailabilityException

3. **Create REST Endpoints** (Admin UI)
   - Manage ServiceVersionDay records
   - Manage ServiceVersionSlotConfig
   - Manage AvailabilityException

4. **Data Migration** (If existing service_version has JSON data)
   - Parse existing openingHours JSON → Create ServiceVersionDay records
   - Parse existing slotGenerationParams → Create ServiceVersionSlotConfig records
   - Preserve existing availability_exception records

5. **Update Reservation Service**
   - Use SchedulingService to check availability before creating reservations
   - Apply business rules: max concurrent per slot, max per day, exclude breaks/closures

6. **Testing**
   - Unit tests for helper methods (isOpen(), hasBreak(), isWithinOpeningHours())
   - Integration tests for availability checking scenarios
   - Performance tests for slot generation

---

## Future Extensibility

This architecture supports:

- **Multiple slot modes**: Different configs for lunch/dinner service
- **Dynamic pricing**: Tag slots by operating mode (normal/premium)
- **Staff scheduling**: Link breaks/unavailability to staff shifts
- **Capacity planning**: Track per-day vs per-slot limits
- **Reporting**: Efficient queries for occupancy rates, popular time slots

---

Generated: 2025-11-30
Architecture: Hybrid Service Scheduling (JOINED table per inheritance with queryable day/slot configuration)
Status: ✅ Complete - Ready for testing and migration
