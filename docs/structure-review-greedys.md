# Greedys API - Structure Review and Refactoring Plan

**Generated:** November 30, 2025  
**Repository:** greedys-api  
**Branch:** main  
**Total Java Files:** 594

---

## 1. Goal

This document provides a comprehensive analysis and refactoring plan for the Greedys API Spring Boot monolith with the following objectives:

1. **Reorganize Controllers by User Context**
   - ADMIN
   - CUSTOMER  
   - RESTAURANT_USER (staff for a specific restaurant)
   - RESTAURANT_USER_HUB (user managing multiple restaurants)
   - AGENCY_USER (user working inside a specific agency)
   - AGENCY_USER_HUB (user working across multiple agencies)

2. **Centralize Service Layer Logic** - Minimize duplication by consolidating shared domain/application services

3. **Clean up `common` Package** - Ensure only truly shared, generic utilities remain

4. **Clarify `persistence` Organization** - Clear separation of DAO/repository and JPA model layers by bounded context

5. **Identify and Remove Legacy Code** - Mark deprecated code for removal

---

## 2. Current High-Level Structure

### 2.1 Project Layout

```
greedys_api/
├── src/
│   ├── main/
│   │   ├── java/com/application/
│   │   │   ├── admin/           (57 files)
│   │   │   ├── agency/          (30 files)  
│   │   │   ├── common/          (266 files) ⚠️ BLOATED
│   │   │   ├── customer/        (58 files)
│   │   │   ├── restaurant/      (182 files)
│   │   │   └── Application.java
│   │   └── resources/
│   └── test/
└── pom.xml
```

### 2.2 Current Module Breakdown

| Module | Files | Controllers | Services | Persistence | DTOs |
|--------|-------|-------------|----------|-------------|------|
| admin | 57 | 17 | 9 | 15 | 16 |
| agency | 30 | 4 | 4 | 9 | 5 |
| common | 266 | 7 | 25 | 70+ | 55+ |
| customer | 58 | 12 | 11 | 14 | 8 |
| restaurant | 182 | 25+ | 28 | 53 | 55+ |

### 2.3 Observed Issues

1. **`common` package is bloated** - Contains 266 files including:
   - Reservation logic (`common/reservation/`)
   - Notification logic (`common/notification/`)
   - Restaurant-specific DTOs (`common/web/dto/restaurant/`)
   - Customer-specific DTOs (`common/web/dto/customer/`)
   - Spring configuration (`common/spring/`)
   - Security components (`common/security/`)

2. **Inconsistent Controller Organization**
   - `admin/controller/restaurant/` - Admin controllers for restaurant management
   - `restaurant/controller/restaurant/` - Restaurant user controllers
   - `customer/controller/restaurant/` - Customer viewing restaurant info
   - Mixed user-context responsibilities in single packages

3. **Hub Users Not Clearly Separated**
   - `RUserHub` exists but no dedicated `/restaurant-hub/**` API path
   - `AgencyUserHub` exists but no dedicated `/agency-hub/**` API path
   - Hub validation filters exist but endpoints mixed with regular users

4. **Deprecated Code Present**
   - `SlotTransitionController.java` - marked for removal in v3.0
   - `RestaurantSlotController.java` - deprecated slot-based APIs
   - `CustomerSlotController.java` - deprecated customer slot APIs

5. **Persistence Scattered**
   - Each module has its own `persistence/dao` and `persistence/model`
   - `common/persistence/model/reservation/` - Core reservation models in common
   - `common/persistence/model/notification/` - Notification models in common
   - Duplicate `ReservationAuditDAO` in both `common` and `customer` packages

---

## 3. Target Architectural Guidelines

### 3.1 User Contexts (Classification Schema)

| Context | Description | API Prefix |
|---------|-------------|------------|
| `ADMIN` | Platform administrators | `/admin/**` |
| `CUSTOMER` | End-user customers | `/customer/**` |
| `RESTAURANT_USER` | Restaurant staff (single restaurant) | `/restaurant/**` |
| `RESTAURANT_USER_HUB` | Multi-restaurant manager | `/restaurant-hub/**` (NEW) |
| `AGENCY_USER` | Agency staff (single agency) | `/agency/**` |
| `AGENCY_USER_HUB` | Multi-agency manager | `/agency-hub/**` (NEW) |
| `PUBLIC` | Unauthenticated endpoints | Various |
| `SHARED` | Truly context-agnostic code | Internal only |

### 3.2 Layer Classification

| Layer | Package Pattern | Purpose |
|-------|-----------------|---------|
| `api` | `.../api/{context}/` | Controllers, request/response DTOs |
| `application` | `.../application/` or `.../service/` | Application services, use cases |
| `domain` | `.../domain/` | Entities, aggregates, domain events, domain services |
| `persistence` | `.../persistence/` | DAO/Repository, JPA models |
| `infrastructure` | `.../infrastructure/` | External adapters, config, security |
| `common` | `.../common/` | Shared utilities, exceptions, cross-cutting |

### 3.3 Proposed Target Package Structure

```
com.application/
├── api/
│   ├── admin/                    # Admin controllers
│   ├── customer/                 # Customer controllers  
│   ├── restaurant/               # Restaurant user controllers
│   ├── restaurant_hub/           # Restaurant hub controllers (NEW)
│   ├── agency/                   # Agency user controllers
│   ├── agency_hub/               # Agency hub controllers (NEW)
│   └── public/                   # Public endpoints (booking widget, etc.)
│
├── core/                         # Shared business logic
│   ├── domain/
│   │   ├── reservation/          # Reservation aggregate
│   │   ├── restaurant/           # Restaurant aggregate
│   │   ├── customer/             # Customer aggregate
│   │   ├── agency/               # Agency aggregate
│   │   └── event/                # Domain events
│   └── service/
│       ├── reservation/          # ReservationService (shared)
│       ├── restaurant/           # RestaurantService (shared)
│       ├── notification/         # Notification orchestration
│       └── authentication/       # Auth services
│
├── persistence/
│   ├── model/
│   │   ├── reservation/          # Reservation, Service, ServiceVersion, Slot
│   │   ├── restaurant/           # Restaurant, Room, Table, Menu
│   │   ├── customer/             # Customer, Allergy
│   │   ├── admin/                # Admin, AdminRole
│   │   ├── agency/               # Agency, AgencyUser
│   │   └── notification/         # Notification entities
│   └── dao/
│       ├── reservation/
│       ├── restaurant/
│       ├── customer/
│       ├── admin/
│       ├── agency/
│       └── notification/
│
├── infrastructure/
│   ├── security/                 # JWT, filters, providers
│   ├── config/                   # Spring configuration
│   ├── external/                 # External service adapters
│   │   ├── google/               # Google Places, verification
│   │   ├── twilio/               # SMS verification
│   │   ├── firebase/             # Push notifications
│   │   └── rabbitmq/             # Message queue
│   └── web/                      # Global exception handler, CORS
│
├── common/                       # Only truly shared utilities
│   ├── util/
│   ├── exception/
│   ├── dto/                      # Cross-context DTOs only
│   └── mapper/                   # MapStruct mappers
│
└── Application.java
```

---

## 4. File Inventory and Decisions

### 4.1 Controllers (API Layer)

#### 4.1.1 Admin Controllers (57 files total in admin module)

| File | Current Path | Status | Proposed Path | Context | Reason |
|------|--------------|--------|---------------|---------|--------|
| AdminAuthenticationController.java | admin/controller/admin/ | MOVE | api/admin/auth/ | ADMIN | Group auth controllers |
| AdminController.java | admin/controller/admin/ | MOVE | api/admin/user/ | ADMIN | Admin user management |
| AdminRegistrationController.java | admin/controller/admin/ | MOVE | api/admin/auth/ | ADMIN | Auth-related |
| AdminAllergyController.java | admin/controller/ | MOVE | api/admin/allergy/ | ADMIN | Admin managing allergies |
| AdminCustomerController.java | admin/controller/ | MOVE | api/admin/customer/ | ADMIN | Admin managing customers |
| AdminReservationController.java | admin/controller/ | MOVE | api/admin/reservation/ | ADMIN | Admin managing reservations |
| AdminRestaurantVerificationController.java | admin/controller/ | MOVE | api/admin/verification/ | ADMIN | Restaurant verification |
| AdminServicesController.java | admin/controller/ | MOVE | api/admin/service/ | ADMIN | Service types management |
| TwilioRestaurantVerificationController.java | admin/controller/ | MOVE | api/admin/verification/ | ADMIN | Verification via Twilio |
| AdminCategoryController.java | admin/controller/restaurant/ | MOVE | api/admin/restaurant/ | ADMIN | Restaurant categories |
| AdminRestaurantManagementController.java | admin/controller/restaurant/ | MOVE | api/admin/restaurant/ | ADMIN | Restaurant CRUD |
| AdminRestaurantReservationController.java | admin/controller/restaurant/ | MOVE | api/admin/reservation/ | ADMIN | Reservations via admin |
| AdminRoomTableController.java | admin/controller/restaurant/ | MOVE | api/admin/restaurant/ | ADMIN | Room/table management |
| AdminRUserController.java | admin/controller/restaurant/ | MOVE | api/admin/restaurant/user/ | ADMIN | Restaurant user mgmt |
| AdminUserController.java | admin/controller/restaurant/ | MOVE | api/admin/restaurant/user/ | ADMIN | Generic user mgmt |
| AdminTestEmailController.java | admin/controller/test/ | LEGACY_CANDIDATE | - | ADMIN | Test controller, remove from prod |
| AdminTestWhatsAppController.java | admin/controller/test/ | LEGACY_CANDIDATE | - | ADMIN | Test controller, remove from prod |

#### 4.1.2 Customer Controllers

| File | Current Path | Status | Proposed Path | Context | Reason |
|------|--------------|--------|---------------|---------|--------|
| CustomerAuthenticationController.java | customer/controller/customer/ | MOVE | api/customer/auth/ | CUSTOMER | Auth controllers |
| CustomerController.java | customer/controller/customer/ | MOVE | api/customer/profile/ | CUSTOMER | Customer profile |
| CustomerRegistrationController.java | customer/controller/customer/ | MOVE | api/customer/auth/ | CUSTOMER | Registration |
| CustomerAllergyController.java | customer/controller/customer/ | MOVE | api/customer/allergy/ | CUSTOMER | Customer allergies |
| CustomerReservationController.java | customer/controller/ | MOVE | api/customer/reservation/ | CUSTOMER | Customer reservations |
| CustomerNotificationController.java | customer/controller/ | MOVE | api/customer/notification/ | CUSTOMER | Customer notifications |
| CustomerMatchController.java | customer/controller/matching/ | MOVE | api/customer/matching/ | CUSTOMER | Customer matching |
| CustomerRestaurantInfoController.java | customer/controller/restaurant/ | MOVE | api/customer/restaurant/ | CUSTOMER | View restaurant info |
| CustomerMenuController.java | customer/controller/restaurant/ | MOVE | api/customer/restaurant/ | CUSTOMER | View menus |
| CustomerRoomController.java | customer/controller/restaurant/ | MOVE | api/customer/restaurant/ | CUSTOMER | View rooms |
| CustomerTableController.java | customer/controller/restaurant/ | MOVE | api/customer/restaurant/ | CUSTOMER | View tables |
| CustomerRestaurantServiceController.java | customer/controller/restaurant/ | MOVE | api/customer/restaurant/ | CUSTOMER | View services |
| CustomerSlotController.java | customer/controller/restaurant/ | LEGACY_CANDIDATE | - | CUSTOMER | DEPRECATED - remove in v3.0 |

#### 4.1.3 Restaurant User Controllers

| File | Current Path | Status | Proposed Path | Context | Reason |
|------|--------------|--------|---------------|---------|--------|
| RestaurantAuthenticationController.java | restaurant/controller/ | MOVE | api/restaurant/auth/ | RESTAURANT_USER | Auth |
| RestaurantRegistrationController.java | restaurant/controller/ | MOVE | api/restaurant/auth/ | RESTAURANT_USER | Registration |
| RestaurantReservationController.java | restaurant/controller/ | MOVE | api/restaurant/reservation/ | RESTAURANT_USER | Reservations |
| RestaurantNotificationController.java | restaurant/controller/ | MOVE | api/restaurant/notification/ | RESTAURANT_USER | Notifications |
| RestaurantCustomerController.java | restaurant/controller/ | MOVE | api/restaurant/customer/ | RESTAURANT_USER | Customer mgmt |
| RestaurantCustomerContactController.java | restaurant/controller/ | MOVE | api/restaurant/customer/ | RESTAURANT_USER | Customer contacts |
| BookingFormController.java | restaurant/controller/ | MOVE | api/public/booking/ | PUBLIC | Booking widget |
| RestaurantInfoController.java | restaurant/controller/restaurant/ | MOVE | api/restaurant/info/ | RESTAURANT_USER | Restaurant info |
| RestaurantMenuController.java | restaurant/controller/restaurant/ | MOVE | api/restaurant/menu/ | RESTAURANT_USER | Menu mgmt |
| RestaurantRoomController.java | restaurant/controller/restaurant/ | MOVE | api/restaurant/room/ | RESTAURANT_USER | Room mgmt |
| RestaurantTableController.java | restaurant/controller/restaurant/ | MOVE | api/restaurant/table/ | RESTAURANT_USER | Table mgmt |
| RestaurantServicesController.java | restaurant/controller/restaurant/ | MOVE | api/restaurant/service/ | RESTAURANT_USER | Service mgmt |
| RestaurantSlotController.java | restaurant/controller/restaurant/ | LEGACY_CANDIDATE | - | RESTAURANT_USER | DEPRECATED |
| RestaurantSlotManagementController.java | restaurant/controller/restaurant/ | MOVE | api/restaurant/schedule/ | RESTAURANT_USER | Schedule mgmt |
| ServiceVersionScheduleController.java | restaurant/controller/restaurant/ | MOVE | api/restaurant/schedule/ | RESTAURANT_USER | New schedule API |
| RUserAuthController.java | restaurant/controller/rUser/ | MOVE | api/restaurant/user/auth/ | RESTAURANT_USER | Staff auth |
| RUserController.java | restaurant/controller/rUser/ | MOVE | api/restaurant/user/ | RESTAURANT_USER | Staff mgmt |
| RUserFcmController.java | restaurant/controller/rUser/ | MOVE | api/restaurant/user/ | RESTAURANT_USER | FCM tokens |
| RestaurantCustomerAgendaController.java | restaurant/controller/agenda/ | MOVE | api/restaurant/agenda/ | RESTAURANT_USER | Customer agenda |
| GoogleBusinessVerificationController.java | restaurant/controller/google/ | MOVE | api/restaurant/google/ | RESTAURANT_USER | Google verification |
| GoogleReserveController.java | restaurant/controller/google/ | MOVE | api/restaurant/google/ | RESTAURANT_USER | Reserve with Google |
| SlotTransitionController.java | restaurant/web/ | LEGACY_CANDIDATE | - | RESTAURANT_USER | DEPRECATED - remove v3.0 |

#### 4.1.4 Agency Controllers

| File | Current Path | Status | Proposed Path | Context | Reason |
|------|--------------|--------|---------------|---------|--------|
| AgencyAuthenticationController.java | agency/controller/ | MOVE | api/agency/auth/ | AGENCY_USER | Auth |
| AgencyController.java | agency/controller/ | MOVE | api/agency/management/ | AGENCY_USER | Agency mgmt |
| AgencyRegistrationController.java | agency/controller/ | MOVE | api/agency/auth/ | AGENCY_USER | Registration |
| AgencyReservationController.java | agency/controller/ | MOVE | api/agency/reservation/ | AGENCY_USER | Reservations |

#### 4.1.5 Common Controllers

| File | Current Path | Status | Proposed Path | Context | Reason |
|------|--------------|--------|---------------|---------|--------|
| BaseController.java | common/controller/ | KEEP_AS_IS | infrastructure/web/ | SHARED | Base controller utilities |
| CustomErrorController.java | common/controller/ | KEEP_AS_IS | infrastructure/web/ | SHARED | Error handling |
| ImageController.java | common/controller/ | MOVE | api/shared/ | SHARED | Image handling |
| SwaggerGroupsController.java | common/controller/ | MOVE | infrastructure/swagger/ | SHARED | Swagger config |
| NotificationReadController.java | common/notification/controller/ | MOVE | api/shared/notification/ | SHARED | Cross-context notifications |
| BatchReservationController.java | common/reservation/controller/parsing/ | MOVE | api/shared/reservation/ | SHARED | Batch operations |
| ReservationParsingController.java | common/reservation/controller/parsing/ | MOVE | api/shared/reservation/ | SHARED | Parsing operations |

---

### 4.2 Services (Application Layer)

#### 4.2.1 Core/Shared Services (Keep in centralized location)

| File | Current Path | Status | Proposed Path | Context | Reason |
|------|--------------|--------|---------------|---------|--------|
| ReservationService.java | common/service/reservation/ | KEEP | core/service/reservation/ | SHARED | Core reservation logic |
| ReservationAuditService.java | common/service/reservation/ | KEEP | core/service/reservation/ | SHARED | Audit for reservations |
| RestaurantService.java | common/service/ | KEEP | core/service/restaurant/ | SHARED | Core restaurant logic |
| RestaurantCategoryService.java | common/service/ | KEEP | core/service/restaurant/ | SHARED | Category management |
| AllergyService.java | common/service/ | KEEP | core/service/allergy/ | SHARED | Allergy management |
| EmailService.java | common/service/ | MOVE | infrastructure/external/email/ | SHARED | External integration |
| WhatsAppService.java | common/service/ | MOVE | infrastructure/external/twilio/ | SHARED | External integration |
| FirebaseService.java | common/service/ | MOVE | infrastructure/external/firebase/ | SHARED | External integration |
| ReliableNotificationService.java | common/service/ | MOVE | infrastructure/external/notification/ | SHARED | Notification reliability |
| GoogleAuthService.java | common/service/authentication/ | MOVE | infrastructure/external/google/ | SHARED | Google auth |

#### 4.2.2 Context-Specific Services

| File | Current Path | Status | Proposed Path | Context | Reason |
|------|--------------|--------|---------------|---------|--------|
| AdminService.java | admin/service/ | KEEP | api/admin/service/ | ADMIN | Admin-specific |
| AdminReservationService.java | admin/service/ | KEEP | api/admin/service/ | ADMIN | Admin reservations |
| AdminCustomerService.java | admin/service/ | KEEP | api/admin/service/ | ADMIN | Admin customers |
| CustomerService.java | customer/service/ | KEEP | api/customer/service/ | CUSTOMER | Customer-specific |
| CustomerReservationService.java | customer/service/reservation/ | EVALUATE | core/service/reservation/ | SHARED | Merge with core? |
| CustomerMatchService.java | customer/service/ | KEEP | api/customer/service/ | CUSTOMER | Matching logic |
| RUserService.java | restaurant/service/ | KEEP | api/restaurant/service/ | RESTAURANT_USER | Restaurant user mgmt |
| RestaurantAgendaService.java | restaurant/service/agenda/ | KEEP | api/restaurant/service/ | RESTAURANT_USER | Agenda logic |
| RestaurantMenuService.java | restaurant/service/ | KEEP | api/restaurant/service/ | RESTAURANT_USER | Menu logic |
| SlotService.java | restaurant/service/ | LEGACY_CANDIDATE | - | RESTAURANT_USER | DEPRECATED |
| SlotTransitionService.java | restaurant/service/ | LEGACY_CANDIDATE | - | RESTAURANT_USER | DEPRECATED |
| ServiceVersionService.java | restaurant/service/ | KEEP | core/service/schedule/ | SHARED | New scheduling |
| ServiceVersionScheduleService.java | restaurant/service/ | KEEP | core/service/schedule/ | SHARED | New scheduling |
| AgencyService.java | agency/service/ | KEEP | api/agency/service/ | AGENCY_USER | Agency mgmt |
| AgencyUserService.java | agency/service/ | KEEP | api/agency/service/ | AGENCY_USER | Agency user mgmt |

#### 4.2.3 Notification Services

| File | Current Path | Status | Proposed Path | Context | Reason |
|------|--------------|--------|---------------|---------|--------|
| SharedReadService.java | common/service/notification/ | KEEP | core/service/notification/ | SHARED | Notification reading |
| RestaurantNotificationService.java | restaurant/service/ | KEEP | api/restaurant/service/ | RESTAURANT_USER | Restaurant notifications |
| CustomerNotificationService.java | customer/service/notification/ | KEEP | api/customer/service/ | CUSTOMER | Customer notifications |
| NotificationOrchestrator.java | common/service/notification/orchestrator/ | KEEP | core/service/notification/ | SHARED | Orchestration |
| EventOutboxOrchestrator.java | common/service/notification/orchestrator/ | KEEP | core/service/notification/ | SHARED | Event outbox |

---

### 4.3 Persistence Layer

#### 4.3.1 Core Persistence Models (common/persistence/model/)

| File | Current Path | Status | Proposed Path | Context | Reason |
|------|--------------|--------|---------------|---------|--------|
| Reservation.java | common/persistence/model/reservation/ | KEEP | persistence/model/reservation/ | SHARED | Core entity |
| ReservationAudit.java | common/persistence/model/reservation/ | KEEP | persistence/model/reservation/ | SHARED | Audit entity |
| ReservationRequest.java | common/persistence/model/reservation/ | KEEP | persistence/model/reservation/ | SHARED | Request entity |
| ReservationModificationRequest.java | common/persistence/model/reservation/ | KEEP | persistence/model/reservation/ | SHARED | Modification entity |
| Service.java | common/persistence/model/reservation/ | KEEP | persistence/model/schedule/ | SHARED | Service entity |
| ServiceVersion.java | common/persistence/model/reservation/ | KEEP | persistence/model/schedule/ | SHARED | Version entity |
| ServiceVersionDay.java | common/persistence/model/reservation/ | KEEP | persistence/model/schedule/ | SHARED | Day config |
| ServiceVersionSlotConfig.java | common/persistence/model/reservation/ | KEEP | persistence/model/schedule/ | SHARED | Slot config |
| Slot.java | common/persistence/model/reservation/ | LEGACY_CANDIDATE | - | SHARED | DEPRECATED |
| ClosedSlot.java | common/persistence/model/reservation/ | LEGACY_CANDIDATE | - | SHARED | DEPRECATED |
| SlotChangePolicy.java | common/persistence/model/reservation/ | LEGACY_CANDIDATE | - | SHARED | DEPRECATED |
| AbstractUser.java | common/persistence/model/user/ | KEEP | persistence/model/user/ | SHARED | Base user |
| BaseRole.java | common/persistence/model/user/ | KEEP | persistence/model/user/ | SHARED | Base role |
| BasePrivilege.java | common/persistence/model/user/ | KEEP | persistence/model/user/ | SHARED | Base privilege |
| AFcmToken.java | common/persistence/model/fcm/ | KEEP | persistence/model/fcm/ | SHARED | FCM base |
| ANotification.java | common/persistence/model/notification/ | KEEP | persistence/model/notification/ | SHARED | Base notification |
| EventOutbox.java | common/persistence/model/notification/ | KEEP | persistence/model/notification/ | SHARED | Event outbox |
| NotificationOutbox.java | common/persistence/model/notification/ | KEEP | persistence/model/notification/ | SHARED | Notification outbox |

#### 4.3.2 Context-Specific Persistence

| Module | Files | Status | Recommendation |
|--------|-------|--------|----------------|
| admin/persistence/ | 15 | MOVE | persistence/model/admin/ + persistence/dao/admin/ |
| agency/persistence/ | 9 | MOVE | persistence/model/agency/ + persistence/dao/agency/ |
| customer/persistence/ | 14 | MOVE | persistence/model/customer/ + persistence/dao/customer/ |
| restaurant/persistence/ | 53 | MOVE | persistence/model/restaurant/ + persistence/dao/restaurant/ |

#### 4.3.3 Duplicate DAOs

| File | Location 1 | Location 2 | Resolution |
|------|------------|------------|------------|
| ReservationAuditDAO.java | common/persistence/dao/ | customer/persistence/dao/ | CONSOLIDATE to common |

---

### 4.4 DTOs (Data Transfer Objects)

#### 4.4.1 Common DTOs That Should Stay Common

| File | Current Path | Status | Reason |
|------|--------------|--------|--------|
| AuthRequestDTO.java | common/web/dto/security/ | KEEP | Cross-context auth |
| AuthResponseDTO.java | common/web/dto/security/ | KEEP | Cross-context auth |
| RefreshTokenRequestDTO.java | common/web/dto/security/ | KEEP | Cross-context auth |
| ReservationDTO.java | common/web/dto/reservations/ | KEEP | Used by all contexts |
| RestaurantDTO.java | common/web/dto/restaurant/ | KEEP | Used by all contexts |
| CustomerDTO.java | common/web/dto/customer/ | KEEP | Used by all contexts |
| ServiceDTO.java | common/web/dto/restaurant/ | KEEP | Used by all contexts |
| ServiceVersionDTO.java | common/web/dto/restaurant/ | KEEP | Used by all contexts |

#### 4.4.2 DTOs That Should Move to Context

| File | Current Path | Proposed Path | Context | Reason |
|------|--------------|---------------|---------|--------|
| AdminNewReservationDTO.java | admin/web/dto/reservation/ | api/admin/dto/ | ADMIN | Admin-only DTO |
| RestaurantNewReservationDTO.java | restaurant/web/dto/reservation/ | api/restaurant/dto/ | RESTAURANT_USER | Restaurant-only |
| CustomerNewReservationDTO.java | customer/web/dto/reservations/ | api/customer/dto/ | CUSTOMER | Customer-only |
| NewRUserDTO.java | restaurant/web/dto/staff/ | api/restaurant/dto/ | RESTAURANT_USER | Staff creation |
| AgencyCreateDTO.java | agency/web/dto/ | api/agency/dto/ | AGENCY_USER | Agency creation |

---

### 4.5 Security Components

| File | Current Path | Status | Proposed Path | Context | Reason |
|------|--------------|--------|---------------|---------|--------|
| SecurityConfig.java | common/spring/ | MOVE | infrastructure/security/ | SHARED | Core security |
| JwtUtil.java | common/security/jwt/ | MOVE | infrastructure/security/jwt/ | SHARED | JWT utilities |
| TokenTypeValidationFilter.java | common/security/ | MOVE | infrastructure/security/filter/ | SHARED | Token validation |
| RUserRequestFilter.java | restaurant/ | MOVE | infrastructure/security/filter/ | RESTAURANT_USER | Context filter |
| CustomerRequestFilter.java | customer/ | MOVE | infrastructure/security/filter/ | CUSTOMER | Context filter |
| AdminRequestFilter.java | admin/ | MOVE | infrastructure/security/filter/ | ADMIN | Context filter |
| AgencyUserRequestFilter.java | agency/ | MOVE | infrastructure/security/filter/ | AGENCY_USER | Context filter |
| RUserHubValidationFilter.java | restaurant/ | MOVE | infrastructure/security/filter/ | RESTAURANT_USER_HUB | Hub validation |
| AgencyUserHubValidationFilter.java | agency/ | MOVE | infrastructure/security/filter/ | AGENCY_USER_HUB | Hub validation |
| WebSocketChannelInterceptor.java | common/security/websocket/ | MOVE | infrastructure/security/websocket/ | SHARED | WebSocket security |

---

### 4.6 Configuration

| File | Current Path | Status | Proposed Path | Reason |
|------|--------------|--------|---------------|--------|
| PersistenceJPAConfig.java | common/spring/ | MOVE | infrastructure/config/ | DB config |
| WebSocketConfig.java | common/config/ | MOVE | infrastructure/config/ | WebSocket config |
| RabbitMQConfig.java | common/config/ | MOVE | infrastructure/config/ | RabbitMQ config |
| CacheConfig.java | common/spring/config/ | MOVE | infrastructure/config/ | Cache config |
| AsyncConfig.java | common/spring/config/ | MOVE | infrastructure/config/ | Async config |
| SwaggerConfig.java | common/spring/swagger/ | MOVE | infrastructure/config/ | API docs |
| TwilioConfig.java | common/spring/ | MOVE | infrastructure/external/twilio/ | External service |
| MailConfig.java | common/spring/ | MOVE | infrastructure/external/email/ | External service |

---

## 5. Legacy Code Summary

### 5.1 Files Marked for Removal

| File | Reason | Replaced By | Safe to Delete After |
|------|--------|-------------|---------------------|
| SlotTransitionController.java | DEPRECATED since v2.0 | ServiceVersionScheduleController | v3.0 (Q2 2025) |
| RestaurantSlotController.java | DEPRECATED since v2.0 | ServiceVersionScheduleController | v3.0 (Q2 2025) |
| CustomerSlotController.java | DEPRECATED since v2.0 | Customer uses new schedule API | v3.0 (Q2 2025) |
| SlotService.java | DEPRECATED | ServiceVersionScheduleService | v3.0 (Q2 2025) |
| SlotTransitionService.java | DEPRECATED | ServiceVersionScheduleService | v3.0 (Q2 2025) |
| Slot.java | DEPRECATED entity | ServiceVersionSlotConfig | v3.0 (Q2 2025) |
| ClosedSlot.java | DEPRECATED entity | AvailabilityException | v3.0 (Q2 2025) |
| SlotChangePolicy.java | DEPRECATED enum | ServiceVersion state machine | v3.0 (Q2 2025) |
| AdminTestEmailController.java | Test controller | Remove from production | Immediately |
| AdminTestWhatsAppController.java | Test controller | Remove from production | Immediately |

### 5.2 Pre-Deletion Verification Commands

```bash
# Verify no usages of deprecated Slot classes
grep -rn "import.*\.Slot" src/main/java --include="*.java" | grep -v "SlotConfig\|SlotDTO"

# Verify no usages of deprecated controllers
grep -rn "SlotTransitionController\|RestaurantSlotController\|CustomerSlotController" src/main/java

# Verify test controllers are not referenced
grep -rn "AdminTestEmailController\|AdminTestWhatsAppController" src/main/java
```

---

## 6. Observations and Recommendations

### 6.1 Service Consolidation Opportunities

1. **ReservationService Consolidation**
   - `common/service/reservation/ReservationService.java` - Core logic
   - `customer/service/reservation/CustomerReservationService.java` - Customer-specific
   - `admin/service/AdminReservationService.java` - Admin-specific
   - **Recommendation:** Keep core in `ReservationService`, have context-specific services delegate to it

2. **Authentication Service Pattern**
   - Each context has its own authentication service (good separation)
   - Consider creating shared `AbstractAuthenticationService` for common logic

3. **Notification Orchestration**
   - Complex notification logic spread across multiple orchestrators
   - Consider simplifying with strategy pattern

### 6.2 API Path Recommendations

Current:
- `/admin/**`
- `/customer/**`
- `/restaurant/**`
- `/agency/**`

Proposed additions for Hub users:
- `/restaurant-hub/**` - Multi-restaurant management
- `/agency-hub/**` - Multi-agency management

**Implementation:** Create new security filter chains in `SecurityConfig.java`

### 6.3 Package Naming Conventions

Current naming inconsistencies:
- `rUser` vs `restaurant_user` vs `RUser`
- `dto` sometimes in `web/dto`, sometimes in root

**Recommendation:** Standardize on:
- snake_case for package names: `restaurant_user`, `agency_user`
- All DTOs in `dto/` subdirectory of their context

### 6.4 Future Module Extraction (Optional)

If the monolith grows, consider extracting to separate Spring Boot apps:
1. `greedys-admin-api` - Admin management
2. `greedys-customer-api` - Customer app backend
3. `greedys-restaurant-api` - Restaurant app backend
4. `greedys-agency-api` - Agency app backend
5. `greedys-core` - Shared library (domain, persistence)

---

## 7. Proposed git mv Commands

### 7.1 Phase 1: Create Target Structure

```bash
# Create new package directories
mkdir -p src/main/java/com/application/api/{admin,customer,restaurant,restaurant_hub,agency,agency_hub,public,shared}
mkdir -p src/main/java/com/application/core/{domain,service}
mkdir -p src/main/java/com/application/infrastructure/{security,config,external,web}
mkdir -p src/main/java/com/application/persistence/{model,dao}
```

### 7.2 Phase 2: Move Admin Controllers

```bash
# Admin auth controllers
git mv src/main/java/com/application/admin/controller/admin/AdminAuthenticationController.java src/main/java/com/application/api/admin/auth/
git mv src/main/java/com/application/admin/controller/admin/AdminRegistrationController.java src/main/java/com/application/api/admin/auth/
git mv src/main/java/com/application/admin/controller/admin/AdminController.java src/main/java/com/application/api/admin/user/

# Admin resource controllers
git mv src/main/java/com/application/admin/controller/AdminAllergyController.java src/main/java/com/application/api/admin/allergy/
git mv src/main/java/com/application/admin/controller/AdminCustomerController.java src/main/java/com/application/api/admin/customer/
git mv src/main/java/com/application/admin/controller/AdminReservationController.java src/main/java/com/application/api/admin/reservation/
git mv src/main/java/com/application/admin/controller/AdminServicesController.java src/main/java/com/application/api/admin/service/

# Admin verification
git mv src/main/java/com/application/admin/controller/AdminRestaurantVerificationController.java src/main/java/com/application/api/admin/verification/
git mv src/main/java/com/application/admin/controller/TwilioRestaurantVerificationController.java src/main/java/com/application/api/admin/verification/

# Admin restaurant management
git mv src/main/java/com/application/admin/controller/restaurant/AdminCategoryController.java src/main/java/com/application/api/admin/restaurant/
git mv src/main/java/com/application/admin/controller/restaurant/AdminRestaurantManagementController.java src/main/java/com/application/api/admin/restaurant/
git mv src/main/java/com/application/admin/controller/restaurant/AdminRestaurantReservationController.java src/main/java/com/application/api/admin/reservation/
git mv src/main/java/com/application/admin/controller/restaurant/AdminRoomTableController.java src/main/java/com/application/api/admin/restaurant/
git mv src/main/java/com/application/admin/controller/restaurant/AdminRUserController.java src/main/java/com/application/api/admin/restaurant/user/
git mv src/main/java/com/application/admin/controller/restaurant/AdminUserController.java src/main/java/com/application/api/admin/restaurant/user/
```

### 7.3 Phase 3: Move Customer Controllers

```bash
# Customer auth
git mv src/main/java/com/application/customer/controller/customer/CustomerAuthenticationController.java src/main/java/com/application/api/customer/auth/
git mv src/main/java/com/application/customer/controller/customer/CustomerRegistrationController.java src/main/java/com/application/api/customer/auth/
git mv src/main/java/com/application/customer/controller/customer/CustomerController.java src/main/java/com/application/api/customer/profile/
git mv src/main/java/com/application/customer/controller/customer/CustomerAllergyController.java src/main/java/com/application/api/customer/allergy/

# Customer resources
git mv src/main/java/com/application/customer/controller/CustomerReservationController.java src/main/java/com/application/api/customer/reservation/
git mv src/main/java/com/application/customer/controller/CustomerNotificationController.java src/main/java/com/application/api/customer/notification/
git mv src/main/java/com/application/customer/controller/matching/CustomerMatchController.java src/main/java/com/application/api/customer/matching/

# Customer restaurant views
git mv src/main/java/com/application/customer/controller/restaurant/CustomerRestaurantInfoController.java src/main/java/com/application/api/customer/restaurant/
git mv src/main/java/com/application/customer/controller/restaurant/CustomerMenuController.java src/main/java/com/application/api/customer/restaurant/
git mv src/main/java/com/application/customer/controller/restaurant/CustomerRoomController.java src/main/java/com/application/api/customer/restaurant/
git mv src/main/java/com/application/customer/controller/restaurant/CustomerTableController.java src/main/java/com/application/api/customer/restaurant/
git mv src/main/java/com/application/customer/controller/restaurant/CustomerRestaurantServiceController.java src/main/java/com/application/api/customer/restaurant/
```

### 7.4 Phase 4: Move Restaurant Controllers

```bash
# Restaurant auth
git mv src/main/java/com/application/restaurant/controller/RestaurantAuthenticationController.java src/main/java/com/application/api/restaurant/auth/
git mv src/main/java/com/application/restaurant/controller/RestaurantRegistrationController.java src/main/java/com/application/api/restaurant/auth/

# Restaurant resources
git mv src/main/java/com/application/restaurant/controller/RestaurantReservationController.java src/main/java/com/application/api/restaurant/reservation/
git mv src/main/java/com/application/restaurant/controller/RestaurantNotificationController.java src/main/java/com/application/api/restaurant/notification/
git mv src/main/java/com/application/restaurant/controller/RestaurantCustomerController.java src/main/java/com/application/api/restaurant/customer/
git mv src/main/java/com/application/restaurant/controller/RestaurantCustomerContactController.java src/main/java/com/application/api/restaurant/customer/

# Restaurant configuration
git mv src/main/java/com/application/restaurant/controller/restaurant/RestaurantInfoController.java src/main/java/com/application/api/restaurant/info/
git mv src/main/java/com/application/restaurant/controller/restaurant/RestaurantMenuController.java src/main/java/com/application/api/restaurant/menu/
git mv src/main/java/com/application/restaurant/controller/restaurant/RestaurantRoomController.java src/main/java/com/application/api/restaurant/room/
git mv src/main/java/com/application/restaurant/controller/restaurant/RestaurantTableController.java src/main/java/com/application/api/restaurant/table/
git mv src/main/java/com/application/restaurant/controller/restaurant/RestaurantServicesController.java src/main/java/com/application/api/restaurant/service/
git mv src/main/java/com/application/restaurant/controller/restaurant/RestaurantSlotManagementController.java src/main/java/com/application/api/restaurant/schedule/
git mv src/main/java/com/application/restaurant/controller/restaurant/ServiceVersionScheduleController.java src/main/java/com/application/api/restaurant/schedule/

# Restaurant user management
git mv src/main/java/com/application/restaurant/controller/rUser/RUserAuthController.java src/main/java/com/application/api/restaurant/user/auth/
git mv src/main/java/com/application/restaurant/controller/rUser/RUserController.java src/main/java/com/application/api/restaurant/user/
git mv src/main/java/com/application/restaurant/controller/rUser/RUserFcmController.java src/main/java/com/application/api/restaurant/user/

# Restaurant agenda
git mv src/main/java/com/application/restaurant/controller/agenda/RestaurantCustomerAgendaController.java src/main/java/com/application/api/restaurant/agenda/

# Google integration
git mv src/main/java/com/application/restaurant/controller/google/GoogleBusinessVerificationController.java src/main/java/com/application/api/restaurant/google/
git mv src/main/java/com/application/restaurant/controller/google/GoogleReserveController.java src/main/java/com/application/api/restaurant/google/

# Public booking form
git mv src/main/java/com/application/restaurant/controller/BookingFormController.java src/main/java/com/application/api/public/booking/
```

### 7.5 Phase 5: Move Agency Controllers

```bash
git mv src/main/java/com/application/agency/controller/AgencyAuthenticationController.java src/main/java/com/application/api/agency/auth/
git mv src/main/java/com/application/agency/controller/AgencyRegistrationController.java src/main/java/com/application/api/agency/auth/
git mv src/main/java/com/application/agency/controller/AgencyController.java src/main/java/com/application/api/agency/management/
git mv src/main/java/com/application/agency/controller/AgencyReservationController.java src/main/java/com/application/api/agency/reservation/
```

### 7.6 Phase 6: Move Infrastructure

```bash
# Security
git mv src/main/java/com/application/common/spring/SecurityConfig.java src/main/java/com/application/infrastructure/security/
git mv src/main/java/com/application/common/security/jwt/ src/main/java/com/application/infrastructure/security/
git mv src/main/java/com/application/common/security/TokenTypeValidationFilter.java src/main/java/com/application/infrastructure/security/filter/
git mv src/main/java/com/application/common/security/websocket/ src/main/java/com/application/infrastructure/security/

# Config
git mv src/main/java/com/application/common/spring/PersistenceJPAConfig.java src/main/java/com/application/infrastructure/config/
git mv src/main/java/com/application/common/config/WebSocketConfig.java src/main/java/com/application/infrastructure/config/
git mv src/main/java/com/application/common/config/RabbitMQConfig.java src/main/java/com/application/infrastructure/config/
git mv src/main/java/com/application/common/spring/config/ src/main/java/com/application/infrastructure/config/

# External services
git mv src/main/java/com/application/common/service/EmailService.java src/main/java/com/application/infrastructure/external/email/
git mv src/main/java/com/application/common/service/WhatsAppService.java src/main/java/com/application/infrastructure/external/twilio/
git mv src/main/java/com/application/common/service/FirebaseService.java src/main/java/com/application/infrastructure/external/firebase/
git mv src/main/java/com/application/common/service/authentication/GoogleAuthService.java src/main/java/com/application/infrastructure/external/google/
```

### 7.7 Phase 7: Move Core Services

```bash
# Core reservation
git mv src/main/java/com/application/common/service/reservation/ src/main/java/com/application/core/service/reservation/

# Core restaurant
git mv src/main/java/com/application/common/service/RestaurantService.java src/main/java/com/application/core/service/restaurant/
git mv src/main/java/com/application/common/service/RestaurantCategoryService.java src/main/java/com/application/core/service/restaurant/

# Core notification
git mv src/main/java/com/application/common/service/notification/ src/main/java/com/application/core/service/notification/

# Domain events
git mv src/main/java/com/application/common/domain/ src/main/java/com/application/core/domain/
```

---

## 8. Candidate Deletes

### 8.1 Deprecated Controllers (Remove in v3.0)

```bash
# After verifying no production dependencies:
rm src/main/java/com/application/restaurant/web/SlotTransitionController.java
rm src/main/java/com/application/restaurant/controller/restaurant/RestaurantSlotController.java
rm src/main/java/com/application/customer/controller/restaurant/CustomerSlotController.java
```

### 8.2 Deprecated Services (Remove in v3.0)

```bash
rm src/main/java/com/application/restaurant/service/SlotService.java
rm src/main/java/com/application/restaurant/service/SlotTransitionService.java
```

### 8.3 Deprecated Entities (Remove in v3.0)

```bash
rm src/main/java/com/application/common/persistence/model/reservation/Slot.java
rm src/main/java/com/application/common/persistence/model/reservation/ClosedSlot.java
rm src/main/java/com/application/common/persistence/model/reservation/SlotChangePolicy.java
```

### 8.4 Test Controllers (Remove from Production)

```bash
rm src/main/java/com/application/admin/controller/test/AdminTestEmailController.java
rm src/main/java/com/application/admin/controller/test/AdminTestWhatsAppController.java
```

### 8.5 Pre-Delete Verification

Before deleting any file, run:

```bash
# Find all references to a class
CLASS_NAME="SlotTransitionController"
grep -rn "$CLASS_NAME" src/main/java --include="*.java"
grep -rn "$CLASS_NAME" src/test/java --include="*.java"

# Verify no Spring configuration references
grep -rn "$CLASS_NAME" src/main/resources
```

---

## 9. Implementation Roadmap

### Phase 1: Non-Breaking Changes (Week 1-2)
1. Create new package structure (directories only)
2. Move infrastructure components (security, config)
3. Move core services
4. Update all imports

### Phase 2: API Layer Reorganization (Week 3-4)
1. Move controllers to new api/ structure
2. Update Spring MVC mappings if needed
3. Update Swagger documentation
4. Test all endpoints

### Phase 3: Create Hub Endpoints (Week 5-6)
1. Create `/restaurant-hub/**` security chain
2. Create `/agency-hub/**` security chain
3. Move/create hub-specific controllers
4. Test hub authentication flows

### Phase 4: Legacy Cleanup (Week 7-8)
1. Verify deprecated code has no usages
2. Remove deprecated controllers
3. Remove deprecated services
4. Remove deprecated entities
5. Database migration if needed

### Phase 5: Documentation & Testing (Week 9-10)
1. Update API documentation
2. Update architecture documentation
3. Full regression testing
4. Performance testing

---

## 10. Appendix: File Count Summary

| Category | Current Count | After Refactoring |
|----------|--------------|-------------------|
| Admin Controllers | 17 | 15 (remove 2 test) |
| Customer Controllers | 12 | 11 (remove 1 deprecated) |
| Restaurant Controllers | 25+ | 20+ (remove deprecated) |
| Agency Controllers | 4 | 4 |
| Core Services | 25 | 25 |
| Persistence Models | 70+ | 65+ (remove deprecated) |
| Deprecated Files | 10 | 0 |
| **Total Reduction** | - | **~15 files** |

---

*Document generated by architectural analysis. Review with the development team before implementing changes.*
