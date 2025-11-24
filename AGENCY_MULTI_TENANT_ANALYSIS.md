# Agency Multi-Tenant Architecture Analysis

## Summary: ❌ DIFFERENT Implementation

The Agency system has a **DIFFERENT architecture** compared to Restaurant (RUser + RestaurantUserHub).

---

## Architecture Comparison

### Restaurant Multi-Tenant (✅ Already Implemented)

```
RestaurantUserHub (1 hub can manage multiple restaurants)
    ├─ email
    ├─ password
    ├─ 1..* RUser (one per restaurant)
    │   ├─ email
    │   ├─ restaurantId  ← Link to specific restaurant
    │   └─ JWT contains: "email:restaurantId"
    │
    └─ Methods:
       ├─ selectRestaurant(restaurantId)  ← Generates NEW JWT for specific restaurant
       ├─ changeRestaurant(restaurantId)  ← Same as selectRestaurant
       └─ refreshRUserToken(refreshToken) ← Parses email:restaurantId from token
```

**Key Point**: RUser table has explicit `restaurantId` column for hub management.

---

### Agency Multi-Tenant (❌ Different Pattern)

```
AgencyUserHub (1 hub that manages multiple agencies)
    ├─ email
    ├─ password
    ├─ firstName
    ├─ lastName
    ├─ phoneNumber
    ├─ status (VERIFY_TOKEN, ENABLED, BLOCKED, DELETED, DISABLED)
    │
    └─ 1..* AgencyUser (one per agency)
        ├─ email
        ├─ name
        ├─ surname
        ├─ phoneNumber
        ├─ agencyId  ← Link to specific agency
        └─ status (must match Hub status)

    Methods (in AgencyUserService):
    ├─ registerAgencyUser(email, agencyId, ...)  ← Register user for specific agency
    ├─ findByEmailAndAgencyId(email, agencyId)   ← Find user by email + agency
    ├─ getUserAgencies(userEmail)                ← Get all agencies for user
    ├─ hasUserAccessToAgency(email, agencyId)    ← Check if user has access
    └─ ❌ NO selectAgency() or changeAgency() method!
```

**Key Difference**: 
- **Restaurant**: `selectRestaurant()` generates new JWT for that restaurant
- **Agency**: ❌ NO equivalent method exists yet!

---

## Current Agency Authentication Status

### AgencyAuthenticationController.java (Lines 1-60)

```java
@PostMapping(value = "/login", produces = "application/json")
public ResponseEntity<AuthResponseDTO> login(@RequestBody AgencyLoginRequestDTO request) {
    return execute("agency login", () -> {
        // ❌ TODO: Implement authentication logic
        throw new UnsupportedOperationException("Agency authentication not yet implemented");
    });
}

@PostMapping(value = "/refresh", produces = "application/json")
public ResponseEntity<AuthResponseDTO> refresh(@RequestBody RefreshTokenRequestDTO request) {
    return execute("refresh agency token", () -> {
        // ❌ TODO: Implement refresh logic
        throw new UnsupportedOperationException("Agency token refresh not yet implemented");
    });
}

@PostMapping(value = "/refresh/hub", produces = "application/json")
public ResponseEntity<AuthResponseDTO> refreshHub(@RequestBody RefreshTokenRequestDTO request) {
    return execute("refresh agency hub token", () -> {
        // ❌ TODO: Implement hub refresh logic
        throw new UnsupportedOperationException("Agency hub token refresh not yet implemented");
    });
}
```

**Status**: ❌ **NOT YET IMPLEMENTED** - All methods throw UnsupportedOperationException

---

## Detailed Structure Comparison

### Data Model Layer

#### Restaurant (Complete ✅)

| Component | Purpose | Status |
|-----------|---------|--------|
| `RestaurantUserHub` | Hub user (manages multiple restaurants) | ✅ |
| `RUser` | Single restaurant user | ✅ |
| `RUserDAO.findByEmailAndRestaurantId()` | Find user by email+restaurant | ✅ |
| JWT Format | `email:restaurantId` | ✅ |

#### Agency (Partial ❌)

| Component | Purpose | Status |
|-----------|---------|--------|
| `AgencyUserHub` | Hub user (manages multiple agencies) | ✅ |
| `AgencyUser` | Single agency user | ✅ |
| `AgencyUserDAO.findAgencyUsersByEmail()` | Find user by email only (no agencyId) | ⚠️ Incomplete |
| JWT Format | ??? Not yet defined | ❌ TODO |
| `selectAgency()` | Switch to specific agency JWT | ❌ Missing |

---

## Service Layer Comparison

### RestaurantAuthenticationService (Complete ✅)

```java
// ✅ For Hub Login
public AuthResponseDTO loginWithHubSupport() { ... }
public AuthResponseDTO refreshHubToken(String refreshToken) { ... }

// ✅ For RUser (Single Restaurant)
public AuthResponseDTO refreshRUserToken(String refreshToken) {
    String username = jwtUtil.extractUsername(refreshToken);
    String[] parts = username.split(":");  // email:restaurantId
    String email = parts[0];
    Long restaurantId = Long.parseLong(parts[1]);
    
    RUser rUser = RUserDAO.findByEmailAndRestaurantId(email, restaurantId);
    // ... regenerate JWT with same restaurantId
}

// ✅ For Switching Restaurants
public AuthResponseDTO selectRestaurant(Long restaurantId) {
    RUser user = RUserDAO.findByEmailAndRestaurantId(email, restaurantId);
    String jwt = jwtUtil.generateToken(user);  // NEW JWT for this restaurant
    return new AuthResponseDTO(jwt, ...);
}
```

### AgencyAuthenticationService (Incomplete ❌)

```java
// ✅ For Hub Registration Verification
public String confirmAgencyUserHubRegistration(String token) { ... }

// ❌ TODO: For Hub Login
public AuthResponseDTO loginWithHubSupport() { 
    throw new UnsupportedOperationException(...);
}

// ❌ TODO: For AgencyUser (Single Agency)
public AuthResponseDTO refreshAgencyUserToken(String refreshToken) {
    throw new UnsupportedOperationException(...);
}

// ❌ TODO: For Switching Agencies
public AuthResponseDTO selectAgency(Long agencyId) {
    throw new UnsupportedOperationException(...);
}
```

**Status**: Only hub registration verification is implemented.

---

## JWT Token Format Comparison

### Restaurant RUser JWT

```json
{
  "sub": "mario@email.com:5",        // email:restaurantId
  "user_type": "restaurant-user",
  "user_id": 123,
  "restaurant_id": 5,
  "email": "mario@email.com",
  "authorities": ["ROLE_RESTAURANT"],
  "iat": 1700815200,
  "exp": 1700818800                  // 1 hour
}
```

### Agency User JWT (Not Yet Defined)

```json
{
  "sub": ???,                        // ??? Should be "email:agencyId"?
  "user_type": "agency-user",       // ??? Or "agency-hub-user"?
  "user_id": ???,
  "agency_id": ???,                 // ??? 
  "email": ???,
  "authorities": ["ROLE_AGENCY"],
  "iat": 1700815200,
  "exp": 1700818800
}
```

**Status**: JWT format NOT YET DEFINED for Agency

---

## Multi-Agency Selection Flow (Missing)

### For Restaurant (Already Works ✅)

```
1. Hub user logs in
   └─ Gets HUB JWT + refresh token

2. Hub user selects a restaurant
   POST /api/restaurant/auth/select-restaurant {restaurantId: 5}
   
3. selectRestaurant(5) is called
   ├─ Loads RUser with email + restaurantId=5
   ├─ Generates NEW JWT with "email:5" in subject
   └─ Returns new JWT (no refresh token) + user details

4. Now RUser is bound to restaurantId=5
   └─ WebSocket validates destination against restaurantId in JWT

5. If RUser switches restaurants
   POST /api/restaurant/auth/select-restaurant {restaurantId: 7}
   └─ New JWT generated with "email:7"
```

### For Agency (Missing ❌)

```
1. Hub user logs in with email + password
   └─ ??? Should get HUB JWT + refresh token

2. Hub user selects an agency
   POST /api/agency/auth/select-agency {agencyId: 10}
   └─ ❌ ENDPOINT DOESN'T EXIST!

3. selectAgency(10) should be called (NOT IMPLEMENTED)
   ├─ Load AgencyUser with email + agencyId=10
   ├─ Generate NEW JWT with ??? format
   └─ Return new JWT + user details

4. Now AgencyUser is bound to agencyId=10
   └─ WebSocket validates destination against agencyId in JWT

5. If AgencyUser switches agencies
   POST /api/agency/auth/select-agency {agencyId: 12}
   └─ New JWT generated
```

---

## Missing Implementation Checklist

### 1. AgencyAuthenticationService

- [ ] `loginWithHub(String email, String password, String agencyId)` 
  - Returns: HUB JWT + refresh token (for hub-level access)
  
- [ ] `loginAsAgencyUser(String email, String password, Long agencyId)`
  - Returns: AGENCY USER JWT + refresh token (for specific agency)
  
- [ ] `selectAgency(Long agencyId)`
  - Like `selectRestaurant()` - generates new JWT for specific agency
  - Returns: New AGENCY USER JWT (no refresh token)
  
- [ ] `refreshAgencyUserToken(String refreshToken)`
  - Validates refresh token
  - Extracts email + agencyId
  - Returns: New AGENCY USER JWT + refresh token
  
- [ ] `refreshHubToken(String refreshToken)`
  - Similar to Restaurant's `refreshHubToken()`

### 2. JWT Token Format

Need to define JWT format for AgencyUser:

```json
// Option A: Match Restaurant pattern
{
  "sub": "mario@email.com:10",      // email:agencyId
  "user_type": "agency-user",
  "user_id": 456,
  "agency_id": 10,
  "email": "mario@email.com"
}

// Option B: Flat structure
{
  "sub": "mario@email.com",
  "agency_id": 10,
  "user_type": "agency-user"
}
```

**Recommendation**: Use Option A (Option B) to match Restaurant pattern!

### 3. Controller Endpoints

**File**: `AgencyAuthenticationController.java`

```java
@PostMapping(value = "/login", produces = "application/json")
public ResponseEntity<AuthResponseDTO> login(@RequestBody AgencyLoginRequestDTO request) {
    // Implement
}

@PostMapping(value = "/refresh", produces = "application/json")
public ResponseEntity<AuthResponseDTO> refresh(@RequestBody RefreshTokenRequestDTO request) {
    // Implement
}

@PostMapping(value = "/refresh/hub", produces = "application/json")
public ResponseEntity<AuthResponseDTO> refreshHub(@RequestBody RefreshTokenRequestDTO request) {
    // Implement
}

// NEW ENDPOINT
@PostMapping(value = "/select-agency", produces = "application/json")
public ResponseEntity<AuthResponseDTO> selectAgency(@RequestBody SelectAgencyRequestDTO request) {
    // Implement - like selectRestaurant() but for agencies
}

// NEW ENDPOINT
@PostMapping(value = "/change-agency", produces = "application/json")
public ResponseEntity<AuthResponseDTO> changeAgency(@RequestBody SelectAgencyRequestDTO request) {
    // Implement - alias for selectAgency
}
```

### 4. Helper DAO Method

**File**: `AgencyUserDAO.java`

Add method (currently missing):
```java
// ✅ Already have findAgencyUsersByEmail(email)
// ❌ NEED: findByEmailAndAgencyId(email, agencyId)

Optional<AgencyUser> findByEmailAndAgencyId(String email, Long agencyId);
```

---

## WebSocket Implications

### For Agency Users (Currently Missing)

Current WebSocket destination for AgencyUser:
```
/topic/agency/{agencyId}/notifications
```

But **AgencyUser JWT doesn't contain agencyId yet**, so validation will fail!

#### WebSocketDestinationValidator needs to handle:

```java
// Current (for Restaurant)
public boolean validateRestaurantReservationsAccess(
        String destination,
        Long restaurantIdFromJwt,
        Long restaurantIdFromUrl) {
    return restaurantIdFromJwt.equals(restaurantIdFromUrl);
}

// Missing (for Agency)
public boolean validateAgencyNotificationsAccess(
        String destination,
        Long agencyIdFromJwt,
        Long agencyIdFromUrl) {
    // Need agencyId in JWT first!
    return agencyIdFromJwt.equals(agencyIdFromUrl);
}
```

---

## Implementation Priority

### Phase 1: Core Authentication (Must Have)

1. **AgencyAuthenticationService** 
   - `loginAsAgencyUser()` - Login to specific agency
   - `refreshAgencyUserToken()` - Refresh with email:agencyId parsing
   
2. **JWT Format** 
   - Use `email:agencyId` format (match Restaurant pattern)
   
3. **AgencyAuthenticationController**
   - Implement `/login`, `/refresh` endpoints

### Phase 2: Multi-Agency Support (Should Have)

4. **selectAgency()** / **changeAgency()**
   - Generate new JWT when switching agencies
   - Similar to `selectRestaurant()`
   
5. **refreshHub()**
   - For hub-level token refresh

### Phase 3: WebSocket Integration (Nice to Have)

6. **WebSocketDestinationValidator**
   - Add `validateAgencyNotificationsAccess()`
   - Extract agencyId from JWT

---

## Summary Table

| Feature | Restaurant | Agency | Status |
|---------|-----------|--------|--------|
| Hub user table | `RestaurantUserHub` | `AgencyUserHub` | ✅ Both exist |
| Single user table | `RUser` | `AgencyUser` | ✅ Both exist |
| Multi-tenant lookup | `findByEmailAndRestaurantId()` | `findByEmailAndAgencyId()` | ❌ Agency missing |
| Hub login | ✅ Implemented | ❌ TODO |
| Single user login | ✅ Implemented | ❌ TODO |
| Select/Switch tenant | ✅ `selectRestaurant()` | ❌ `selectAgency()` TODO |
| Token refresh | ✅ `refreshRUserToken()` | ❌ TODO |
| JWT format | ✅ `email:restaurantId` | ❌ Not defined |
| WebSocket integration | ✅ Working | ❌ Blocked by JWT |

---

## Conclusion

**Agency Architecture is LESS COMPLETE than Restaurant**:

✅ **Data Model**: Exists (AgencyUserHub + AgencyUser)
✅ **Hub Verification**: Implemented  
❌ **Authentication Services**: NOT IMPLEMENTED (all TODOs)
❌ **Multi-Agency Selection**: NOT IMPLEMENTED
❌ **JWT Format**: NOT DEFINED
❌ **WebSocket Ready**: NO (needs JWT format first)

**Same concept as Restaurant, but implementation stage is much earlier.**
