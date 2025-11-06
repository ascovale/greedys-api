# Temporal Slot Management System

## Overview

This enhanced slot management system solves the critical architectural problem where changing restaurant schedules breaks existing reservation integrity. The solution implements industry-standard temporal validity patterns used by major booking platforms like OpenTable and TheFork.

## Problem Solved

**Before**: When restaurants changed slot times, existing reservations became orphaned or invalid, causing data inconsistency and customer confusion.

**After**: Temporal slot versioning allows schedule changes while preserving reservation integrity through configurable migration policies.

## Core Components

### 1. Enhanced Slot Entity (`Slot.java`)

New attributes added to support temporal management:

```java
// Temporal validity
@Column(name = "valid_from")
private LocalDate validFrom;  // When this slot version becomes valid

@Column(name = "valid_to") 
private LocalDate validTo;    // When this slot version expires (NULL = indefinite)

@Column(name = "active", nullable = false)
private Boolean active = true; // Whether slot accepts new reservations

// Change management
@ManyToOne
@JoinColumn(name = "superseded_by")
private Slot supersededBy;    // Links to replacement slot

@Enumerated(EnumType.STRING)
@Column(name = "change_policy")
private SlotChangePolicy changePolicy = SlotChangePolicy.HARD_CUT;
```

### 2. Slot Change Policies (`SlotChangePolicy.java`)

Three business policies for handling existing reservations:

- **HARD_CUT**: Old and new slots coexist, no migration
- **NOTIFY_CUSTOMERS**: Send notifications, let customers choose  
- **AUTO_MIGRATE**: Automatically move compatible reservations

### 3. Temporal Data Access (`SlotDAO.java`)

Enhanced queries for date-based slot retrieval:

```java
// Get slots active on a specific date
List<Slot> findActiveSlotsByRestaurantAndDate(Long restaurantId, LocalDate date);

// Get slots expiring on a date (for cleanup/notifications)
List<Slot> findSlotsExpiringOnDate(LocalDate date);
```

### 4. Slot Transition Service (`SlotTransitionService.java`)

Business logic for managing slot schedule changes:

```java
// Main transition method
SlotDTO changeSlotSchedule(Long slotId, LocalTime newStart, LocalTime newEnd, 
                          LocalDate effectiveDate, SlotChangePolicy policy);

// Utility methods
boolean canSlotBeModified(Long slotId);
int getFutureReservationsCount(Long slotId);
void deactivateSlot(Long slotId, LocalDate fromDate);
void reactivateSlot(Long slotId);
```

### 5. REST API (`SlotTransitionController.java`)

RESTful endpoints for slot management:

```
POST /api/restaurant/slot-transitions/change-schedule
GET  /api/restaurant/slot-transitions/active-slots/service/{serviceId}
GET  /api/restaurant/slot-transitions/can-modify/{slotId}
POST /api/restaurant/slot-transitions/deactivate/{slotId}
POST /api/restaurant/slot-transitions/reactivate/{slotId}
```

## Usage Examples

### 1. Changing Slot Schedule

```json
POST /api/restaurant/slot-transitions/change-schedule
{
  "slotId": 123,
  "newStartTime": "19:00",
  "newEndTime": "21:00", 
  "effectiveDate": "2024-02-01",
  "changePolicy": "NOTIFY_CUSTOMERS"
}
```

### 2. Checking Slot Modification Safety

```json
GET /api/restaurant/slot-transitions/can-modify/123

Response:
{
  "canModify": false,
  "futureReservationsCount": 5,
  "message": "Slot has 5 future reservations"
}
```

### 3. Getting Active Slots

```json
GET /api/restaurant/slot-transitions/active-slots/service/456?date=2024-01-15

Response: [
  {
    "id": 123,
    "start": "18:00",
    "end": "20:00",
    "validFrom": "2024-01-01",
    "validTo": null,
    "active": true
  }
]
```

## Database Migration

The migration script `V1.1.0__add_slot_temporal_management.sql` adds:

- New temporal columns with backward compatibility
- Foreign key constraints and indexes
- Trigger for circular reference prevention  
- View for commonly-used active slot queries

## Change Policy Behaviors

### HARD_CUT
- Creates new slot with effective date
- Old slot remains valid until effective date
- Existing reservations stay with old slot
- New reservations use new slot from effective date

### NOTIFY_CUSTOMERS  
- Same as HARD_CUT but sends notifications
- Customers can choose to stay or move
- Requires notification system integration

### AUTO_MIGRATE
- Automatically moves compatible reservations
- Uses time overlap logic for compatibility
- Falls back to notification for incompatible reservations

## Industry Comparison

### OpenTable Approach
- Uses temporal validity with hard cutover dates
- Sends email notifications to affected customers
- Allows customers to modify or cancel through links

### TheFork Approach  
- Similar temporal model with automatic migration
- Proactive customer service for conflicts
- Maintains reservation history for analytics

### Our Implementation
- Combines best of both approaches
- Configurable policies per restaurant preference
- Maintains full audit trail

## Benefits

1. **Data Integrity**: Reservations never become orphaned
2. **Flexibility**: Multiple policies for different scenarios  
3. **Scalability**: Efficient temporal queries with indexes
4. **Audit Trail**: Full history of slot changes preserved
5. **Customer Experience**: Smooth transitions with notifications

## Future Enhancements

1. **Notification System**: Email/SMS integration for customer alerts
2. **Analytics Dashboard**: Reporting on slot change impacts
3. **Advanced Migration**: AI-powered reservation compatibility
4. **Bulk Operations**: Change multiple slots simultaneously
5. **Integration**: Calendar sync with external systems

## Testing

Run the comprehensive test suite:

```bash
mvn test -Dtest=SlotTransitionServiceTest
mvn test -Dtest=SlotTransitionControllerTest
```

## Monitoring

Key metrics to monitor:
- Slot change frequency by restaurant
- Reservation migration success rates
- Customer notification response rates
- Performance of temporal queries

## Support

For issues or questions about the slot management system:
1. Check this documentation
2. Review test cases for usage examples  
3. Contact the development team with specific scenarios