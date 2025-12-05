package com.application.common.persistence.model.audit;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Audit log for Challenge/Tournament/Ranking system changes.
 * 
 * Tracks all modifications including:
 * - Challenge lifecycle (creation, status changes, completion)
 * - Tournament phases (registration, qualification, brackets, finals)
 * - Ranking updates (position changes, votes)
 * - Participation changes (registration, qualification, elimination)
 * - Votes cast (match votes, ranking votes)
 * - Social content (stories, reels)
 * 
 * Each record captures:
 * - WHAT entity was changed (challenge, tournament, match, ranking, etc.)
 * - WHO changed it (customer, restaurant, admin, system)
 * - WHEN it was changed
 * - WHAT was the action
 * - Additional context (old/new values, reasons)
 */
@Entity
@Table(name = "challenge_audit_log", indexes = {
    @Index(name = "idx_challenge_audit_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_challenge_audit_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_challenge_audit_customer", columnList = "customer_id"),
    @Index(name = "idx_challenge_audit_challenge", columnList = "challenge_id"),
    @Index(name = "idx_challenge_audit_tournament", columnList = "tournament_id"),
    @Index(name = "idx_challenge_audit_action", columnList = "action"),
    @Index(name = "idx_challenge_audit_changed_at", columnList = "changed_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== ENTITY REFERENCES ====================

    /**
     * Type of entity being audited
     */
    @Column(name = "entity_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ChallengeEntityType entityType;

    /**
     * ID of the entity being audited
     */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /**
     * Challenge ID (for context, may be same as entityId if entityType=CHALLENGE)
     */
    @Column(name = "challenge_id")
    private Long challengeId;

    /**
     * Tournament ID (for context)
     */
    @Column(name = "tournament_id")
    private Long tournamentId;

    /**
     * Match ID (for match-related audits)
     */
    @Column(name = "match_id")
    private Long matchId;

    /**
     * Ranking ID (for ranking-related audits)
     */
    @Column(name = "ranking_id")
    private Long rankingId;

    /**
     * Restaurant ID (if action involves a restaurant)
     */
    @Column(name = "restaurant_id")
    private Long restaurantId;

    /**
     * Customer ID (if action involves a customer)
     */
    @Column(name = "customer_id")
    private Long customerId;

    // ==================== ACTION DETAILS ====================

    /**
     * Action performed
     */
    @Column(name = "action", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ChallengeAuditAction action;

    /**
     * User who made the change
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * Type of user who made the change
     */
    @Column(name = "user_type", length = 20)
    @Enumerated(EnumType.STRING)
    private UserType userType;

    /**
     * Specific field that was changed (for UPDATE actions)
     */
    @Column(name = "field_changed", length = 50)
    private String fieldChanged;

    /**
     * Old value (JSON or simple string)
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /**
     * New value (JSON or simple string)
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /**
     * Additional metadata (JSON)
     * Can include: phase, position, score, vote weight, etc.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Optional reason/notes for the change
     */
    @Column(name = "change_reason", length = 500)
    private String changeReason;

    /**
     * IP address (for anti-fraud on votes)
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * When the change was made
     */
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    // ==================== ENUMS ====================

    /**
     * Types of entities in the Challenge system
     */
    public enum ChallengeEntityType {
        CHALLENGE,
        TOURNAMENT,
        TOURNAMENT_MATCH,
        RANKING,
        RANKING_ENTRY,
        PARTICIPATION,
        MATCH_VOTE,
        RANKING_VOTE,
        CHALLENGE_STORY,
        CHALLENGE_REEL
    }

    /**
     * Types of audit actions for Challenge system
     */
    public enum ChallengeAuditAction {
        // ─────── CHALLENGE LIFECYCLE ───────
        CHALLENGE_CREATED,
        CHALLENGE_UPDATED,
        CHALLENGE_STATUS_CHANGED,
        CHALLENGE_REGISTRATION_OPENED,
        CHALLENGE_REGISTRATION_CLOSED,
        CHALLENGE_STARTED,
        CHALLENGE_VOTING_OPENED,
        CHALLENGE_VOTING_CLOSED,
        CHALLENGE_COMPLETED,
        CHALLENGE_CANCELLED,
        
        // ─────── TOURNAMENT LIFECYCLE ───────
        TOURNAMENT_CREATED,
        TOURNAMENT_UPDATED,
        TOURNAMENT_STATUS_CHANGED,
        TOURNAMENT_PHASE_CHANGED,
        TOURNAMENT_BRACKET_GENERATED,
        TOURNAMENT_COMPLETED,
        TOURNAMENT_CANCELLED,
        
        // ─────── MATCH LIFECYCLE ───────
        MATCH_CREATED,
        MATCH_SCHEDULED,
        MATCH_VOTING_OPENED,
        MATCH_VOTING_CLOSED,
        MATCH_RESULT_DECIDED,
        MATCH_CANCELLED,
        
        // ─────── PARTICIPATION ───────
        RESTAURANT_REGISTERED,
        RESTAURANT_QUALIFIED,
        RESTAURANT_ELIMINATED,
        RESTAURANT_WITHDRAWN,
        RESTAURANT_DISQUALIFIED,
        RESTAURANT_WON,
        PARTICIPATION_UPDATED,
        GROUP_ASSIGNED,
        
        // ─────── RANKING ───────
        RANKING_CREATED,
        RANKING_UPDATED,
        RANKING_PUBLISHED,
        RANKING_PERIOD_ENDED,
        RANKING_ENTRY_CREATED,
        RANKING_POSITION_CHANGED,
        
        // ─────── VOTES ───────
        VOTE_CAST,
        VOTE_VERIFIED,
        VOTE_INVALIDATED,
        VOTE_WEIGHT_ADJUSTED,
        
        // ─────── SOCIAL CONTENT ───────
        STORY_CREATED,
        STORY_DELETED,
        STORY_FLAGGED,
        STORY_APPROVED,
        REEL_CREATED,
        REEL_DELETED,
        REEL_FLAGGED,
        REEL_APPROVED,
        REEL_FEATURED,
        REEL_WON,
        
        // ─────── MODERATION ───────
        CONTENT_FLAGGED,
        CONTENT_REMOVED,
        USER_WARNED,
        USER_BANNED_FROM_VOTING
    }

    /**
     * Type of user who made the change
     */
    public enum UserType {
        CUSTOMER,
        RESTAURANT_USER,
        ADMIN,
        MODERATOR,
        SYSTEM
    }

    // ==================== UTILITY ====================

    @Override
    public String toString() {
        return "ChallengeAuditLog{" +
                "id=" + id +
                ", entityType=" + entityType +
                ", entityId=" + entityId +
                ", action=" + action +
                ", userType=" + userType +
                ", changedAt=" + changedAt +
                '}';
    }

    /**
     * Check if this is a vote-related audit
     */
    public boolean isVoteAudit() {
        return action == ChallengeAuditAction.VOTE_CAST ||
               action == ChallengeAuditAction.VOTE_VERIFIED ||
               action == ChallengeAuditAction.VOTE_INVALIDATED ||
               action == ChallengeAuditAction.VOTE_WEIGHT_ADJUSTED;
    }

    /**
     * Check if this is a moderation audit
     */
    public boolean isModerationAudit() {
        return action == ChallengeAuditAction.CONTENT_FLAGGED ||
               action == ChallengeAuditAction.CONTENT_REMOVED ||
               action == ChallengeAuditAction.USER_WARNED ||
               action == ChallengeAuditAction.USER_BANNED_FROM_VOTING ||
               action == ChallengeAuditAction.STORY_FLAGGED ||
               action == ChallengeAuditAction.REEL_FLAGGED;
    }

    /**
     * Check if this is a status change audit
     */
    public boolean isStatusChangeAudit() {
        return action.name().contains("STATUS_CHANGED") ||
               action.name().contains("PHASE_CHANGED");
    }
}
