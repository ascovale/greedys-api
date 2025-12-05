package com.application.common.persistence.dao.audit;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.audit.ChallengeAuditLog;
import com.application.common.persistence.model.audit.ChallengeAuditLog.ChallengeAuditAction;
import com.application.common.persistence.model.audit.ChallengeAuditLog.ChallengeEntityType;

/**
 * DAO for ChallengeAuditLog entity.
 */
@Repository
public interface ChallengeAuditLogDAO extends JpaRepository<ChallengeAuditLog, Long> {

    // ==================== FIND BY ENTITY ====================

    List<ChallengeAuditLog> findByEntityTypeAndEntityIdOrderByChangedAtDesc(
            ChallengeEntityType entityType, Long entityId);

    Page<ChallengeAuditLog> findByEntityTypeAndEntityIdOrderByChangedAtDesc(
            ChallengeEntityType entityType, Long entityId, Pageable pageable);

    // ==================== FIND BY CHALLENGE ====================

    List<ChallengeAuditLog> findByChallengeIdOrderByChangedAtDesc(Long challengeId);

    Page<ChallengeAuditLog> findByChallengeIdOrderByChangedAtDesc(Long challengeId, Pageable pageable);

    @Query("SELECT a FROM ChallengeAuditLog a WHERE a.challengeId = :challengeId AND a.action IN :actions ORDER BY a.changedAt DESC")
    List<ChallengeAuditLog> findByChallengeIdAndActions(
            @Param("challengeId") Long challengeId,
            @Param("actions") List<ChallengeAuditAction> actions);

    // ==================== FIND BY TOURNAMENT ====================

    List<ChallengeAuditLog> findByTournamentIdOrderByChangedAtDesc(Long tournamentId);

    Page<ChallengeAuditLog> findByTournamentIdOrderByChangedAtDesc(Long tournamentId, Pageable pageable);

    @Query("SELECT a FROM ChallengeAuditLog a WHERE a.tournamentId = :tournamentId AND a.action IN :actions ORDER BY a.changedAt DESC")
    List<ChallengeAuditLog> findByTournamentIdAndActions(
            @Param("tournamentId") Long tournamentId,
            @Param("actions") List<ChallengeAuditAction> actions);

    // ==================== FIND BY MATCH ====================

    List<ChallengeAuditLog> findByMatchIdOrderByChangedAtDesc(Long matchId);

    // ==================== FIND BY RANKING ====================

    List<ChallengeAuditLog> findByRankingIdOrderByChangedAtDesc(Long rankingId);

    // ==================== FIND BY RESTAURANT ====================

    List<ChallengeAuditLog> findByRestaurantIdOrderByChangedAtDesc(Long restaurantId);

    Page<ChallengeAuditLog> findByRestaurantIdOrderByChangedAtDesc(Long restaurantId, Pageable pageable);

    @Query("SELECT a FROM ChallengeAuditLog a WHERE a.restaurantId = :restaurantId AND a.challengeId = :challengeId ORDER BY a.changedAt DESC")
    List<ChallengeAuditLog> findByRestaurantAndChallenge(
            @Param("restaurantId") Long restaurantId,
            @Param("challengeId") Long challengeId);

    // ==================== FIND BY CUSTOMER ====================

    List<ChallengeAuditLog> findByCustomerIdOrderByChangedAtDesc(Long customerId);

    Page<ChallengeAuditLog> findByCustomerIdOrderByChangedAtDesc(Long customerId, Pageable pageable);

    @Query("SELECT a FROM ChallengeAuditLog a WHERE a.customerId = :customerId AND a.action = 'VOTE_CAST' ORDER BY a.changedAt DESC")
    List<ChallengeAuditLog> findVotesByCustomer(@Param("customerId") Long customerId);

    // ==================== FIND BY ACTION ====================

    List<ChallengeAuditLog> findByActionOrderByChangedAtDesc(ChallengeAuditAction action);

    Page<ChallengeAuditLog> findByActionOrderByChangedAtDesc(ChallengeAuditAction action, Pageable pageable);

    List<ChallengeAuditLog> findByActionInOrderByChangedAtDesc(List<ChallengeAuditAction> actions);

    // ==================== FIND BY DATE RANGE ====================

    @Query("SELECT a FROM ChallengeAuditLog a WHERE a.changedAt BETWEEN :start AND :end ORDER BY a.changedAt DESC")
    List<ChallengeAuditLog> findByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT a FROM ChallengeAuditLog a WHERE a.challengeId = :challengeId AND a.changedAt BETWEEN :start AND :end ORDER BY a.changedAt DESC")
    List<ChallengeAuditLog> findByChallengeAndDateRange(
            @Param("challengeId") Long challengeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // ==================== VOTE AUDITS ====================

    @Query("SELECT a FROM ChallengeAuditLog a WHERE a.matchId = :matchId AND a.action = 'VOTE_CAST' ORDER BY a.changedAt DESC")
    List<ChallengeAuditLog> findVoteAuditsByMatch(@Param("matchId") Long matchId);

    @Query("SELECT a FROM ChallengeAuditLog a WHERE a.rankingId = :rankingId AND a.action = 'VOTE_CAST' ORDER BY a.changedAt DESC")
    List<ChallengeAuditLog> findVoteAuditsByRanking(@Param("rankingId") Long rankingId);

    @Query("SELECT COUNT(a) FROM ChallengeAuditLog a WHERE a.customerId = :customerId AND a.action = 'VOTE_CAST' AND a.changedAt >= :since")
    long countRecentVotesByCustomer(@Param("customerId") Long customerId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM ChallengeAuditLog a WHERE a.ipAddress = :ip AND a.action = 'VOTE_CAST' AND a.changedAt >= :since")
    long countRecentVotesByIp(@Param("ip") String ipAddress, @Param("since") LocalDateTime since);

    // ==================== MODERATION AUDITS ====================

    @Query("SELECT a FROM ChallengeAuditLog a WHERE a.action IN ('CONTENT_FLAGGED', 'CONTENT_REMOVED', 'USER_WARNED', 'USER_BANNED_FROM_VOTING', 'STORY_FLAGGED', 'REEL_FLAGGED') ORDER BY a.changedAt DESC")
    List<ChallengeAuditLog> findModerationAudits(Pageable pageable);

    @Query("SELECT a FROM ChallengeAuditLog a WHERE a.customerId = :customerId AND a.action IN ('USER_WARNED', 'USER_BANNED_FROM_VOTING') ORDER BY a.changedAt DESC")
    List<ChallengeAuditLog> findUserWarnings(@Param("customerId") Long customerId);

    // ==================== STATUS CHANGE AUDITS ====================

    @Query("SELECT a FROM ChallengeAuditLog a WHERE a.challengeId = :challengeId AND a.action LIKE '%STATUS_CHANGED%' ORDER BY a.changedAt ASC")
    List<ChallengeAuditLog> findChallengeStatusHistory(@Param("challengeId") Long challengeId);

    @Query("SELECT a FROM ChallengeAuditLog a WHERE a.tournamentId = :tournamentId AND a.action LIKE '%PHASE_CHANGED%' ORDER BY a.changedAt ASC")
    List<ChallengeAuditLog> findTournamentPhaseHistory(@Param("tournamentId") Long tournamentId);

    // ==================== STATISTICS ====================

    @Query("SELECT a.action, COUNT(a) FROM ChallengeAuditLog a WHERE a.challengeId = :challengeId GROUP BY a.action")
    List<Object[]> countActionsByChallenge(@Param("challengeId") Long challengeId);

    @Query("SELECT a.action, COUNT(a) FROM ChallengeAuditLog a WHERE a.tournamentId = :tournamentId GROUP BY a.action")
    List<Object[]> countActionsByTournament(@Param("tournamentId") Long tournamentId);

    @Query("SELECT COUNT(a) FROM ChallengeAuditLog a WHERE a.action = 'VOTE_CAST' AND a.challengeId = :challengeId")
    long countTotalVotesForChallenge(@Param("challengeId") Long challengeId);

    @Query("SELECT COUNT(a) FROM ChallengeAuditLog a WHERE a.action = 'VOTE_CAST' AND a.tournamentId = :tournamentId")
    long countTotalVotesForTournament(@Param("tournamentId") Long tournamentId);
}
