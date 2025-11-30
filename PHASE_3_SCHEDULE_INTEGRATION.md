# Phase 3: Schedule Integration with Reservation System

**Status**: 🔄 In Progress  
**Timeline**: Complete within this session

---

## Overview

Phase 3 integrates the new ServiceVersionSchedule architecture with the existing reservation system. This involves:

1. **Reservation Validation Integration** - Using ServiceVersionScheduleService to validate reservations
2. **Conflict Detection** - Preventing double-booking
3. **API Versioning** - Managing old vs new endpoints during migration
4. **Deprecation Tracking** - Monitoring old API usage
5. **Full System Integration** - Wiring everything together

---

## Component 1: Reservation Validation Service

**Purpose**: Validate incoming reservation requests against available slots

**Location**: `com.application.restaurant.service.ReservationValidationService`

**Key Methods**:

```java
// Validate a specific date/time for a party
ValidationResult validateReservationDateTime(
    Long serviceVersionId,
    LocalDate reservationDate, 
    LocalTime reservationTime,
    Integer partySize)

// Find all available slots for a date with capacity for party size
List<TimeSlotDto> checkAvailableSlots(
    Long serviceVersionId,
    LocalDate reservationDate,
    Integer partySize)

// Check capacity for specific slot
CapacityCheckResult checkCapacity(
    Long serviceVersionId,
    LocalDate reservationDate,
    LocalTime startTime,
    LocalTime endTime,
    Integer partySize)

// Return alternatives when preferred time unavailable
List<AvailableTimeRange> returnAvailableTimeRanges(
    Long serviceVersionId,
    LocalDate preferredDate,
    LocalTime preferredTime,
    Integer partySize,
    Integer daysAhead)
```

**Data Classes**:

```java
// Result of validation check
public class ValidationResult {
    boolean valid;
    String message;
    
    public static ValidationResult valid()
    public static ValidationResult invalid(String message)
}

// Capacity check details
public class CapacityCheckResult {
    String slotId;
    Integer maxCapacity;
    Integer currentBookings;
    Integer availableCapacity;
    Integer requestedPartySize;
    boolean hasCapacity;
    Integer percentageOccupancy;
    String errorMessage;
}

// Alternative time range
public class AvailableTimeRange {
    LocalDate date;
    List<TimeSlotDto> slots;
    Integer numberOfSlots;
    LocalTime firstAvailableTime;
    LocalTime lastAvailableTime;
}
```

**Integration Points**:

- Called by ReservationController before creating reservation
- Uses ServiceVersionScheduleService for slot data
- Returns ValidationResult and CapacityCheckResult

---

## Component 2: Slot Conflict Detection Service

**Purpose**: Prevent double-booking and detect scheduling conflicts

**Location**: `com.application.restaurant.service.SlotConflictDetectionService`

**Key Methods**:

```java
// Find all reservations conflicting with requested slot
List<Reservation> findConflictingReservations(
    Long serviceVersionId,
    LocalDate reservationDate,
    LocalTime startTime,
    LocalTime endTime,
    Integer bufferMinutes)

// Check if conflict exists with capacity implications
ConflictCheckResult hasConflictWithExisting(
    Long serviceVersionId,
    LocalDate reservationDate,
    LocalTime startTime,
    LocalTime endTime,
    Integer bufferMinutes,
    Integer requestedPartySize)

// Suggest alternative slots when conflicts detected
List<AlternativeSlot> suggestAlternativeSlots(
    Long serviceVersionId,
    LocalDate preferredDate,
    LocalTime preferredTime,
    Integer partySize,
    Integer durationMinutes,
    Integer searchDaysAhead)

// Auto-adjust capacity when necessary
CapacityAdjustmentResult autoAdjustCapacity(
    Long serviceVersionId,
    LocalDate reservationDate,
    LocalTime startTime,
    LocalTime endTime,
    Integer requestedPartySize,
    Integer maxCapacityPerSlot)
```

**Data Classes**:

```java
// Conflict detection result
public class ConflictCheckResult {
    boolean hasConflict;
    Integer conflictingCount;
    Integer totalOccupancy;
    List<Reservation> conflictingReservations;
    Reservation firstConflict;
    String errorMessage;
}

// Alternative slot suggestion
public class AlternativeSlot {
    LocalDate date;
    LocalTime startTime;
    LocalTime endTime;
    Integer daysFromPreferred;
    Integer availableCapacity;
    String slotId;
}

// Capacity adjustment result
public class CapacityAdjustmentResult {
    boolean canAccommodate;
    boolean adjustmentNeeded;
    Integer recommendedCapacity;
    Integer currentOccupancy;
    Integer totalCapacity;
    String note;
    String errorMessage;
}
```

**Integration Points**:

- Called during reservation validation
- Returns suggestions for alternative times
- Manages capacity overflow scenarios

---

## Component 3: API Versioning Layer

**Purpose**: Manage old vs new endpoint versions during migration

**Location**: `com.application.restaurant.controller.api.ApiVersioningConfig`

**Strategy**:

```
Old API (v1/v2):         /restaurant/slot/*                [DEPRECATED]
New API (v3/v4):         /restaurant/schedule/*            [CURRENT]
Versioned Response:      Include API version in headers
Response Wrapper:        Consistent format across versions
```

**Implementation**:

```java
@Component
public class ApiVersionWrapper {
    public ResponseEntity<ApiResponse<T>> wrap(T data, String version, String message) {
        return ResponseEntity.ok(
            ApiResponse.builder()
                .data(data)
                .version(version)
                .message(message)
                .timestamp(Instant.now())
                .build()
        );
    }
}
```

---

## Component 4: Deprecation Warning Middleware

**Purpose**: Track and warn about deprecated API usage

**Location**: `com.application.restaurant.interceptor.DeprecationInterceptor`

**Annotations**:

```java
@Deprecated
@DeprecatedEndpoint(
    since = "2025-11-30",
    replacement = "ServiceVersionScheduleController.getWeeklySchedule()",
    removalDate = "2026-05-30"
)
public ResponseEntity<?> oldSlotEndpoint() { }
```

**Interceptor Logic**:

```java
public void preHandle(HttpServletRequest request) {
    if (request.getRequestURI().contains("/restaurant/slot/")) {
        log.warn("DEPRECATED API CALL: {} - Use new /restaurant/schedule/ endpoints instead", 
            request.getRequestURI());
        response.addHeader("X-API-Deprecated", "true");
        response.addHeader("X-Deprecation-Removal-Date", "2026-05-30");
    }
}
```

---

## Component 5: Reservation Service Integration

**Purpose**: Wire new validation into existing reservation flows

**Location**: `com.application.restaurant.service.ReservationService`

**Changes Required**:

### 1. Create Reservation Flow:

```java
public Reservation createReservation(ReservationRequest request) {
    // Step 1: Validate using new service
    ValidationResult validation = reservationValidationService.validateReservationDateTime(
        request.getServiceVersionId(),
        request.getReservationDate(),
        request.getReservationTime(),
        request.getPartySize()
    );
    
    if (!validation.isValid()) {
        throw new InvalidReservationException(validation.getMessage());
    }
    
    // Step 2: Check for conflicts
    ConflictCheckResult conflict = slotConflictDetectionService.hasConflictWithExisting(
        request.getServiceVersionId(),
        request.getReservationDate(),
        request.getReservationTime(),
        request.getReservationTime().plusMinutes(60),
        15,  // 15 min buffer
        request.getPartySize()
    );
    
    if (conflict.isHasConflict() && !conflict.isCanAccommodate()) {
        // Suggest alternatives
        List<AlternativeSlot> alternatives = slotConflictDetectionService.suggestAlternativeSlots(
            request.getServiceVersionId(),
            request.getReservationDate(),
            request.getReservationTime(),
            request.getPartySize(),
            60,
            7
        );
        throw new ConflictException(alternatives);
    }
    
    // Step 3: Create reservation (existing logic)
    Reservation reservation = new Reservation();
    reservation.setServiceVersion(serviceVersionService.getById(request.getServiceVersionId()));
    // ... set other fields
    
    return reservationDAO.save(reservation);
}
```

### 2. Cancel Reservation Flow:

```java
public void cancelReservation(Long reservationId) {
    Reservation reservation = reservationDAO.findById(reservationId)
        .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    
    // Update status
    reservation.setStatus(ReservationStatus.CANCELLED);
    reservation.setUpdatedAt(LocalDateTime.now());
    
    reservationDAO.save(reservation);
    
    // Log to audit trail
    log.info("Reservation cancelled: id={}, serviceVersion={}, date={}", 
        reservationId, 
        reservation.getServiceVersion().getId(),
        reservation.getReservationDate());
}
```

### 3. Modify Reservation Flow:

```java
public Reservation modifyReservation(Long reservationId, ReservationModifyRequest request) {
    Reservation existing = reservationDAO.findById(reservationId)
        .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    
    // If time changed, re-validate
    if (!existing.getReservationDate().equals(request.getNewDate()) ||
        !existing.getReservationTime().equals(request.getNewTime())) {
        
        ValidationResult validation = reservationValidationService.validateReservationDateTime(
            existing.getServiceVersion().getId(),
            request.getNewDate(),
            request.getNewTime(),
            request.getPartySize() != null ? request.getPartySize() : existing.getPartySize()
        );
        
        if (!validation.isValid()) {
            throw new InvalidReservationException("Cannot modify: " + validation.getMessage());
        }
    }
    
    // Update reservation
    existing.setReservationDate(request.getNewDate());
    existing.setReservationTime(request.getNewTime());
    if (request.getPartySize() != null) {
        existing.setPartySize(request.getPartySize());
    }
    existing.setUpdatedAt(LocalDateTime.now());
    
    return reservationDAO.save(existing);
}
```

---

## Backward Compatibility Strategy

### Phase 3A (Current):
- New endpoints available alongside old ones
- Both systems operational
- Monitoring enabled on old APIs

### Phase 3B (Month 2):
- Encourage migration via API deprecation headers
- Provide migration guides
- Log warnings for old API users

### Phase 3C (Month 3):
- Reduce old API availability
- Provide conversion tools
- Final migration support

### Phase 4 (Month 4):
- Remove old Slot-based endpoints
- Full transition to ServiceVersion schedule

---

## Testing Strategy

### 1. Unit Tests:

```java
@Test
public void testValidateReservationDateTime_Success() {
    ValidationResult result = service.validateReservationDateTime(
        serviceVersionId, date, time, 4);
    assertThat(result.isValid()).isTrue();
}

@Test
public void testValidateReservationDateTime_ClosedDay() {
    // Day marked as closed
    ValidationResult result = service.validateReservationDateTime(
        serviceVersionId, closedDate, time, 4);
    assertThat(result.isValid()).isFalse();
    assertThat(result.getMessage()).contains("not available");
}

@Test
public void testCheckAvailableSlots() {
    List<TimeSlotDto> slots = service.checkAvailableSlots(
        serviceVersionId, date, 4);
    assertThat(slots).isNotEmpty();
    slots.forEach(slot -> 
        assertThat(slot.getAvailableCapacity()).isGreaterThanOrEqualTo(4)
    );
}

@Test
public void testReturnAvailableTimeRanges() {
    List<AvailableTimeRange> ranges = service.returnAvailableTimeRanges(
        serviceVersionId, preferredDate, preferredTime, 4, 7);
    assertThat(ranges).isNotEmpty();
}
```

### 2. Integration Tests:

```java
@Test
public void testReservationCreationFlow() {
    // Create reservation using new service
    Reservation reservation = reservationService.createReservation(request);
    
    // Verify reservation exists
    assertThat(reservationDAO.findById(reservation.getId())).isPresent();
    
    // Verify slot capacity updated
    List<TimeSlotDto> updatedSlots = scheduleService.getActiveTimeSlotsForDate(
        reservation.getServiceVersion().getId(),
        reservation.getReservationDate(),
        restaurantId, ownerId);
    
    TimeSlotDto slot = updatedSlots.stream()
        .filter(s -> matches(s, reservation))
        .findFirst()
        .orElseThrow();
    
    assertThat(slot.getAvailableCapacity())
        .isLessThan(initialCapacity);
}

@Test
public void testConflictDetection() {
    // Create first reservation
    Reservation first = reservationService.createReservation(request1);
    
    // Try to create overlapping reservation
    assertThrows(ConflictException.class, () ->
        reservationService.createReservation(request2)
    );
}
```

### 3. Migration Tests:

```java
@Test
public void testOldAPIStillWorks() {
    // Old endpoint should still return data
    ResponseEntity<?> response = restTemplate.getForEntity(
        "/restaurant/slot/" + slotId, Object.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
}

@Test
public void testNewAPIReturnsEquivalentData() {
    // Old API response vs New API response should be equivalent
    Object oldData = oldController.getSlot(slotId);
    List<ServiceVersionDayDto> newData = newController.getWeeklySchedule(
        serviceVersionId, restaurantId, userId);
    
    assertThat(convertToComparable(oldData))
        .isEqualTo(convertToComparable(newData));
}
```

---

## Migration Checklist

- [ ] Phase 3 Component 1: ReservationValidationService (core logic)
- [ ] Phase 3 Component 2: SlotConflictDetectionService (conflict prevention)
- [ ] Phase 3 Component 3: API Versioning Layer (response wrapper)
- [ ] Phase 3 Component 4: Deprecation Interceptor (tracking)
- [ ] Phase 3 Component 5: ReservationService Integration (wire everything)
- [ ] Unit tests for all new services (85%+ coverage)
- [ ] Integration tests with existing systems
- [ ] Migration guide documentation
- [ ] Monitoring setup for old API usage
- [ ] Load testing new endpoints
- [ ] Production deployment (staged rollout)
- [ ] Customer communication about changes

---

## Success Metrics

- ✅ 100% of validation logic uses ServiceVersionScheduleService
- ✅ 0 double-booking incidents after Phase 3
- ✅ <5ms response time for validation checks
- ✅ 85%+ unit test coverage
- ✅ Zero breaking changes for existing customers
- ✅ Old API monitoring shows <10% usage by end of Phase 3
- ✅ All integration tests passing

---

## Next Steps

1. **Implement ReservationValidationService** (4 methods, 3 result classes)
2. **Implement SlotConflictDetectionService** (4 methods, 3 result classes)
3. **Create ApiVersioningConfig** (response wrapper)
4. **Add DeprecationInterceptor** (tracking)
5. **Wire into ReservationService** (integration)
6. **Write comprehensive tests** (unit + integration)
7. **Deploy and monitor** (staged rollout)
