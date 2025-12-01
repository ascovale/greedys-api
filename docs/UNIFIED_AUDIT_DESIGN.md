# Unified Audit Layer Design

## 1. Goal

- **Separate audit table per entity** — ReservationAudit, SlotConfigurationAudit, ServiceAudit, ClosingDayAudit
- **Main tables store only current state** — no history columns
- **Audit records store old values only** — new value readable from main entity
- **Common base class** — shared audit fields via `@MappedSuperclass`, no duplication
- **Traceable changes** — who changed, when, what was the previous value

---

## 2. Package Structure

```
com.application.common
├── persistence
│   ├── model
│   │   └── audit
│   │       ├── BaseAuditLog.java              # @MappedSuperclass with common fields
│   │       ├── ReservationAuditLog.java       # extends BaseAuditLog
│   │       ├── SlotConfigurationAuditLog.java # extends BaseAuditLog
│   │       ├── ServiceAuditLog.java           # extends BaseAuditLog
│   │       └── ClosingDayAuditLog.java        # extends BaseAuditLog
│   └── dao
│       └── audit
│           ├── ReservationAuditLogDAO.java
│           ├── SlotConfigurationAuditLogDAO.java
│           ├── ServiceAuditLogDAO.java
│           └── ClosingDayAuditLogDAO.java
└── service
    └── audit
        └── AuditService.java                  # Generic service, delegates to specific DAOs
```

---

## 3. Conceptual Changes by Entity

### Reservation
- **Audit table**: `reservation_audit_log`
- **Hook location**: `ReservationService.updateReservation()`, `deleteReservation()`
- **Before update**: load current entity, pass to AuditService
- **Audit data**: `reservationId`, `userId`, `action`, `oldValue` (JSON)

### SlotConfiguration (ServiceVersionSlotConfig)
- **Audit table**: `slot_configuration_audit_log`
- **Hook location**: `ServiceVersionScheduleService.updateSlotConfiguration()`
- **Before update**: load current config, pass to AuditService
- **Audit data**: `slotConfigId`, `userId`, `action`, `oldValue`

### Service
- **Audit table**: `service_audit_log`
- **Hook location**: `ServiceService.updateService()`, `deleteService()`
- **Before update**: load current service, pass to AuditService
- **Audit data**: `serviceId`, `userId`, `action`, `oldValue`

### ClosingDayException (AvailabilityException)
- **Audit table**: `closing_day_audit_log`
- **Hook location**: `ServiceVersionScheduleService.createAvailabilityException()`, `deleteAvailabilityException()`
- **Before delete**: load current exception, pass to AuditService
- **Audit data**: `exceptionId`, `userId`, `action`, `oldValue`

---

## 4. Base Audit Record Structure (@MappedSuperclass)

| Field | Description |
|-------|-------------|
| `id` | Auto-generated PK |
| `entityId` | ID of the modified entity (FK to main table) |
| `action` | Enum: CREATE, UPDATE, DELETE |
| `userId` | Who made the change |
| `userType` | CUSTOMER, RESTAURANT_USER, ADMIN (optional) |
| `oldValue` | JSON string of previous state (null for CREATE) |
| `timestamp` | When the change occurred |

Each entity-specific audit class extends this and adds its own `@Table` annotation.

---

## 5. Flow Diagram (UPDATE operation)

```
  Controller          Service            AuditService       EntityAuditDAO      DB
      │                  │                    │                  │               │
      │── updateReq ────>│                    │                  │               │
      │                  │                    │                  │               │
      │                  │── findById ────────────────────────────────────────-->│
      │                  │<─ currentEntity ──────────────────────────────────────│
      │                  │                    │                  │               │
      │                  │── logChange ──────>│                  │               │
      │                  │   (entity, userId, │                  │               │
      │                  │    action, old)    │                  │               │
      │                  │                    │── save ─────────>│               │
      │                  │                    │  (to specific    │── INSERT ────>│
      │                  │                    │   audit table)   │  entity_audit │
      │                  │                    │<─ ok ────────────│               │
      │                  │<─ ok ──────────────│                  │               │
      │                  │                    │                  │               │
      │                  │── save(updated) ──────────────────────────────────────>│
      │                  │<─ savedEntity ────────────────────────────────────────│
      │<── response ─────│                    │                  │               │
      │                  │                    │                  │               │
```

---

## 6. Key Principles

- **One audit table per entity** — `reservation_audit_log`, `service_audit_log`, etc.
- **Shared base class** — `BaseAuditLog` with common fields (DRY)
- **Audit BEFORE modify** — capture old state before save/delete
- **JSON for oldValue** — flexible, stores only changed fields
- **No cascade** — audit records are independent, never deleted with main entity
