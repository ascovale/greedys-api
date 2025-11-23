# Shared Read & Read Status - Functional Description

## Shared Read Strategy

When user marks notification as read, propagate to related users based on scope.

**Scopes**:
- NONE: Individual only
- RESTAURANT: Entire restaurant staff
- RESTAURANT_HUB: Hub-level group
- RESTAURANT_HUB_ALL: All hub members (broadcast)

**Implementation**: Strategy pattern with scope-specific UPDATE queries

---

## Read Status Management

Track read/unread status with optional shared propagation and real-time broadcast.

**Operations**:
1. User marks notification as READ (WebSocket)
2. Handler checks `read_by_all` flag
3. If true: Call SharedReadService with scope
4. Strategy executes scope-specific UPDATE
5. Broadcast update to connected users
6. All affected users see notification as READ

**Broadcast**: `/topic/notifications/{userId}/{type}` with READ status update

---

**Document Version**: 1.0  
**Components**: Shared Read Strategy & Read Status Management
