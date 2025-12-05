package com.application.challenge.persistence.dao;

import com.application.challenge.persistence.model.Ranking;
import com.application.challenge.persistence.model.enums.RankingPeriod;
import com.application.challenge.persistence.model.enums.RankingScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository per Ranking.
 */
@Repository
public interface RankingRepository extends JpaRepository<Ranking, Long> {

    // ==================== FIND BY SCOPE/PERIOD ====================

    List<Ranking> findByScope(RankingScope scope);

    List<Ranking> findByPeriod(RankingPeriod period);

    List<Ranking> findByScopeAndPeriod(RankingScope scope, RankingPeriod period);

    Optional<Ranking> findByScopeAndPeriodAndIsActiveTrue(RankingScope scope, RankingPeriod period);

    // ==================== FIND BY LOCATION ====================

    List<Ranking> findByCity(String city);

    List<Ranking> findByCityAndIsActiveTrue(String city);

    List<Ranking> findByRegion(String region);

    List<Ranking> findByZone(String zone);

    @Query("SELECT r FROM Ranking r WHERE r.city = :city AND r.period = :period AND r.isActive = true")
    Optional<Ranking> findActiveByCityAndPeriod(
            @Param("city") String city,
            @Param("period") RankingPeriod period);

    @Query("SELECT r FROM Ranking r WHERE r.city = :city AND r.cuisineType = :cuisineType AND r.period = :period AND r.isActive = true")
    Optional<Ranking> findActiveByCityCuisineAndPeriod(
            @Param("city") String city,
            @Param("cuisineType") String cuisineType,
            @Param("period") RankingPeriod period);

    // ==================== FIND BY CUISINE/CATEGORY ====================

    List<Ranking> findByCuisineType(String cuisineType);

    List<Ranking> findByCuisineTypeAndIsActiveTrue(String cuisineType);

    List<Ranking> findByDishCategory(String dishCategory);

    @Query("SELECT r FROM Ranking r WHERE r.cuisineType = :cuisineType AND r.scope = :scope AND r.isActive = true")
    List<Ranking> findActiveByCuisineAndScope(
            @Param("cuisineType") String cuisineType,
            @Param("scope") RankingScope scope);

    // ==================== FIND BY DATE ====================

    @Query("SELECT r FROM Ranking r WHERE r.periodStart <= :date AND r.periodEnd >= :date AND r.isActive = true")
    List<Ranking> findActiveOnDate(@Param("date") LocalDate date);

    List<Ranking> findByPeriodEndBeforeAndIsActiveTrue(LocalDate date);

    @Query("SELECT r FROM Ranking r WHERE r.period = :period AND r.periodStart >= :startDate AND r.periodStart <= :endDate")
    List<Ranking> findByPeriodAndDateRange(
            @Param("period") RankingPeriod period,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // ==================== FIND ACTIVE ====================

    List<Ranking> findByIsActiveTrue();

    Page<Ranking> findByIsActiveTrue(Pageable pageable);

    List<Ranking> findByPeriodAndIsActiveTrue(RankingPeriod period);

    @Query("SELECT r FROM Ranking r WHERE r.isActive = true ORDER BY r.lastCalculatedAt DESC")
    List<Ranking> findMostRecentActive(Pageable pageable);

    // ==================== COMPLEX QUERIES ====================

    @Query("SELECT r FROM Ranking r WHERE " +
           "r.isActive = true AND " +
           "(:scope IS NULL OR r.scope = :scope) AND " +
           "(:period IS NULL OR r.period = :period) AND " +
           "(:cuisineType IS NULL OR r.cuisineType = :cuisineType) AND " +
           "(:city IS NULL OR r.city = :city)")
    Page<Ranking> findByFilters(
            @Param("scope") RankingScope scope,
            @Param("period") RankingPeriod period,
            @Param("cuisineType") String cuisineType,
            @Param("city") String city,
            Pageable pageable);

    // ==================== STATISTICS ====================

    @Query("SELECT COUNT(r) FROM Ranking r WHERE r.isActive = true")
    long countActive();

    @Query("SELECT r.scope, COUNT(r) FROM Ranking r WHERE r.isActive = true GROUP BY r.scope")
    List<Object[]> countByScope();

    @Query("SELECT r.period, COUNT(r) FROM Ranking r WHERE r.isActive = true GROUP BY r.period")
    List<Object[]> countByPeriod();
}
