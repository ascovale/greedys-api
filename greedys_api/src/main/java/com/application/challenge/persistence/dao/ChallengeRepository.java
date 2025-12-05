package com.application.challenge.persistence.dao;

import com.application.challenge.persistence.model.Challenge;
import com.application.challenge.persistence.model.enums.ChallengeStatus;
import com.application.challenge.persistence.model.enums.ChallengeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository per Challenge.
 */
@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    // ==================== FIND BY SLUG/NAME ====================

    Optional<Challenge> findBySlug(String slug);

    Optional<Challenge> findByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);

    // ==================== FIND BY STATUS ====================

    List<Challenge> findByStatus(ChallengeStatus status);

    Page<Challenge> findByStatus(ChallengeStatus status, Pageable pageable);

    List<Challenge> findByStatusIn(List<ChallengeStatus> statuses);

    @Query("SELECT c FROM Challenge c WHERE c.status IN :statuses ORDER BY c.startDate ASC")
    List<Challenge> findActiveOrUpcoming(@Param("statuses") List<ChallengeStatus> statuses);

    // ==================== FIND BY TYPE/CATEGORY ====================

    List<Challenge> findByChallengeType(ChallengeType type);

    Page<Challenge> findByChallengeType(ChallengeType type, Pageable pageable);

    List<Challenge> findByChallengeTypeAndStatus(ChallengeType type, ChallengeStatus status);

    List<Challenge> findByCategoryFilterIgnoreCase(String category);

    @Query("SELECT c FROM Challenge c WHERE c.challengeType = :type AND c.status IN :statuses")
    List<Challenge> findByTypeAndStatuses(
            @Param("type") ChallengeType type,
            @Param("statuses") List<ChallengeStatus> statuses);

    // ==================== FIND BY LOCATION ====================

    List<Challenge> findByCity(String city);

    List<Challenge> findByCityAndStatus(String city, ChallengeStatus status);

    List<Challenge> findByRegion(String region);

    List<Challenge> findByRegionAndStatus(String region, ChallengeStatus status);

    @Query("SELECT c FROM Challenge c WHERE c.city = :city AND c.status IN :statuses ORDER BY c.startDate ASC")
    List<Challenge> findByCityAndStatuses(
            @Param("city") String city,
            @Param("statuses") List<ChallengeStatus> statuses);

    @Query("SELECT c FROM Challenge c WHERE " +
           "(6371 * acos(cos(radians(:lat)) * cos(radians(c.latitude)) * " +
           "cos(radians(c.longitude) - radians(:lon)) + " +
           "sin(radians(:lat)) * sin(radians(c.latitude)))) <= :radiusKm " +
           "AND c.status IN :statuses")
    List<Challenge> findNearbyWithStatuses(
            @Param("lat") double latitude,
            @Param("lon") double longitude,
            @Param("radiusKm") double radiusKm,
            @Param("statuses") List<ChallengeStatus> statuses);

    // ==================== FIND BY DATE ====================

    List<Challenge> findByStartDateBetween(LocalDate start, LocalDate end);

    List<Challenge> findByEndDateBefore(LocalDate date);

    @Query("SELECT c FROM Challenge c WHERE c.registrationStartDate <= :date AND c.registrationEndDate >= :date AND c.status = 'REGISTRATION'")
    List<Challenge> findWithOpenRegistration(@Param("date") LocalDate date);

    @Query("SELECT c FROM Challenge c WHERE c.votingStartDate <= :date AND c.votingEndDate >= :date AND c.status = 'VOTING'")
    List<Challenge> findWithOpenVoting(@Param("date") LocalDate date);

    // ==================== FEATURED/SPONSORED ====================

    List<Challenge> findByIsFeaturedTrueAndStatus(ChallengeStatus status);

    List<Challenge> findByIsSponsoredTrue();

    @Query("SELECT c FROM Challenge c WHERE c.isFeatured = true AND c.status IN :statuses ORDER BY c.startDate ASC")
    List<Challenge> findFeaturedWithStatuses(@Param("statuses") List<ChallengeStatus> statuses);

    // ==================== SEARCH ====================

    @Query("SELECT c FROM Challenge c WHERE " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.categoryFilter) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND c.status IN :statuses")
    Page<Challenge> searchByQueryAndStatuses(
            @Param("query") String query,
            @Param("statuses") List<ChallengeStatus> statuses,
            Pageable pageable);

    // ==================== TOURNAMENT ====================

    List<Challenge> findByTournamentId(Long tournamentId);

    // ==================== STATISTICS ====================

    @Query("SELECT COUNT(c) FROM Challenge c WHERE c.status = :status")
    long countByStatus(@Param("status") ChallengeStatus status);

    long countByStatusAndCompletedAtBefore(ChallengeStatus status, LocalDateTime before);

    @Query("SELECT c FROM Challenge c ORDER BY c.participantsCount DESC")
    List<Challenge> findMostPopular(Pageable pageable);

    @Query("SELECT c FROM Challenge c WHERE c.status = :status ORDER BY c.viewsCount DESC")
    List<Challenge> findMostViewedByStatus(@Param("status") ChallengeStatus status, Pageable pageable);

    // ==================== SCHEDULED JOBS ====================

    List<Challenge> findByStatusAndStartDateBefore(ChallengeStatus status, LocalDateTime before);

    List<Challenge> findByStatusInAndEndDateBefore(List<ChallengeStatus> statuses, LocalDateTime before);
}
