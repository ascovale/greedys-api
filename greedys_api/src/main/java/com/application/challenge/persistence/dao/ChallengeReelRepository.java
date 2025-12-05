package com.application.challenge.persistence.dao;

import com.application.challenge.persistence.model.ChallengeReel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository per ChallengeReel.
 */
@Repository
public interface ChallengeReelRepository extends JpaRepository<ChallengeReel, Long> {

    // ==================== FIND BY CHALLENGE ====================

    List<ChallengeReel> findByChallengeId(Long challengeId);

    Page<ChallengeReel> findByChallengeId(Long challengeId, Pageable pageable);

    @Query("SELECT r FROM ChallengeReel r WHERE r.challenge.id = :challengeId AND r.isActive = true AND r.isApproved = true ORDER BY r.createdAt DESC")
    List<ChallengeReel> findActiveByChallengeId(@Param("challengeId") Long challengeId);

    @Query("SELECT r FROM ChallengeReel r WHERE r.challenge.id = :challengeId AND r.isActive = true AND r.isApproved = true ORDER BY r.createdAt DESC")
    Page<ChallengeReel> findActiveByChallengeId(@Param("challengeId") Long challengeId, Pageable pageable);

    // ==================== FIND BY RESTAURANT ====================

    List<ChallengeReel> findByRestaurantId(Long restaurantId);

    Page<ChallengeReel> findByRestaurantId(Long restaurantId, Pageable pageable);

    @Query("SELECT r FROM ChallengeReel r WHERE r.restaurant.id = :restaurantId AND r.isActive = true ORDER BY r.createdAt DESC")
    List<ChallengeReel> findActiveByRestaurantId(@Param("restaurantId") Long restaurantId);

    @Query("SELECT r FROM ChallengeReel r WHERE r.challenge.id = :challengeId AND r.restaurant.id = :restaurantId AND r.isActive = true")
    List<ChallengeReel> findActiveByChallengeAndRestaurant(
            @Param("challengeId") Long challengeId,
            @Param("restaurantId") Long restaurantId);

    // ==================== FIND BY CUSTOMER ====================

    List<ChallengeReel> findByCustomerId(Long customerId);

    @Query("SELECT r FROM ChallengeReel r WHERE r.customer.id = :customerId AND r.isActive = true ORDER BY r.createdAt DESC")
    List<ChallengeReel> findActiveByCustomerId(@Param("customerId") Long customerId);

    // ==================== FIND FEATURED ====================

    @Query("SELECT r FROM ChallengeReel r WHERE r.challenge.id = :challengeId AND r.isFeatured = true AND r.isActive = true")
    List<ChallengeReel> findFeaturedByChallengeId(@Param("challengeId") Long challengeId);

    @Query("SELECT r FROM ChallengeReel r WHERE r.isFeatured = true AND r.isActive = true ORDER BY r.viewsCount DESC")
    List<ChallengeReel> findFeatured(Pageable pageable);

    // ==================== FIND WINNERS ====================

    @Query("SELECT r FROM ChallengeReel r WHERE r.challenge.id = :challengeId AND r.isWinner = true")
    List<ChallengeReel> findWinnersByChallengeId(@Param("challengeId") Long challengeId);

    // ==================== FIND FLAGGED ====================

    List<ChallengeReel> findByIsFlaggedTrue();

    List<ChallengeReel> findByIsApprovedFalse();

    // ==================== FIND POPULAR ====================

    @Query("SELECT r FROM ChallengeReel r WHERE r.challenge.id = :challengeId AND r.isActive = true ORDER BY r.viewsCount DESC")
    List<ChallengeReel> findMostViewedByChallengeId(@Param("challengeId") Long challengeId, Pageable pageable);

    @Query("SELECT r FROM ChallengeReel r WHERE r.challenge.id = :challengeId AND r.isActive = true ORDER BY r.likesCount DESC")
    List<ChallengeReel> findMostLikedByChallengeId(@Param("challengeId") Long challengeId, Pageable pageable);

    @Query("SELECT r FROM ChallengeReel r WHERE r.challenge.id = :challengeId AND r.isActive = true ORDER BY r.engagementRate DESC")
    List<ChallengeReel> findMostEngagingByChallengeId(@Param("challengeId") Long challengeId, Pageable pageable);

    @Query("SELECT r FROM ChallengeReel r WHERE r.isActive = true ORDER BY r.viewsCount DESC")
    List<ChallengeReel> findTrending(Pageable pageable);

    // ==================== UPDATE OPERATIONS ====================

    @Modifying
    @Query("UPDATE ChallengeReel r SET r.viewsCount = r.viewsCount + 1 WHERE r.id = :id")
    void incrementViews(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ChallengeReel r SET r.completeViews = r.completeViews + 1 WHERE r.id = :id")
    void incrementCompleteViews(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ChallengeReel r SET r.likesCount = r.likesCount + 1 WHERE r.id = :id")
    void incrementLikes(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ChallengeReel r SET r.likesCount = r.likesCount - 1 WHERE r.id = :id AND r.likesCount > 0")
    void decrementLikes(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ChallengeReel r SET r.commentsCount = r.commentsCount + 1 WHERE r.id = :id")
    void incrementComments(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ChallengeReel r SET r.sharesCount = r.sharesCount + 1 WHERE r.id = :id")
    void incrementShares(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ChallengeReel r SET r.savesCount = r.savesCount + 1 WHERE r.id = :id")
    void incrementSaves(@Param("id") Long id);

    // ==================== STATISTICS ====================

    @Query("SELECT COUNT(r) FROM ChallengeReel r WHERE r.challenge.id = :challengeId AND r.isActive = true")
    long countActiveByChallengeId(@Param("challengeId") Long challengeId);

    @Query("SELECT SUM(r.viewsCount) FROM ChallengeReel r WHERE r.challenge.id = :challengeId")
    Long getTotalViewsByChallengeId(@Param("challengeId") Long challengeId);

    @Query("SELECT SUM(r.likesCount) FROM ChallengeReel r WHERE r.challenge.id = :challengeId")
    Long getTotalLikesByChallengeId(@Param("challengeId") Long challengeId);

    @Query("SELECT AVG(r.engagementRate) FROM ChallengeReel r WHERE r.challenge.id = :challengeId AND r.isActive = true")
    java.math.BigDecimal getAverageEngagementByChallengeId(@Param("challengeId") Long challengeId);

    @Query("SELECT r.restaurant.id, COUNT(r), SUM(r.viewsCount) FROM ChallengeReel r " +
           "WHERE r.challenge.id = :challengeId AND r.restaurant IS NOT NULL " +
           "GROUP BY r.restaurant.id ORDER BY SUM(r.viewsCount) DESC")
    List<Object[]> getRestaurantStatsByChallengeId(@Param("challengeId") Long challengeId);
}
