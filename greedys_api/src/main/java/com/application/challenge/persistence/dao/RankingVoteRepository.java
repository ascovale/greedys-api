package com.application.challenge.persistence.dao;

import com.application.challenge.persistence.model.RankingVote;
import com.application.challenge.persistence.model.enums.VoterType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository per RankingVote.
 */
@Repository
public interface RankingVoteRepository extends JpaRepository<RankingVote, Long> {

    // ==================== FIND BY RANKING ====================

    List<RankingVote> findByRankingId(Long rankingId);

    @Query("SELECT COUNT(v) FROM RankingVote v WHERE v.ranking.id = :rankingId")
    long countByRankingId(@Param("rankingId") Long rankingId);

    // ==================== FIND BY RESTAURANT ====================

    List<RankingVote> findByRestaurantId(Long restaurantId);

    @Query("SELECT COUNT(v) FROM RankingVote v WHERE v.ranking.id = :rankingId AND v.restaurant.id = :restaurantId")
    long countByRankingAndRestaurant(
            @Param("rankingId") Long rankingId,
            @Param("restaurantId") Long restaurantId);

    @Query("SELECT AVG(v.score) FROM RankingVote v WHERE v.ranking.id = :rankingId AND v.restaurant.id = :restaurantId")
    Double getAverageScoreByRankingAndRestaurant(
            @Param("rankingId") Long rankingId,
            @Param("restaurantId") Long restaurantId);

    @Query("SELECT SUM(v.voteWeight) FROM RankingVote v WHERE v.ranking.id = :rankingId AND v.restaurant.id = :restaurantId")
    java.math.BigDecimal getWeightedSumByRankingAndRestaurant(
            @Param("rankingId") Long rankingId,
            @Param("restaurantId") Long restaurantId);

    // ==================== FIND BY CUSTOMER ====================

    List<RankingVote> findByVoterId(Long voterId);

    Optional<RankingVote> findByRankingIdAndRestaurantIdAndVoterId(
            Long rankingId, Long restaurantId, Long voterId);

    boolean existsByRankingIdAndRestaurantIdAndVoterId(
            Long rankingId, Long restaurantId, Long voterId);

    @Query("SELECT COUNT(v) FROM RankingVote v WHERE v.voter.id = :customerId AND v.votedAt >= :since")
    long countRecentVotesByCustomer(@Param("customerId") Long customerId, @Param("since") LocalDateTime since);

    // ==================== FIND BY VOTER TYPE ====================

    List<RankingVote> findByRankingIdAndVoterType(Long rankingId, VoterType voterType);

    @Query("SELECT COUNT(v) FROM RankingVote v WHERE v.ranking.id = :rankingId AND v.voterType = :voterType")
    long countByRankingAndVoterType(
            @Param("rankingId") Long rankingId,
            @Param("voterType") VoterType voterType);

    @Query("SELECT v.voterType, COUNT(v) FROM RankingVote v WHERE v.ranking.id = :rankingId GROUP BY v.voterType")
    List<Object[]> countByRankingGroupedByVoterType(@Param("rankingId") Long rankingId);

    @Query("SELECT v.voterType, COUNT(v) FROM RankingVote v WHERE v.ranking.id = :rankingId AND v.restaurant.id = :restaurantId GROUP BY v.voterType")
    List<Object[]> countByRestaurantGroupedByVoterType(
            @Param("rankingId") Long rankingId,
            @Param("restaurantId") Long restaurantId);

    // ==================== FIND VERIFIED ====================

    List<RankingVote> findByRankingIdAndIsVerifiedTrue(Long rankingId);

    @Query("SELECT COUNT(v) FROM RankingVote v WHERE v.ranking.id = :rankingId AND v.isVerified = true")
    long countVerifiedByRanking(@Param("rankingId") Long rankingId);

    @Query("SELECT COUNT(v) FROM RankingVote v WHERE v.ranking.id = :rankingId AND v.restaurant.id = :restaurantId AND v.isVerified = true")
    long countVerifiedByRankingAndRestaurant(
            @Param("rankingId") Long rankingId,
            @Param("restaurantId") Long restaurantId);

    // ==================== FIND BY RESERVATION ====================

    @Query("SELECT v FROM RankingVote v WHERE v.reservation.id = :reservationId")
    Optional<RankingVote> findByReservationId(@Param("reservationId") Long reservationId);

    boolean existsByReservationId(Long reservationId);

    // ==================== FIND BY CATEGORY ====================

    @Query("SELECT v FROM RankingVote v WHERE v.ranking.id = :rankingId AND v.categoryVoted = :category")
    List<RankingVote> findByRankingAndCategory(
            @Param("rankingId") Long rankingId,
            @Param("category") String category);

    @Query("SELECT v.categoryVoted, AVG(v.score), COUNT(v) FROM RankingVote v " +
           "WHERE v.ranking.id = :rankingId AND v.restaurant.id = :restaurantId AND v.categoryVoted IS NOT NULL " +
           "GROUP BY v.categoryVoted")
    List<Object[]> getCategoryStatsByRestaurant(
            @Param("rankingId") Long rankingId,
            @Param("restaurantId") Long restaurantId);

    // ==================== RECENT VOTES ====================

    @Query("SELECT v FROM RankingVote v WHERE v.ranking.id = :rankingId ORDER BY v.votedAt DESC")
    List<RankingVote> findRecentByRanking(@Param("rankingId") Long rankingId, Pageable pageable);

    @Query("SELECT v FROM RankingVote v WHERE v.restaurant.id = :restaurantId ORDER BY v.votedAt DESC")
    List<RankingVote> findRecentByRestaurant(@Param("restaurantId") Long restaurantId, Pageable pageable);

    // ==================== STATISTICS ====================

    @Query("SELECT v.restaurant.id, COUNT(v), AVG(v.score), SUM(v.voteWeight) FROM RankingVote v " +
           "WHERE v.ranking.id = :rankingId GROUP BY v.restaurant.id ORDER BY SUM(v.voteWeight) DESC")
    List<Object[]> getRestaurantStatsByRanking(@Param("rankingId") Long rankingId);

    @Query("SELECT DATE(v.votedAt), COUNT(v) FROM RankingVote v " +
           "WHERE v.ranking.id = :rankingId GROUP BY DATE(v.votedAt) ORDER BY DATE(v.votedAt)")
    List<Object[]> getVotesByDate(@Param("rankingId") Long rankingId);
}
