package com.application.challenge.persistence.dao;

import com.application.challenge.persistence.model.RankingEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository per RankingEntry.
 */
@Repository
public interface RankingEntryRepository extends JpaRepository<RankingEntry, Long> {

    // ==================== FIND BY RANKING ====================

    List<RankingEntry> findByRankingId(Long rankingId);

    Page<RankingEntry> findByRankingId(Long rankingId, Pageable pageable);

    @Query("SELECT e FROM RankingEntry e WHERE e.ranking.id = :rankingId ORDER BY e.position ASC")
    List<RankingEntry> findByRankingOrderedByPosition(@Param("rankingId") Long rankingId);

    @Query("SELECT e FROM RankingEntry e WHERE e.ranking.id = :rankingId ORDER BY e.position ASC")
    Page<RankingEntry> findByRankingOrderedByPosition(@Param("rankingId") Long rankingId, Pageable pageable);

    @Query("SELECT e FROM RankingEntry e WHERE e.ranking.id = :rankingId AND e.position <= :topN ORDER BY e.position ASC")
    List<RankingEntry> findTopN(@Param("rankingId") Long rankingId, @Param("topN") int topN);

    // ==================== FIND BY RESTAURANT ====================

    List<RankingEntry> findByRestaurantId(Long restaurantId);

    Optional<RankingEntry> findByRankingIdAndRestaurantId(Long rankingId, Long restaurantId);

    boolean existsByRankingIdAndRestaurantId(Long rankingId, Long restaurantId);

    @Query("SELECT e FROM RankingEntry e WHERE e.restaurant.id = :restaurantId AND e.ranking.isActive = true ORDER BY e.position ASC")
    List<RankingEntry> findActiveByRestaurant(@Param("restaurantId") Long restaurantId);

    @Query("SELECT e FROM RankingEntry e WHERE e.restaurant.id = :restaurantId ORDER BY e.ranking.periodStart DESC")
    List<RankingEntry> findByRestaurantOrderedByDate(@Param("restaurantId") Long restaurantId, Pageable pageable);

    // ==================== FIND BY POSITION ====================

    @Query("SELECT e FROM RankingEntry e WHERE e.ranking.id = :rankingId AND e.position = 1")
    Optional<RankingEntry> findLeader(@Param("rankingId") Long rankingId);

    @Query("SELECT e FROM RankingEntry e WHERE e.ranking.id = :rankingId AND e.position <= 3 ORDER BY e.position ASC")
    List<RankingEntry> findPodium(@Param("rankingId") Long rankingId);

    @Query("SELECT e FROM RankingEntry e WHERE e.ranking.id = :rankingId AND e.position BETWEEN :start AND :end ORDER BY e.position ASC")
    List<RankingEntry> findByPositionRange(
            @Param("rankingId") Long rankingId,
            @Param("start") int start,
            @Param("end") int end);

    // ==================== FIND MOVERS ====================

    /**
     * Trova i ristoranti che sono saliti di più in classifica.
     * Calcolo: previousPosition - position > 0 significa che sono saliti.
     * Ordiniamo per (previousPosition - position) DESC.
     */
    @Query("SELECT e FROM RankingEntry e WHERE e.ranking.id = :rankingId AND e.previousPosition IS NOT NULL AND (e.previousPosition - e.position) > 0 ORDER BY (e.previousPosition - e.position) DESC")
    List<RankingEntry> findBiggestClimbers(@Param("rankingId") Long rankingId, Pageable pageable);

    /**
     * Trova i ristoranti che sono scesi di più in classifica.
     * Calcolo: previousPosition - position < 0 significa che sono scesi.
     * Ordiniamo per (previousPosition - position) ASC (i più negativi prima).
     */
    @Query("SELECT e FROM RankingEntry e WHERE e.ranking.id = :rankingId AND e.previousPosition IS NOT NULL AND (e.previousPosition - e.position) < 0 ORDER BY (e.previousPosition - e.position) ASC")
    List<RankingEntry> findBiggestFallers(@Param("rankingId") Long rankingId, Pageable pageable);

    @Query("SELECT e FROM RankingEntry e WHERE e.ranking.id = :rankingId AND e.previousPosition IS NULL")
    List<RankingEntry> findNewEntries(@Param("rankingId") Long rankingId);

    // ==================== UPDATE OPERATIONS ====================

    @Modifying
    @Query("UPDATE RankingEntry e SET e.totalVotes = e.totalVotes + 1 WHERE e.id = :id")
    void incrementVotes(@Param("id") Long id);

    @Modifying
    @Query("UPDATE RankingEntry e SET e.score = e.score + :score WHERE e.id = :id")
    void addScore(@Param("id") Long id, @Param("score") java.math.BigDecimal score);

    // ==================== STATISTICS ====================

    @Query("SELECT COUNT(e) FROM RankingEntry e WHERE e.ranking.id = :rankingId")
    long countByRankingId(@Param("rankingId") Long rankingId);

    @Query("SELECT MAX(e.position) FROM RankingEntry e WHERE e.ranking.id = :rankingId")
    Integer getMaxPosition(@Param("rankingId") Long rankingId);

    @Query("SELECT AVG(e.score) FROM RankingEntry e WHERE e.ranking.id = :rankingId")
    java.math.BigDecimal getAverageScore(@Param("rankingId") Long rankingId);

    @Query("SELECT SUM(e.totalVotes) FROM RankingEntry e WHERE e.ranking.id = :rankingId")
    Long getTotalVotes(@Param("rankingId") Long rankingId);

    @Query("SELECT e.position FROM RankingEntry e WHERE e.ranking.id = :rankingId AND e.restaurant.id = :restaurantId")
    Optional<Integer> getRestaurantPosition(
            @Param("rankingId") Long rankingId,
            @Param("restaurantId") Long restaurantId);
}
