package com.application.challenge.persistence.dao;

import com.application.challenge.persistence.model.MatchVote;
import com.application.challenge.persistence.model.enums.VoterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository per MatchVote.
 */
@Repository
public interface MatchVoteRepository extends JpaRepository<MatchVote, Long> {

    // ==================== FIND BY MATCH ====================

    List<MatchVote> findByMatchId(Long matchId);

    @Query("SELECT COUNT(v) FROM MatchVote v WHERE v.match.id = :matchId")
    long countByMatchId(@Param("matchId") Long matchId);

    // ==================== FIND BY CUSTOMER ====================

    List<MatchVote> findByCustomerId(Long customerId);

    Optional<MatchVote> findByMatchIdAndCustomerId(Long matchId, Long customerId);

    boolean existsByMatchIdAndCustomerId(Long matchId, Long customerId);

    @Query("SELECT COUNT(v) FROM MatchVote v WHERE v.customer.id = :customerId AND v.votedAt >= :since")
    long countRecentVotesByCustomer(@Param("customerId") Long customerId, @Param("since") LocalDateTime since);

    // ==================== FIND BY RESTAURANT ====================

    List<MatchVote> findByVotedRestaurantId(Long restaurantId);

    @Query("SELECT COUNT(v) FROM MatchVote v WHERE v.match.id = :matchId AND v.votedRestaurant.id = :restaurantId")
    long countVotesForRestaurantInMatch(
            @Param("matchId") Long matchId,
            @Param("restaurantId") Long restaurantId);

    @Query("SELECT SUM(v.voteWeight) FROM MatchVote v WHERE v.match.id = :matchId AND v.votedRestaurant.id = :restaurantId")
    java.math.BigDecimal getWeightedVotesForRestaurant(
            @Param("matchId") Long matchId,
            @Param("restaurantId") Long restaurantId);

    // ==================== FIND BY VOTER TYPE ====================

    List<MatchVote> findByMatchIdAndVoterType(Long matchId, VoterType voterType);

    @Query("SELECT COUNT(v) FROM MatchVote v WHERE v.match.id = :matchId AND v.voterType = :voterType")
    long countByMatchAndVoterType(@Param("matchId") Long matchId, @Param("voterType") VoterType voterType);

    @Query("SELECT v.voterType, COUNT(v) FROM MatchVote v WHERE v.match.id = :matchId GROUP BY v.voterType")
    List<Object[]> countByMatchGroupedByVoterType(@Param("matchId") Long matchId);

    // ==================== FIND VERIFIED ====================

    List<MatchVote> findByMatchIdAndIsVerifiedTrue(Long matchId);

    @Query("SELECT COUNT(v) FROM MatchVote v WHERE v.match.id = :matchId AND v.isVerified = true")
    long countVerifiedVotesByMatch(@Param("matchId") Long matchId);

    @Query("SELECT v FROM MatchVote v WHERE v.match.id = :matchId AND v.isVerified = true AND v.votedRestaurant.id = :restaurantId")
    List<MatchVote> findVerifiedVotesForRestaurant(
            @Param("matchId") Long matchId,
            @Param("restaurantId") Long restaurantId);

    // ==================== ANTI-FRAUD ====================

    @Query("SELECT COUNT(v) FROM MatchVote v WHERE v.ipAddress = :ip AND v.match.id = :matchId")
    long countByIpAndMatch(@Param("ip") String ipAddress, @Param("matchId") Long matchId);

    @Query("SELECT COUNT(v) FROM MatchVote v WHERE v.deviceId = :deviceId AND v.match.id = :matchId")
    long countByDeviceAndMatch(@Param("deviceId") String deviceId, @Param("matchId") Long matchId);

    @Query("SELECT v FROM MatchVote v WHERE v.match.id = :matchId AND v.ipAddress = :ip AND v.customer.id != :customerId")
    List<MatchVote> findSuspiciousVotesByIp(
            @Param("matchId") Long matchId,
            @Param("ip") String ipAddress,
            @Param("customerId") Long customerId);

    // ==================== STATISTICS ====================

    @Query("SELECT v.votedRestaurant.id, COUNT(v), SUM(v.voteWeight) FROM MatchVote v " +
           "WHERE v.match.id = :matchId GROUP BY v.votedRestaurant.id")
    List<Object[]> getVoteStatsByMatch(@Param("matchId") Long matchId);

    @Query("SELECT AVG(v.voteWeight) FROM MatchVote v WHERE v.match.id = :matchId")
    java.math.BigDecimal getAverageWeightByMatch(@Param("matchId") Long matchId);
}
