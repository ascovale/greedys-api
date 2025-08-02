# DTO Organization - Admin Web Package

This package contains all DTOs (Data Transfer Objects) for the admin web API, organized by functionality.

## Package Structure

```
com.application.admin.web.dto/
├── admin/                    # Admin management DTOs
│   ├── AdminDTO.java         # Admin details DTO
│   └── NewAdminDTO.java      # New admin creation DTO
├── communication/            # Communication DTOs
│   └── EmailRequestDTO.java  # Email request DTO
├── reservation/              # Reservation management DTOs
│   └── AdminNewReservationDTO.java  # Admin reservation creation DTO
├── service/                  # Service management DTOs
│   └── AdminNewServiceDTO.java       # Admin service creation DTO
└── verification/             # Restaurant verification DTOs
    ├── AdminVerificationResult.java     # Verification result DTO
    └── UserRestaurantAssociation.java   # User-restaurant association DTO
```

## Functionality Groups

### 1. Admin Management (`admin/`)
DTOs related to admin user management operations:
- Creating new admin accounts
- Admin profile information
- Admin authentication data

### 2. Communication (`communication/`)
DTOs for communication and messaging:
- Email requests
- Notification data
- Message templates

### 3. Reservation Management (`reservation/`)
DTOs for reservation operations managed by admins:
- Creating reservations for customers
- Modifying existing reservations
- Reservation status updates

### 4. Service Management (`service/`)
DTOs for restaurant service management:
- Creating new services
- Service configuration
- Service type definitions

### 5. Verification (`verification/`)
DTOs for restaurant verification processes:
- Restaurant ownership verification
- User-restaurant associations
- Verification results and status

## Usage Guidelines

1. **Import by functionality**: Import DTOs from their specific functional package
   ```java
   import com.application.admin.web.dto.admin.NewAdminDTO;
   import com.application.admin.web.dto.reservation.AdminNewReservationDTO;
   ```

2. **Package consistency**: All admin web DTOs should be placed in appropriate functional subpackages

3. **Naming conventions**: 
   - Use descriptive names that indicate the DTO purpose
   - Prefix with "Admin" for admin-specific operations
   - Suffix with "DTO" for clarity

## Migration Notes

This structure replaces the previous `get/` and `post/` organization, providing better logical grouping by business functionality instead of HTTP method.
