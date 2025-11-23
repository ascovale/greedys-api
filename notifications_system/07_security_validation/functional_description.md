# Security Validation - Functional Description

Multi-layer JWT authentication and role-based destination access control for WebSocket connections.

**3 Security Layers**:
1. **Handshake**: JWT token validation
2. **STOMP Frame**: Destination authorization
3. **Integration**: Consistent with REST security (same JWT secret, roles)

**Authentication**:
- Extract JWT from Authorization header or ?token query param
- Validate signature and expiration
- Extract user claims (userId, userType)
- Store in session for later STOMP frame validation

**Authorization**:
- Rule 1: `/user/{id}/queue/*` → Only user {id} can subscribe
- Rule 2: `/topic/{role}/*` → Only users with matching role
- Rule 3: `/broadcast/*` → Only ADMIN users
- Rule 4: Cross-role subscriptions DENIED

**User Types**: RESTAURANT, CUSTOMER, AGENCY, ADMIN (+ hub variants)

---

**Document Version**: 1.0  
**Component**: Security Validation (WebSocket Authentication & Authorization)
