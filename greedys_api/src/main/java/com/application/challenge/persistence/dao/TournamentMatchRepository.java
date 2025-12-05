package com.application.challenge.persistence.dao;

import com.application.challenge.persistence.model.TournamentMatch;
import com.application.challenge.persistence.model.enums.MatchStatus;
import com.application.challenge.persistence.model.enums.TournamentPhase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository per TournamentMatch.
 */
@Repository
public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, Long> {

    // ==================== FIND BY TOURNAMENT ====================

    List<TournamentMatch> findByTournamentId(Long tournamentId);

    List<TournamentMatch> findByTournamentIdOrderByPhaseAscMatchNumberAsc(Long tournamentId);

    List<TournamentMatch> findByTournamentIdAndPhase(Long tournamentId, TournamentPhase phase);

    List<TournamentMatch> findByTournamentIdAndStatus(Long tournamentId, MatchStatus status);

    @Query("SELECT m FROM TournamentMatch m WHERE m.tournament.id = :tournamentId AND m.phase = :phase ORDER BY m.groupNumber, m.matchNumber")
    List<TournamentMatch> findByTournamentAndPhaseOrdered(
            @Param("tournamentId") Long tournamentId,
            @Param("phase") TournamentPhase phase);

    // ==================== FIND BY GROUP ====================

    List<TournamentMatch> findByTournamentIdAndGroupNumber(Long tournamentId, Integer groupNumber);

    @Query("SELECT m FROM TournamentMatch m WHERE m.tournament.id = :tournamentId AND m.groupNumber = :groupNumber ORDER BY m.roundNumber, m.matchNumber")
    List<TournamentMatch> findGroupMatches(
            @Param("tournamentId") Long tournamentId,
            @Param("groupNumber") Integer groupNumber);

    // ==================== FIND BY STATUS ====================

    List<TournamentMatch> findByStatus(MatchStatus status);

    List<TournamentMatch> findByStatusAndVotingEndsAtBefore(MatchStatus status, LocalDateTime time);

    @Query("SELECT m FROM TournamentMatch m WHERE m.status = 'VOTING' AND m.votingEndsAt <= :time")
    List<TournamentMatch> findExpiredVotingMatches(@Param("time") LocalDateTime time);

    @Query("SELECT m FROM TournamentMatch m WHERE m.status = 'SCHEDULED' AND m.votingStartsAt <= :time")
    List<TournamentMatch> findMatchesToOpen(@Param("time") LocalDateTime time);

    // ==================== FIND BY RESTAURANT ====================

    @Query("SELECT m FROM TournamentMatch m WHERE m.restaurant1.id = :restaurantId OR m.restaurant2.id = :restaurantId")
    List<TournamentMatch> findByRestaurantId(@Param("restaurantId") Long restaurantId);

    @Query("SELECT m FROM TournamentMatch m WHERE m.tournament.id = :tournamentId AND (m.restaurant1.id = :restaurantId OR m.restaurant2.id = :restaurantId)")
    List<TournamentMatch> findByTournamentAndRestaurant(
            @Param("tournamentId") Long tournamentId,
            @Param("restaurantId") Long restaurantId);

    @Query("SELECT m FROM TournamentMatch m WHERE m.winner.id = :restaurantId")
    List<TournamentMatch> findWonByRestaurant(@Param("restaurantId") Long restaurantId);

    // ==================== FIND CURRENT/NEXT ====================

    @Query("SELECT m FROM TournamentMatch m WHERE m.tournament.id = :tournamentId AND m.status = 'VOTING' ORDER BY m.votingEndsAt ASC")
    List<TournamentMatch> findCurrentVotingMatches(@Param("tournamentId") Long tournamentId);

    @Query("SELECT m FROM TournamentMatch m WHERE m.tournament.id = :tournamentId AND m.status = 'SCHEDULED' ORDER BY m.votingStartsAt ASC")
    List<TournamentMatch> findNextMatches(@Param("tournamentId") Long tournamentId, Pageable pageable);

    // ==================== FIND SPECIFIC MATCH ====================

    @Query("SELECT m FROM TournamentMatch m WHERE m.tournament.id = :tournamentId AND m.phase = :phase AND m.matchNumber = :matchNumber")
    Optional<TournamentMatch> findSpecificMatch(
            @Param("tournamentId") Long tournamentId,
            @Param("phase") TournamentPhase phase,
            @Param("matchNumber") Integer matchNumber);

    // ==================== STATISTICS ====================

    @Query("SELECT COUNT(m) FROM TournamentMatch m WHERE m.tournament.id = :tournamentId AND m.status = :status")
    long countByTournamentAndStatus(
            @Param("tournamentId") Long tournamentId,
            @Param("status") MatchStatus status);

    @Query("SELECT SUM(m.votes1 + m.votes2) FROM TournamentMatch m WHERE m.tournament.id = :tournamentId")
    Long getTotalVotesByTournament(@Param("tournamentId") Long tournamentId);

    @Query("SELECT m FROM TournamentMatch m WHERE m.tournament.id = :tournamentId ORDER BY (m.votes1 + m.votes2) DESC")
    List<TournamentMatch> findMostVotedMatches(@Param("tournamentId") Long tournamentId, Pageable pageable);
}
