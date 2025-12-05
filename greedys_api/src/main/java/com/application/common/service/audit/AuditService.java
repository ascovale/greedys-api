package com.application.common.service.audit;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.dao.audit.ChallengeAuditLogDAO;
import com.application.common.persistence.dao.audit.ReservationAuditLogDAO;
import com.application.common.persistence.dao.audit.ScheduleAuditLogDAO;
import com.application.common.persistence.model.audit.ChallengeAuditLog;
import com.application.common.persistence.model.audit.ReservationAuditLog;
import com.application.common.persistence.model.audit.ReservationAuditLog.ReservationAuditAction;
import com.application.common.persistence.model.audit.ReservationAuditLog.UserType;
import com.application.common.persistence.model.audit.ScheduleAuditLog;
import com.application.common.persistence.model.audit.ScheduleAuditLog.AuditAction;
import com.application.common.persistence.model.audit.ScheduleAuditLog.EntityType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for creating audit log entries.
 * 
 * Provides a unified API for auditing:
 * - Schedule changes (slot config, day schedules, exceptions)
 * - Reservation changes (creation, status, data, terms)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuditService {

    private final ScheduleAuditLogDAO scheduleAuditLogDAO;
    private final ReservationAuditLogDAO reservationAuditLogDAO;
    private final ChallengeAuditLogDAO challengeAuditLogDAO;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────────
    // SCHEDULE AUDIT METHODS
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Audit a schedule entity creation
     */
    public ScheduleAuditLog auditScheduleCreated(
            EntityType entityType,
            Long entityId,
            Long serviceId,
            Long restaurantId,
            Long userId,
            Object newEntity,
            String reason) {
        
        return createScheduleAuditLog(
            entityType, entityId, serviceId, restaurantId,
            AuditAction.CREATED, userId,
            null, toJson(newEntity), reason
        );
    }

    /**
     * Audit a schedule entity update
     */
    public ScheduleAuditLog auditScheduleUpdated(
            EntityType entityType,
            Long entityId,
            Long serviceId,
            Long restaurantId,
            Long userId,
            Object oldEntity,
            Object newEntity,
            String reason) {
        
        return createScheduleAuditLog(
            entityType, entityId, serviceId, restaurantId,
            AuditAction.UPDATED, userId,
            toJson(oldEntity), toJson(newEntity), reason
        );
    }

    /**
     * Audit a schedule entity deletion
     */
    public ScheduleAuditLog auditScheduleDeleted(
            EntityType entityType,
            Long entityId,
            Long serviceId,
            Long restaurantId,
            Long userId,
            Object deletedEntity,
            String reason) {
        
        return createScheduleAuditLog(
            entityType, entityId, serviceId, restaurantId,
            AuditAction.DELETED, userId,
            toJson(deletedEntity), null, reason
        );
    }

    /**
     * Audit schedule activation
     */
    public ScheduleAuditLog auditScheduleActivated(
            Long serviceId,
            Long restaurantId,
            Long userId,
            String reason) {
        
        return createScheduleAuditLog(
            EntityType.SERVICE, serviceId, serviceId, restaurantId,
            AuditAction.ACTIVATED, userId,
            null, null, reason
        );
    }

    /**
     * Audit schedule deactivation
     */
    public ScheduleAuditLog auditScheduleDeactivated(
            Long serviceId,
            Long restaurantId,
            Long userId,
            String reason) {
        
        return createScheduleAuditLog(
            EntityType.SERVICE, serviceId, serviceId, restaurantId,
            AuditAction.DEACTIVATED, userId,
            null, null, reason
        );
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // SERVICE ENTITY AUDIT METHODS (for Service CRUD operations)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Audit service creation
     */
    public ScheduleAuditLog auditServiceCreated(
            Long serviceId,
            Long restaurantId,
            Long userId,
            Object serviceData,
            String reason) {
        
        return createScheduleAuditLog(
            EntityType.SERVICE, serviceId, serviceId, restaurantId,
            AuditAction.CREATED, userId,
            null, toJson(serviceData), reason
        );
    }

    /**
     * Audit service update
     */
    public ScheduleAuditLog auditServiceUpdated(
            Long serviceId,
            Long restaurantId,
            Long userId,
            Object oldService,
            Object newService,
            String reason) {
        
        return createScheduleAuditLog(
            EntityType.SERVICE, serviceId, serviceId, restaurantId,
            AuditAction.UPDATED, userId,
            toJson(oldService), toJson(newService), reason
        );
    }

    /**
     * Audit service deletion
     */
    public ScheduleAuditLog auditServiceDeleted(
            Long serviceId,
            Long restaurantId,
            Long userId,
            Object deletedService,
            String reason) {
        
        return createScheduleAuditLog(
            EntityType.SERVICE, serviceId, serviceId, restaurantId,
            AuditAction.DELETED, userId,
            toJson(deletedService), null, reason
        );
    }

    private ScheduleAuditLog createScheduleAuditLog(
            EntityType entityType,
            Long entityId,
            Long serviceId,
            Long restaurantId,
            AuditAction action,
            Long userId,
            String oldValue,
            String newValue,
            String reason) {
        
        ScheduleAuditLog auditLog = ScheduleAuditLog.builder()
            .entityType(entityType)
            .entityId(entityId)
            .serviceId(serviceId)
            .restaurantId(restaurantId)
            .action(action)
            .userId(userId)
            .oldValue(oldValue)
            .newValue(newValue)
            .changeReason(reason)
            .changedAt(LocalDateTime.now())
            .build();

        ScheduleAuditLog saved = scheduleAuditLogDAO.save(auditLog);
        log.debug("Created schedule audit log: {}", saved);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // RESERVATION AUDIT METHODS
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Audit reservation creation
     */
    public ReservationAuditLog auditReservationCreated(
            Long reservationId,
            Long restaurantId,
            Long userId,
            UserType userType,
            Object reservationData) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, null,
            ReservationAuditAction.CREATED, userId, userType,
            null, null, toJson(reservationData), null,
            null, null
        );
    }

    /**
     * Audit reservation status change
     */
    public ReservationAuditLog auditReservationStatusChanged(
            Long reservationId,
            Long restaurantId,
            Long userId,
            UserType userType,
            String oldStatus,
            String newStatus,
            String reason) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, null,
            ReservationAuditAction.STATUS_CHANGED, userId, userType,
            "status", oldStatus, newStatus, reason,
            null, null
        );
    }

    /**
     * Audit reservation field update
     */
    public ReservationAuditLog auditReservationUpdated(
            Long reservationId,
            Long restaurantId,
            Long userId,
            UserType userType,
            String fieldChanged,
            String oldValue,
            String newValue,
            String reason) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, null,
            ReservationAuditAction.UPDATED, userId, userType,
            fieldChanged, oldValue, newValue, reason,
            null, null
        );
    }

    /**
     * Audit reservation terms change (snapshot updated)
     */
    public ReservationAuditLog auditReservationTermsChanged(
            Long reservationId,
            Long restaurantId,
            Long userId,
            UserType userType,
            String fieldChanged,
            String oldValue,
            String newValue,
            String reason,
            Boolean customerNotified,
            Boolean customerAccepted) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, null,
            ReservationAuditAction.TERMS_CHANGED, userId, userType,
            fieldChanged, oldValue, newValue, reason,
            customerNotified, customerAccepted
        );
    }

    /**
     * Audit reservation cancellation
     */
    public ReservationAuditLog auditReservationCancelled(
            Long reservationId,
            Long restaurantId,
            Long userId,
            UserType userType,
            String reason) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, null,
            ReservationAuditAction.CANCELLED, userId, userType,
            null, null, null, reason,
            null, null
        );
    }

    /**
     * Audit table assignment
     */
    public ReservationAuditLog auditTableAssigned(
            Long reservationId,
            Long restaurantId,
            Long userId,
            UserType userType,
            String oldTableInfo,
            String newTableInfo) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, null,
            ReservationAuditAction.TABLE_ASSIGNED, userId, userType,
            "table", oldTableInfo, newTableInfo, null,
            null, null
        );
    }

    /**
     * Audit customer seated
     */
    public ReservationAuditLog auditCustomerSeated(
            Long reservationId,
            Long restaurantId,
            Long userId,
            UserType userType) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, null,
            ReservationAuditAction.SEATED, userId, userType,
            null, null, null, null,
            null, null
        );
    }

    /**
     * Audit no-show
     */
    public ReservationAuditLog auditNoShow(
            Long reservationId,
            Long restaurantId,
            Long userId,
            UserType userType) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, null,
            ReservationAuditAction.NO_SHOW, userId, userType,
            null, null, null, null,
            null, null
        );
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // GROUP BOOKING AUDIT METHODS
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Audit group inquiry received
     */
    public ReservationAuditLog auditGroupInquiryReceived(
            Long reservationId,
            Long restaurantId,
            Long groupBookingId,
            Long userId,
            UserType userType,
            Object inquiryData) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, groupBookingId,
            ReservationAuditAction.GROUP_INQUIRY_RECEIVED, userId, userType,
            null, null, toJson(inquiryData), null,
            null, null
        );
    }

    /**
     * Audit group quote sent
     */
    public ReservationAuditLog auditGroupQuoteSent(
            Long reservationId,
            Long restaurantId,
            Long groupBookingId,
            Long userId,
            UserType userType,
            Object quoteData) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, groupBookingId,
            ReservationAuditAction.GROUP_QUOTE_SENT, userId, userType,
            null, null, toJson(quoteData), null,
            null, null
        );
    }

    /**
     * Audit group booking confirmed
     */
    public ReservationAuditLog auditGroupConfirmed(
            Long reservationId,
            Long restaurantId,
            Long groupBookingId,
            Long userId,
            UserType userType) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, groupBookingId,
            ReservationAuditAction.GROUP_CONFIRMED, userId, userType,
            null, null, null, null,
            null, null
        );
    }

    /**
     * Audit group deposit paid
     */
    public ReservationAuditLog auditGroupDepositPaid(
            Long reservationId,
            Long restaurantId,
            Long groupBookingId,
            Long userId,
            UserType userType,
            String depositAmount) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, groupBookingId,
            ReservationAuditAction.GROUP_DEPOSIT_PAID, userId, userType,
            "depositAmount", null, depositAmount, null,
            null, null
        );
    }

    /**
     * Audit group fully paid
     */
    public ReservationAuditLog auditGroupFullyPaid(
            Long reservationId,
            Long restaurantId,
            Long groupBookingId,
            Long userId,
            UserType userType,
            String totalAmount) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, groupBookingId,
            ReservationAuditAction.GROUP_FULLY_PAID, userId, userType,
            "totalAmount", null, totalAmount, null,
            null, null
        );
    }

    /**
     * Audit group booking linked to reservation
     */
    public ReservationAuditLog auditGroupLinkedToReservation(
            Long reservationId,
            Long restaurantId,
            Long groupBookingId,
            Long userId,
            UserType userType) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, groupBookingId,
            ReservationAuditAction.GROUP_LINKED_TO_RESERVATION, userId, userType,
            null, null, null, "Group booking linked to reservation",
            null, null
        );
    }

    /**
     * Audit group booking details updated
     */
    public ReservationAuditLog auditGroupDetailsUpdated(
            Long reservationId,
            Long restaurantId,
            Long groupBookingId,
            Long userId,
            UserType userType,
            String fieldChanged,
            String oldValue,
            String newValue) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, groupBookingId,
            ReservationAuditAction.GROUP_DETAILS_UPDATED, userId, userType,
            fieldChanged, oldValue, newValue, null,
            null, null
        );
    }

    /**
     * Audit group menu selected
     */
    public ReservationAuditLog auditGroupMenuSelected(
            Long reservationId,
            Long restaurantId,
            Long groupBookingId,
            Long userId,
            UserType userType,
            String oldMenuId,
            String newMenuId) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, groupBookingId,
            ReservationAuditAction.GROUP_MENU_SELECTED, userId, userType,
            "fixedPriceMenuId", oldMenuId, newMenuId, null,
            null, null
        );
    }

    /**
     * Audit group dietary needs updated
     */
    public ReservationAuditLog auditGroupDietaryUpdated(
            Long reservationId,
            Long restaurantId,
            Long groupBookingId,
            Long userId,
            UserType userType,
            Object oldDietary,
            Object newDietary) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, groupBookingId,
            ReservationAuditAction.GROUP_DIETARY_UPDATED, userId, userType,
            "dietaryNeeds", toJson(oldDietary), toJson(newDietary), null,
            null, null
        );
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // MODIFICATION REQUEST AUDIT METHODS
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Audit modification requested by customer
     */
    public ReservationAuditLog auditModificationRequested(
            Long reservationId,
            Long restaurantId,
            Long userId,
            UserType userType,
            Object requestedChanges) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, null,
            ReservationAuditAction.MODIFICATION_REQUESTED, userId, userType,
            null, null, toJson(requestedChanges), null,
            null, null
        );
    }

    /**
     * Audit modification approved
     */
    public ReservationAuditLog auditModificationApproved(
            Long reservationId,
            Long restaurantId,
            Long userId,
            UserType userType,
            Object appliedChanges) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, null,
            ReservationAuditAction.MODIFICATION_APPROVED, userId, userType,
            null, null, toJson(appliedChanges), null,
            null, null
        );
    }

    /**
     * Audit modification rejected
     */
    public ReservationAuditLog auditModificationRejected(
            Long reservationId,
            Long restaurantId,
            Long userId,
            UserType userType,
            String rejectionReason) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, null,
            ReservationAuditAction.MODIFICATION_REJECTED, userId, userType,
            null, null, null, rejectionReason,
            null, null
        );
    }

    /**
     * Audit restaurant directly modified reservation
     */
    public ReservationAuditLog auditModifiedByRestaurant(
            Long reservationId,
            Long restaurantId,
            Long userId,
            UserType userType,
            Object changes) {
        
        return createReservationAuditLog(
            reservationId, restaurantId, null,
            ReservationAuditAction.MODIFIED_BY_RESTAURANT, userId, userType,
            null, null, toJson(changes), null,
            null, null
        );
    }

    private ReservationAuditLog createReservationAuditLog(
            Long reservationId,
            Long restaurantId,
            Long groupBookingId,
            ReservationAuditAction action,
            Long userId,
            UserType userType,
            String fieldChanged,
            String oldValue,
            String newValue,
            String reason,
            Boolean customerNotified,
            Boolean customerAccepted) {
        
        ReservationAuditLog auditLog = ReservationAuditLog.builder()
            .reservationId(reservationId)
            .restaurantId(restaurantId)
            .groupBookingId(groupBookingId)
            .action(action)
            .userId(userId)
            .userType(userType)
            .fieldChanged(fieldChanged)
            .oldValue(oldValue)
            .newValue(newValue)
            .changeReason(reason)
            .customerNotified(customerNotified)
            .customerAccepted(customerAccepted)
            .changedAt(LocalDateTime.now())
            .build();

        ReservationAuditLog saved = reservationAuditLogDAO.save(auditLog);
        log.debug("Created reservation audit log: {}", saved);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // CHALLENGE AUDIT METHODS
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Audit challenge created
     */
    public ChallengeAuditLog auditChallengeCreated(
            Long challengeId,
            Long userId,
            ChallengeAuditLog.UserType userType,
            Object challengeData) {
        
        return createChallengeAuditLog(
            ChallengeAuditLog.ChallengeEntityType.CHALLENGE, challengeId,
            challengeId, null, null, null, null, null,
            ChallengeAuditLog.ChallengeAuditAction.CHALLENGE_CREATED,
            userId, userType, null, null, toJson(challengeData), null, null, null
        );
    }

    /**
     * Audit challenge status changed
     */
    public ChallengeAuditLog auditChallengeStatusChanged(
            Long challengeId,
            Long userId,
            ChallengeAuditLog.UserType userType,
            String oldStatus,
            String newStatus) {
        
        return createChallengeAuditLog(
            ChallengeAuditLog.ChallengeEntityType.CHALLENGE, challengeId,
            challengeId, null, null, null, null, null,
            ChallengeAuditLog.ChallengeAuditAction.CHALLENGE_STATUS_CHANGED,
            userId, userType, "status", oldStatus, newStatus, null, null, null
        );
    }

    /**
     * Audit tournament created
     */
    public ChallengeAuditLog auditTournamentCreated(
            Long tournamentId,
            Long userId,
            ChallengeAuditLog.UserType userType,
            Object tournamentData) {
        
        return createChallengeAuditLog(
            ChallengeAuditLog.ChallengeEntityType.TOURNAMENT, tournamentId,
            null, tournamentId, null, null, null, null,
            ChallengeAuditLog.ChallengeAuditAction.TOURNAMENT_CREATED,
            userId, userType, null, null, toJson(tournamentData), null, null, null
        );
    }

    /**
     * Audit tournament phase changed
     */
    public ChallengeAuditLog auditTournamentPhaseChanged(
            Long tournamentId,
            Long userId,
            ChallengeAuditLog.UserType userType,
            String oldPhase,
            String newPhase) {
        
        return createChallengeAuditLog(
            ChallengeAuditLog.ChallengeEntityType.TOURNAMENT, tournamentId,
            null, tournamentId, null, null, null, null,
            ChallengeAuditLog.ChallengeAuditAction.TOURNAMENT_PHASE_CHANGED,
            userId, userType, "phase", oldPhase, newPhase, null, null, null
        );
    }

    /**
     * Audit match vote cast
     */
    public ChallengeAuditLog auditMatchVoteCast(
            Long voteId,
            Long matchId,
            Long tournamentId,
            Long restaurantId,
            Long customerId,
            String metadata,
            String ipAddress) {
        
        return createChallengeAuditLog(
            ChallengeAuditLog.ChallengeEntityType.MATCH_VOTE, voteId,
            null, tournamentId, matchId, null, restaurantId, customerId,
            ChallengeAuditLog.ChallengeAuditAction.VOTE_CAST,
            customerId, ChallengeAuditLog.UserType.CUSTOMER, null, null, null, metadata, null, ipAddress
        );
    }

    /**
     * Audit ranking vote cast
     */
    public ChallengeAuditLog auditRankingVoteCast(
            Long voteId,
            Long rankingId,
            Long restaurantId,
            Long customerId,
            String metadata,
            String ipAddress) {
        
        return createChallengeAuditLog(
            ChallengeAuditLog.ChallengeEntityType.RANKING_VOTE, voteId,
            null, null, null, rankingId, restaurantId, customerId,
            ChallengeAuditLog.ChallengeAuditAction.VOTE_CAST,
            customerId, ChallengeAuditLog.UserType.CUSTOMER, null, null, null, metadata, null, ipAddress
        );
    }

    /**
     * Audit restaurant registered to challenge/tournament
     */
    public ChallengeAuditLog auditRestaurantRegistered(
            Long participationId,
            Long challengeId,
            Long tournamentId,
            Long restaurantId,
            Long userId,
            ChallengeAuditLog.UserType userType) {
        
        return createChallengeAuditLog(
            ChallengeAuditLog.ChallengeEntityType.PARTICIPATION, participationId,
            challengeId, tournamentId, null, null, restaurantId, null,
            ChallengeAuditLog.ChallengeAuditAction.RESTAURANT_REGISTERED,
            userId, userType, null, null, null, null, null, null
        );
    }

    /**
     * Audit restaurant qualified
     */
    public ChallengeAuditLog auditRestaurantQualified(
            Long participationId,
            Long tournamentId,
            Long restaurantId,
            String metadata) {
        
        return createChallengeAuditLog(
            ChallengeAuditLog.ChallengeEntityType.PARTICIPATION, participationId,
            null, tournamentId, null, null, restaurantId, null,
            ChallengeAuditLog.ChallengeAuditAction.RESTAURANT_QUALIFIED,
            null, ChallengeAuditLog.UserType.SYSTEM, null, null, null, metadata, null, null
        );
    }

    /**
     * Audit restaurant eliminated
     */
    public ChallengeAuditLog auditRestaurantEliminated(
            Long participationId,
            Long tournamentId,
            Long matchId,
            Long restaurantId,
            String reason) {
        
        return createChallengeAuditLog(
            ChallengeAuditLog.ChallengeEntityType.PARTICIPATION, participationId,
            null, tournamentId, matchId, null, restaurantId, null,
            ChallengeAuditLog.ChallengeAuditAction.RESTAURANT_ELIMINATED,
            null, ChallengeAuditLog.UserType.SYSTEM, null, null, null, null, reason, null
        );
    }

    /**
     * Audit ranking position changed
     */
    public ChallengeAuditLog auditRankingPositionChanged(
            Long entryId,
            Long rankingId,
            Long restaurantId,
            String oldPosition,
            String newPosition) {
        
        return createChallengeAuditLog(
            ChallengeAuditLog.ChallengeEntityType.RANKING_ENTRY, entryId,
            null, null, null, rankingId, restaurantId, null,
            ChallengeAuditLog.ChallengeAuditAction.RANKING_POSITION_CHANGED,
            null, ChallengeAuditLog.UserType.SYSTEM, "position", oldPosition, newPosition, null, null, null
        );
    }

    /**
     * Audit content flagged
     */
    public ChallengeAuditLog auditContentFlagged(
            ChallengeAuditLog.ChallengeEntityType entityType,
            Long entityId,
            Long challengeId,
            Long reporterId,
            String reason) {
        
        return createChallengeAuditLog(
            entityType, entityId,
            challengeId, null, null, null, null, reporterId,
            ChallengeAuditLog.ChallengeAuditAction.CONTENT_FLAGGED,
            reporterId, ChallengeAuditLog.UserType.CUSTOMER, null, null, null, null, reason, null
        );
    }

    private ChallengeAuditLog createChallengeAuditLog(
            ChallengeAuditLog.ChallengeEntityType entityType,
            Long entityId,
            Long challengeId,
            Long tournamentId,
            Long matchId,
            Long rankingId,
            Long restaurantId,
            Long customerId,
            ChallengeAuditLog.ChallengeAuditAction action,
            Long userId,
            ChallengeAuditLog.UserType userType,
            String fieldChanged,
            String oldValue,
            String newValue,
            String metadata,
            String reason,
            String ipAddress) {
        
        ChallengeAuditLog auditLog = ChallengeAuditLog.builder()
            .entityType(entityType)
            .entityId(entityId)
            .challengeId(challengeId)
            .tournamentId(tournamentId)
            .matchId(matchId)
            .rankingId(rankingId)
            .restaurantId(restaurantId)
            .customerId(customerId)
            .action(action)
            .userId(userId)
            .userType(userType)
            .fieldChanged(fieldChanged)
            .oldValue(oldValue)
            .newValue(newValue)
            .metadata(metadata)
            .changeReason(reason)
            .ipAddress(ipAddress)
            .changedAt(LocalDateTime.now())
            .build();

        ChallengeAuditLog saved = challengeAuditLogDAO.save(auditLog);
        log.debug("Created challenge audit log: {}", saved);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // UTILITY METHODS
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Convert object to JSON string for storage
     */
    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return obj.toString();
        }
    }
}
