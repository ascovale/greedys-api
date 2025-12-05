package com.application.challenge.persistence.dao;

import com.application.challenge.persistence.model.ChallengeParticipation;
import com.application.challenge.persistence.model.enums.ParticipationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository per ChallengeParticipation.
 */
@Repository
public interface ChallengeParticipationRepository extends JpaRepository<ChallengeParticipation, Long> {

    // ==================== FIND BY CHALLENGE ====================

    List<ChallengeParticipation> findByChallengeId(Long challengeId);

    Page<ChallengeParticipation> findByChallengeId(Long challengeId, Pageable pageable);

    List<ChallengeParticipation> findByChallengeIdAndStatus(Long challengeId, ParticipationStatus status);

    @Query("SELECT p FROM ChallengeParticipation p WHERE p.challenge.id = :challengeId ORDER BY p.qualificationScore DESC")
    List<ChallengeParticipation> findByChallengeOrderedByScore(@Param("challengeId") Long challengeId);

    // ==================== FIND BY TOURNAMENT ====================

    List<ChallengeParticipation> findByTournamentId(Long tournamentId);

    Page<ChallengeParticipation> findByTournamentId(Long tournamentId, Pageable pageable);

    List<ChallengeParticipation> findByTournamentIdAndStatus(Long tournamentId, ParticipationStatus status);

    @Query("SELECT p FROM ChallengeParticipation p WHERE p.tournament.id = :tournamentId ORDER BY p.qualificationScore DESC")
    List<ChallengeParticipation> findByTournamentOrderedByScore(@Param("tournamentId") Long tournamentId);

    // ==================== FIND BY GROUP ====================

    @Query("SELECT p FROM ChallengeParticipation p WHERE p.tournament.id = :tournamentId AND p.groupNumber = :groupNumber ORDER BY p.groupPoints DESC, p.totalVotes DESC")
    List<ChallengeParticipation> findGroupStandings(
            @Param("tournamentId") Long tournamentId,
            @Param("groupNumber") Integer groupNumber);

    // ==================== FIND BY RESTAURANT ====================

    List<ChallengeParticipation> findByRestaurantId(Long restaurantId);

    Optional<ChallengeParticipation> findByChallengeIdAndRestaurantId(Long challengeId, Long restaurantId);

    Optional<ChallengeParticipation> findByTournamentIdAndRestaurantId(Long tournamentId, Long restaurantId);

    boolean existsByChallengeIdAndRestaurantId(Long challengeId, Long restaurantId);

    boolean existsByTournamentIdAndRestaurantId(Long tournamentId, Long restaurantId);

    @Query("SELECT p FROM ChallengeParticipation p WHERE p.restaurant.id = :restaurantId AND p.status IN :statuses")
    List<ChallengeParticipation> findActiveByRestaurant(
            @Param("restaurantId") Long restaurantId,
            @Param("statuses") List<ParticipationStatus> statuses);

    // ==================== FIND BY STATUS ====================

    List<ChallengeParticipation> findByStatus(ParticipationStatus status);

    @Query("SELECT p FROM ChallengeParticipation p WHERE p.status = :status AND p.tournament.id = :tournamentId")
    List<ChallengeParticipation> findByTournamentAndStatus(
            @Param("tournamentId") Long tournamentId,
            @Param("status") ParticipationStatus status);

    // ==================== FIND WINNERS ====================

    @Query("SELECT p FROM ChallengeParticipation p WHERE p.challenge.id = :challengeId AND p.status = 'WINNER'")
    Optional<ChallengeParticipation> findChallengeWinner(@Param("challengeId") Long challengeId);

    @Query("SELECT p FROM ChallengeParticipation p WHERE p.tournament.id = :tournamentId AND p.status = 'WINNER'")
    Optional<ChallengeParticipation> findTournamentWinner(@Param("tournamentId") Long tournamentId);

    @Query("SELECT p FROM ChallengeParticipation p WHERE p.tournament.id = :tournamentId AND p.status IN ('WINNER', 'RUNNER_UP', 'THIRD_PLACE') ORDER BY p.finalPosition ASC")
    List<ChallengeParticipation> findTournamentPodium(@Param("tournamentId") Long tournamentId);

    // ==================== QUALIFICATION ====================

    @Query("SELECT p FROM ChallengeParticipation p WHERE p.tournament.id = :tournamentId AND p.status = 'REGISTERED' ORDER BY p.qualificationScore DESC")
    List<ChallengeParticipation> findQualificationRanking(
            @Param("tournamentId") Long tournamentId,
            Pageable pageable);

    @Query("SELECT p FROM ChallengeParticipation p WHERE p.tournament.id = :tournamentId AND p.qualificationRank <= :maxRank")
    List<ChallengeParticipation> findQualified(
            @Param("tournamentId") Long tournamentId,
            @Param("maxRank") Integer maxRank);

    // ==================== STATISTICS ====================

    @Query("SELECT COUNT(p) FROM ChallengeParticipation p WHERE p.challenge.id = :challengeId")
    long countByChallengeId(@Param("challengeId") Long challengeId);

    @Query("SELECT COUNT(p) FROM ChallengeParticipation p WHERE p.tournament.id = :tournamentId")
    long countByTournamentId(@Param("tournamentId") Long tournamentId);

    @Query("SELECT COUNT(p) FROM ChallengeParticipation p WHERE p.tournament.id = :tournamentId AND p.status = :status")
    long countByTournamentAndStatus(
            @Param("tournamentId") Long tournamentId,
            @Param("status") ParticipationStatus status);

    @Query("SELECT p.restaurant.id, COUNT(p) FROM ChallengeParticipation p WHERE p.status = 'WINNER' GROUP BY p.restaurant.id ORDER BY COUNT(p) DESC")
    List<Object[]> findMostWinningRestaurants(Pageable pageable);
}
