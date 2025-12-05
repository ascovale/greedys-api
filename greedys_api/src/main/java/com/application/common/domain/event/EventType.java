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
     * ⭐ Customer has REQUESTED a new reservation (pending restaurant approval).
     * Status: NOT_ACCEPTED (requires confirmation from restaurant staff)
     * Initiated by: CUSTOMER
     * Notifies: RESTAURANT STAFF (team notification)
     * 
     * Payload: reservationId, customerId, restaurantId, email, datetime, pax, kids, notes, initiated_by=CUSTOMER
     */
    RESERVATION_REQUESTED("reservation.requested"),
    
    /**
     * ⭐ Restaurant staff has CREATED a reservation (already confirmed).
     * Status: ACCEPTED (implicitly approved since created by staff)
     * Initiated by: RESTAURANT_USER
     * Notifies: CUSTOMER ONLY (personal confirmation)
     * 
     * Payload: reservationId, customerId, restaurantId, email, date, pax, kids, notes, initiated_by=RESTAURANT
     */
    RESERVATION_CREATED("reservation.created"),
    
    /**
     * ⭐ Admin/Restaurant user created reservation on behalf of customer.
     * Status: ACCEPTED (implicitly approved)
     * Initiated by: ADMIN / RESTAURANT_USER
     * Notifies: CUSTOMER (personal confirmation)
     * 
     * Payload: reservationId, customerId, restaurantId, email, datetime, pax, kids, notes, initiated_by=ADMIN
     */
    CUSTOMER_RESERVATION_CREATED("reservation.customer_created"),

    /**
     * Covers all modifications to reservation details (generic).
     * Payload: reservationId, changedFields
     */
    RESERVATION_MODIFIED("reservation.modified"),
    
    // ─────── RESERVATION MODIFICATION REQUEST EVENTS ───────
    
    /**
     * ⭐ Customer has requested a modification to their reservation.
     * Status: Pending approval from restaurant staff
     * Initiated by: CUSTOMER
     * Notifies: RESTAURANT STAFF (team notification)
     * 
     * Payload: modificationRequestId, reservationId, customerId, restaurantId, 
     *          requestedChanges (datetime, pax, etc.), originalValues
     */
    RESERVATION_MODIFICATION_REQUESTED("reservation.modification_requested"),
    
    /**
     * ⭐ Restaurant has approved a modification request.
     * Status: Changes applied to reservation
     * Initiated by: RESTAURANT_USER
     * Notifies: CUSTOMER (personal confirmation)
     * 
     * Payload: modificationRequestId, reservationId, customerId, appliedChanges
     */
    RESERVATION_MODIFICATION_APPROVED("reservation.modification_approved"),
    
    /**
     * ⭐ Restaurant has rejected a modification request.
     * Status: Request rejected, original reservation unchanged
     * Initiated by: RESTAURANT_USER
     * Notifies: CUSTOMER (personal notification)
     * 
     * Payload: modificationRequestId, reservationId, customerId, rejectionReason
     */
    RESERVATION_MODIFICATION_REJECTED("reservation.modification_rejected"),
    
    /**
     * ⭐ Restaurant has directly modified a customer's reservation.
     * Status: Changes applied without request
     * Initiated by: RESTAURANT_USER
     * Notifies: CUSTOMER (personal notification)
     * 
     * Payload: reservationId, customerId, changedFields, oldValues, newValues
     */
    RESERVATION_MODIFIED_BY_RESTAURANT("reservation.modified_by_restaurant"),

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
    CLIENT_SYNC_COMPLETED("system.client_sync_completed"),

    // ─────── CHAT EVENTS ───────
    
    /**
     * Direct chat message received.
     * Payload: conversationId, messageId, senderId, senderName, content, messageType
     */
    CHAT_MESSAGE_RECEIVED("chat.message_received"),
    
    /**
     * Group chat message received.
     * Payload: conversationId, groupName, messageId, senderId, senderName, content
     */
    CHAT_GROUP_MESSAGE("chat.group_message"),
    
    /**
     * Reservation chat message.
     * Payload: conversationId, reservationId, messageId, senderId, content
     */
    CHAT_RESERVATION_MESSAGE("chat.reservation_message"),
    
    /**
     * Typing indicator in a conversation.
     * Payload: conversationId, userId, userName, isTyping
     */
    CHAT_TYPING_INDICATOR("chat.typing"),
    
    /**
     * User joined a group conversation.
     * Payload: conversationId, userId, userName, role
     */
    CHAT_USER_JOINED("chat.user_joined"),
    
    /**
     * User left a group conversation.
     * Payload: conversationId, userId, userName
     */
    CHAT_USER_LEFT("chat.user_left"),
    
    /**
     * Messages marked as read.
     * Payload: conversationId, userId, lastReadMessageId
     */
    CHAT_MESSAGES_READ("chat.messages_read"),
    
    // ─────── SUPPORT TICKET EVENTS ───────
    
    /**
     * New support ticket created.
     * Payload: ticketId, ticketNumber, requesterId, requesterType, subject, category, priority
     */
    SUPPORT_TICKET_CREATED("support.ticket_created"),
    
    /**
     * Support ticket status changed.
     * Payload: ticketId, oldStatus, newStatus, changedById
     */
    SUPPORT_TICKET_STATUS_CHANGED("support.ticket_status_changed"),
    
    /**
     * Message sent in support ticket.
     * Payload: ticketId, messageId, senderId, content, isFromBot, isFromStaff
     */
    SUPPORT_TICKET_MESSAGE("support.ticket_message"),
    
    /**
     * Bot response in support chat.
     * Payload: ticketId, messageId, faqId, suggestedAnswer
     */
    SUPPORT_BOT_RESPONSE("support.bot_response"),
    
    /**
     * Support ticket resolved.
     * Payload: ticketId, resolution, resolvedById, satisfactionScore
     */
    SUPPORT_TICKET_RESOLVED("support.ticket_resolved"),
    
    // ─────── SOCIAL EVENTS ───────
    
    /**
     * New post in feed (from followed user/restaurant).
     * Payload: postId, authorId, authorType, authorName, content, mediaCount
     */
    SOCIAL_NEW_POST("social.new_post"),
    
    /**
     * Someone liked your post.
     * Payload: postId, likerId, likerName, likerType, reactionType
     */
    SOCIAL_POST_LIKED("social.post_liked"),
    
    /**
     * Someone commented on your post.
     * Payload: postId, commentId, authorId, authorName, content
     */
    SOCIAL_POST_COMMENTED("social.post_commented"),
    
    /**
     * Someone shared your post.
     * Payload: originalPostId, sharedPostId, sharerId, sharerName
     */
    SOCIAL_POST_SHARED("social.post_shared"),
    
    /**
     * Someone mentioned you in a post/comment.
     * Payload: postId, commentId, mentionerId, mentionerName, context
     */
    SOCIAL_USER_MENTIONED("social.user_mentioned"),
    
    /**
     * New follower.
     * Payload: followerId, followerName, followerType
     */
    SOCIAL_NEW_FOLLOWER("social.new_follower"),
    
    /**
     * Follow request (for private profiles).
     * Payload: followerId, followerName, followerType
     */
    SOCIAL_FOLLOW_REQUEST("social.follow_request"),
    
    /**
     * Follow request accepted.
     * Payload: followingId, followingName, followingType
     */
    SOCIAL_FOLLOW_ACCEPTED("social.follow_accepted"),
    
    /**
     * New story from followed user/restaurant.
     * Payload: storyId, authorId, authorType, authorName, expiresAt
     */
    SOCIAL_NEW_STORY("social.new_story"),
    
    /**
     * Someone replied to your story.
     * Payload: storyId, replierId, replierName, content
     */
    SOCIAL_STORY_REPLY("social.story_reply"),
    
    // ─────── RESTAURANT EVENT EVENTS ───────
    
    /**
     * New event created by followed restaurant.
     * Payload: eventId, restaurantId, restaurantName, title, eventDate, eventType
     */
    EVENT_CREATED("event.created"),
    
    /**
     * Event you're interested in is starting soon.
     * Payload: eventId, title, restaurantName, startTime, reminderType
     */
    EVENT_REMINDER("event.reminder"),
    
    /**
     * Event you RSVP'd to has been updated.
     * Payload: eventId, changedFields, oldValues, newValues
     */
    EVENT_UPDATED("event.updated"),
    
    /**
     * Event has been cancelled.
     * Payload: eventId, title, cancellationReason, refundInfo
     */
    EVENT_CANCELLED("event.cancelled"),
    
    /**
     * Your RSVP status changed (e.g., moved from waitlist).
     * Payload: eventId, rsvpId, oldStatus, newStatus
     */
    EVENT_RSVP_STATUS_CHANGED("event.rsvp_status_changed"),
    
    /**
     * New RSVP for your event (restaurant notification).
     * Payload: eventId, rsvpId, userId, userName, guestsCount, status
     */
    EVENT_NEW_RSVP("event.new_rsvp"),

    // ─────── CHALLENGE EVENTS ───────
    
    /**
     * New challenge created (available for registration).
     * Payload: challengeId, name, challengeType, city, startDate, endDate
     */
    CHALLENGE_CREATED("challenge.created"),
    
    /**
     * Challenge status changed (registration open, active, voting, etc.).
     * Payload: challengeId, oldStatus, newStatus, name
     */
    CHALLENGE_STATUS_CHANGED("challenge.status_changed"),
    
    /**
     * Registration period opened for a challenge.
     * Payload: challengeId, name, registrationEndDate, maxParticipants
     */
    CHALLENGE_REGISTRATION_OPENED("challenge.registration_opened"),
    
    /**
     * Registration period closing soon (reminder).
     * Payload: challengeId, name, registrationEndDate, hoursRemaining
     */
    CHALLENGE_REGISTRATION_CLOSING("challenge.registration_closing"),
    
    /**
     * Challenge has started (active competition).
     * Payload: challengeId, name, participantsCount, endDate
     */
    CHALLENGE_STARTED("challenge.started"),
    
    /**
     * Voting phase opened for a challenge.
     * Payload: challengeId, name, votingEndDate, finalistsCount
     */
    CHALLENGE_VOTING_OPENED("challenge.voting_opened"),
    
    /**
     * Challenge completed, results available.
     * Payload: challengeId, name, winnerId, winnerName, podium
     */
    CHALLENGE_COMPLETED("challenge.completed"),
    
    /**
     * Challenge cancelled.
     * Payload: challengeId, name, reason
     */
    CHALLENGE_CANCELLED("challenge.cancelled"),
    
    /**
     * Restaurant registered to a challenge.
     * Payload: challengeId, participationId, restaurantId, restaurantName
     */
    CHALLENGE_RESTAURANT_REGISTERED("challenge.restaurant_registered"),
    
    /**
     * Restaurant qualified (passed preliminary phase).
     * Payload: challengeId, participationId, restaurantId, qualificationRank
     */
    CHALLENGE_RESTAURANT_QUALIFIED("challenge.restaurant_qualified"),
    
    /**
     * Restaurant eliminated from challenge.
     * Payload: challengeId, participationId, restaurantId, phase, reason
     */
    CHALLENGE_RESTAURANT_ELIMINATED("challenge.restaurant_eliminated"),
    
    /**
     * Restaurant won a challenge.
     * Payload: challengeId, participationId, restaurantId, restaurantName, prize
     */
    CHALLENGE_RESTAURANT_WON("challenge.restaurant_won"),
    
    // ─────── TOURNAMENT EVENTS ───────
    
    /**
     * New tournament created.
     * Payload: tournamentId, name, challengeType, city, startDate, totalPrize
     */
    TOURNAMENT_CREATED("tournament.created"),
    
    /**
     * Tournament status changed.
     * Payload: tournamentId, oldStatus, newStatus, name
     */
    TOURNAMENT_STATUS_CHANGED("tournament.status_changed"),
    
    /**
     * Tournament registration opened.
     * Payload: tournamentId, name, registrationEnd, maxParticipants
     */
    TOURNAMENT_REGISTRATION_OPENED("tournament.registration_opened"),
    
    /**
     * Tournament qualification phase started.
     * Payload: tournamentId, name, qualificationEndDate
     */
    TOURNAMENT_QUALIFICATION_STARTED("tournament.qualification_started"),
    
    /**
     * Tournament phase changed (group stage, quarter-finals, etc.).
     * Payload: tournamentId, name, oldPhase, newPhase
     */
    TOURNAMENT_PHASE_CHANGED("tournament.phase_changed"),
    
    /**
     * Tournament bracket/draw published.
     * Payload: tournamentId, name, groupsCount, bracketJson
     */
    TOURNAMENT_BRACKET_PUBLISHED("tournament.bracket_published"),
    
    /**
     * Tournament completed, final results.
     * Payload: tournamentId, name, winnerId, winnerName, finalScore
     */
    TOURNAMENT_COMPLETED("tournament.completed"),
    
    // ─────── TOURNAMENT MATCH EVENTS ───────
    
    /**
     * New match scheduled.
     * Payload: matchId, tournamentId, phase, restaurant1Id, restaurant2Id, votingStartsAt
     */
    MATCH_SCHEDULED("match.scheduled"),
    
    /**
     * Match voting opened.
     * Payload: matchId, tournamentId, restaurant1Name, restaurant2Name, votingEndsAt
     */
    MATCH_VOTING_OPENED("match.voting_opened"),
    
    /**
     * Match voting closing soon (reminder).
     * Payload: matchId, tournamentId, minutesRemaining, currentVotes
     */
    MATCH_VOTING_CLOSING("match.voting_closing"),
    
    /**
     * Match completed, result announced.
     * Payload: matchId, tournamentId, winnerId, winnerName, votes1, votes2, isDraw
     */
    MATCH_COMPLETED("match.completed"),
    
    /**
     * User voted in a match.
     * Payload: matchId, voteId, customerId, votedRestaurantId, voterType, isVerified
     */
    MATCH_VOTE_CAST("match.vote_cast"),
    
    // ─────── RANKING EVENTS ───────
    
    /**
     * New ranking published.
     * Payload: rankingId, scope, period, city, challengeType, entriesCount
     */
    RANKING_PUBLISHED("ranking.published"),
    
    /**
     * Ranking updated (positions changed).
     * Payload: rankingId, scope, period, updateTimestamp
     */
    RANKING_UPDATED("ranking.updated"),
    
    /**
     * Restaurant entered a ranking.
     * Payload: rankingId, entryId, restaurantId, position, score
     */
    RANKING_RESTAURANT_ENTERED("ranking.restaurant_entered"),
    
    /**
     * Restaurant position changed in ranking.
     * Payload: rankingId, entryId, restaurantId, oldPosition, newPosition, positionChange
     */
    RANKING_POSITION_CHANGED("ranking.position_changed"),
    
    /**
     * Restaurant reached podium (top 3) in ranking.
     * Payload: rankingId, entryId, restaurantId, position, scope, period
     */
    RANKING_PODIUM_ACHIEVED("ranking.podium_achieved"),
    
    /**
     * User voted in a ranking.
     * Payload: rankingId, voteId, customerId, restaurantId, score, voterType, isVerified
     */
    RANKING_VOTE_CAST("ranking.vote_cast"),
    
    /**
     * Ranking period ended.
     * Payload: rankingId, scope, period, winnerId, winnerName, finalScore
     */
    RANKING_PERIOD_ENDED("ranking.period_ended"),
    
    // ─────── CHALLENGE SOCIAL EVENTS ───────
    
    /**
     * New story posted for a challenge.
     * Payload: storyId, challengeId, authorId, authorType, authorName, expiresAt
     */
    CHALLENGE_STORY_POSTED("challenge.story_posted"),
    
    /**
     * New reel posted for a challenge.
     * Payload: reelId, challengeId, authorId, authorType, authorName, title
     */
    CHALLENGE_REEL_POSTED("challenge.reel_posted"),
    
    /**
     * Challenge content liked.
     * Payload: contentId, contentType (story/reel), challengeId, likerId
     */
    CHALLENGE_CONTENT_LIKED("challenge.content_liked"),
    
    /**
     * Challenge content shared.
     * Payload: contentId, contentType, challengeId, sharerId
     */
    CHALLENGE_CONTENT_SHARED("challenge.content_shared"),
    
    /**
     * Challenge reel won (featured/winner).
     * Payload: reelId, challengeId, authorId, authorName, prize
     */
    CHALLENGE_REEL_WON("challenge.reel_won");


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
     * Helper: Is this a chat event?
     */
    public boolean isChatEvent() {
        return this.eventName.startsWith("chat.");
    }

    /**
     * Helper: Is this a social event?
     */
    public boolean isSocialEvent() {
        return this.eventName.startsWith("social.");
    }

    /**
     * Helper: Is this a restaurant event event?
     */
    public boolean isEventEvent() {
        return this.eventName.startsWith("event.");
    }

    /**
     * Helper: Is this a challenge event?
     */
    public boolean isChallengeEvent() {
        return this.eventName.startsWith("challenge.");
    }

    /**
     * Helper: Is this a tournament event?
     */
    public boolean isTournamentEvent() {
        return this.eventName.startsWith("tournament.");
    }

    /**
     * Helper: Is this a match event?
     */
    public boolean isMatchEvent() {
        return this.eventName.startsWith("match.");
    }

    /**
     * Helper: Is this a ranking event?
     */
    public boolean isRankingEvent() {
        return this.eventName.startsWith("ranking.");
    }

    /**
     * Helper: Is this any gamification event (challenge, tournament, ranking)?
     */
    public boolean isGamificationEvent() {
        return isChallengeEvent() || isTournamentEvent() || isMatchEvent() || isRankingEvent();
    }

    /**
     * Helper: Is this a status change event?
     */
    public boolean isStatusChangeEvent() {
        return this == RESERVATION_STATUS_CHANGED;
    }
}
