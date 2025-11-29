package com.application.common.domain.event;

/**
 * ⭐ Domain Event Types - Grouped by logical category
 * 
 * These are BUSINESS events independent of transport mechanism.
 * No mention of WebSocket, REST, or any infrastructure detail.
 * 
 * Events are immutable, timestamped facts about what happened in the business domain.
 */
public enum EventType {

    // ─────── RESERVATION LIFECYCLE EVENTS ───────
    /**
     * A reservation has been created.
     * Payload: reservationId, customerId, restaurantId, partySize, requestedDateTime
     */
    RESERVATION_CREATED("reservation.created"),

    /**
     * Covers all modifications to reservation details (generic).
     * Payload: reservationId, changedFields
     */
    RESERVATION_MODIFIED("reservation.modified"),

    /**
     * Specific: reservation time has been changed.
     * Payload: reservationId, oldTime, newTime
     */
    RESERVATION_TIME_CHANGED("reservation.time_changed"),

    /**
     * Specific: party size has been changed.
     * Payload: reservationId, oldPartySize, newPartySize
     */
    RESERVATION_PARTY_SIZE_CHANGED("reservation.party_size_changed"),

    /**
     * Specific: notes/special requests have been updated.
     * Payload: reservationId, oldNotes, newNotes
     */
    RESERVATION_NOTES_UPDATED("reservation.notes_updated"),

    /**
     * Specific: table assignment has changed.
     * Payload: reservationId, oldTableId, newTableId, oldTableNumber, newTableNumber
     */
    RESERVATION_TABLE_CHANGED("reservation.table_changed"),

    /**
     * Specific: staff assignment (e.g., assigned to a waiter).
     * Payload: reservationId, staffId, staffName, staffRole
     */
    RESERVATION_ASSIGNED_TO_STAFF("reservation.assigned_to_staff"),

    /**
     * ⚠️ CRITICAL: Reservation status has transitioned.
     * This is the PRIMARY event for status changes.
     * 
     * Payload fields MUST include:
     * - reservationId
     * - oldStatus (from Status enum)
     * - newStatus (from Status enum)
     * - reason (optional: why the change happened)
     * - rejectionReason (if newStatus = REJECTED)
     * 
     * Valid transitions:
     *   NOT_ACCEPTED → {ACCEPTED, REJECTED, DELETED}
     *   ACCEPTED → {SEATED, NO_SHOW, DELETED}
     *   (any status) → DELETED
     */
    RESERVATION_STATUS_CHANGED("reservation.status_changed"),


    // ─────── MESSAGING/CONVERSATION EVENTS ───────
    /**
     * A message has been sent within a reservation conversation.
     * Payload: reservationId, messageId, senderUserId, senderUserType, messageText, messageType
     */
    RESERVATION_MESSAGE_SENT("reservation.message_sent"),

    /**
     * A message has been marked as read.
     * Payload: reservationId, messageId, readByUserId
     */
    RESERVATION_MESSAGE_READ("reservation.message_read"),

    /**
     * Reservation conversation has been closed (archived).
     * Payload: reservationId, closedByUserId, closedByUserType, reason
     */
    RESERVATION_CONVERSATION_CLOSED("reservation.conversation_closed"),

    /**
     * Reservation conversation has been reopened.
     * Payload: reservationId, reopenedByUserId, reopenedByUserType, reason
     */
    RESERVATION_CONVERSATION_REOPENED("reservation.conversation_reopened"),


    // ─────── SUPPORT/ASSISTANCE EVENTS ───────
    /**
     * A support ticket has been opened for a reservation.
     * Payload: ticketId, reservationId, issueType, description
     */
    SUPPORT_TICKET_OPENED("support.ticket_opened"),

    /**
     * Support ticket has been resolved/closed.
     * Payload: ticketId, reservationId, resolution, closedByUserId
     */
    SUPPORT_TICKET_CLOSED("support.ticket_closed"),

    /**
     * Message sent within support ticket.
     * Payload: ticketId, messageId, senderUserId, messageText
     */
    SUPPORT_MESSAGE_SENT("support.message_sent"),

    /**
     * Ticket reassigned to different staff member.
     * Payload: ticketId, previousAssigneeId, newAssigneeId
     */
    SUPPORT_TICKET_ASSIGNED("support.ticket_assigned"),

    /**
     * Ticket escalated (e.g., to manager).
     * Payload: ticketId, escalatedToLevel, reason
     */
    SUPPORT_TICKET_ESCALATED("support.ticket_escalated"),


    // ─────── SYSTEM/INFRASTRUCTURE EVENTS ───────
    // Note: These should typically NOT be sent over WebSocket to clients.
    // They are internal coordination events.

    /**
     * Client WebSocket has connected.
     * Infrastructure event - used for sync coordination.
     * Payload: clientId, userId, userType, clientTimestamp
     */
    CLIENT_WS_CONNECTED("system.client_ws_connected"),

    /**
     * Client WebSocket has disconnected.
     * Infrastructure event.
     * Payload: clientId, userId, disconnectReason
     */
    CLIENT_WS_DISCONNECTED("system.client_ws_disconnected"),

    /**
     * Client WebSocket has reconnected (after temporary loss).
     * Infrastructure event - may trigger client resync.
     * Payload: clientId, userId, sessionGap, lastSeenTimestamp
     */
    CLIENT_WS_RECONNECTED("system.client_ws_reconnected"),

    /**
     * Client requests sync (typically after WS reconnect).
     * Infrastructure event.
     * Payload: clientId, userId, lastSyncTimestamp, syncType (FULL or DELTA)
     */
    CLIENT_SYNC_REQUIRED("system.client_sync_required"),

    /**
     * Client explicitly requests full data refresh.
     * Infrastructure event.
     * Payload: clientId, userId, reason
     */
    CLIENT_FULL_SYNC_REQUESTED("system.client_full_sync_requested"),

    /**
     * Client requests delta sync (incremental update).
     * Infrastructure event.
     * Payload: clientId, userId, since (lastUpdateTimestamp)
     */
    CLIENT_DELTA_SYNC_REQUESTED("system.client_delta_sync_requested"),

    /**
     * Client sync operation has completed.
     * Infrastructure event.
     * Payload: clientId, userId, syncedEntityCount, newLastUpdateTimestamp
     */
    CLIENT_SYNC_COMPLETED("system.client_sync_completed");


    private final String eventName;

    EventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }

    /**
     * Helper: Is this a reservation-related event?
     */
    public boolean isReservationEvent() {
        return this.eventName.startsWith("reservation.");
    }

    /**
     * Helper: Is this a system/infrastructure event?
     */
    public boolean isSystemEvent() {
        return this.eventName.startsWith("system.");
    }

    /**
     * Helper: Is this a messaging event?
     */
    public boolean isMessagingEvent() {
        return this.eventName.startsWith("reservation.message_") 
            || this.eventName.startsWith("reservation.conversation_");
    }

    /**
     * Helper: Is this a support event?
     */
    public boolean isSupportEvent() {
        return this.eventName.startsWith("support.");
    }

    /**
     * Helper: Is this a status change event?
     */
    public boolean isStatusChangeEvent() {
        return this == RESERVATION_STATUS_CHANGED;
    }
}
