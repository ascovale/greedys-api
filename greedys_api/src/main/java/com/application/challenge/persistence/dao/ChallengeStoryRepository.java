package com.application.challenge.persistence.dao;

import com.application.challenge.persistence.model.ChallengeStory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository per ChallengeStory.
 */
@Repository
public interface ChallengeStoryRepository extends JpaRepository<ChallengeStory, Long> {

    // ==================== FIND BY CHALLENGE ====================

    List<ChallengeStory> findByChallengeId(Long challengeId);

    Page<ChallengeStory> findByChallengeId(Long challengeId, Pageable pageable);

    @Query("SELECT s FROM ChallengeStory s WHERE s.challenge.id = :challengeId AND s.isActive = true AND s.isApproved = true AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<ChallengeStory> findActiveByChallengeId(
            @Param("challengeId") Long challengeId,
            @Param("now") LocalDateTime now);

    // ==================== FIND BY RESTAURANT ====================

    List<ChallengeStory> findByRestaurantId(Long restaurantId);

    @Query("SELECT s FROM ChallengeStory s WHERE s.restaurant.id = :restaurantId AND s.isActive = true AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<ChallengeStory> findActiveByRestaurantId(
            @Param("restaurantId") Long restaurantId,
            @Param("now") LocalDateTime now);

    @Query("SELECT s FROM ChallengeStory s WHERE s.challenge.id = :challengeId AND s.restaurant.id = :restaurantId AND s.isActive = true AND s.expiresAt > :now")
    List<ChallengeStory> findActiveByChallengeAndRestaurant(
            @Param("challengeId") Long challengeId,
            @Param("restaurantId") Long restaurantId,
            @Param("now") LocalDateTime now);

    // ==================== FIND BY CUSTOMER ====================

    List<ChallengeStory> findByCustomerId(Long customerId);

    @Query("SELECT s FROM ChallengeStory s WHERE s.customer.id = :customerId AND s.isActive = true AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<ChallengeStory> findActiveByCustomerId(
            @Param("customerId") Long customerId,
            @Param("now") LocalDateTime now);

    // ==================== FIND FEATURED ====================

    @Query("SELECT s FROM ChallengeStory s WHERE s.challenge.id = :challengeId AND s.isFeatured = true AND s.isActive = true AND s.expiresAt > :now")
    List<ChallengeStory> findFeaturedByChallengeId(
            @Param("challengeId") Long challengeId,
            @Param("now") LocalDateTime now);

    // ==================== FIND EXPIRED ====================

    @Query("SELECT s FROM ChallengeStory s WHERE s.expiresAt <= :now AND s.isActive = true")
    List<ChallengeStory> findExpiredStories(@Param("now") LocalDateTime now);

    // ==================== FIND FLAGGED ====================

    List<ChallengeStory> findByIsFlaggedTrue();

    List<ChallengeStory> findByIsApprovedFalse();

    // ==================== UPDATE OPERATIONS ====================

    @Modifying
    @Query("UPDATE ChallengeStory s SET s.viewsCount = s.viewsCount + 1 WHERE s.id = :id")
    void incrementViews(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ChallengeStory s SET s.likesCount = s.likesCount + 1 WHERE s.id = :id")
    void incrementLikes(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ChallengeStory s SET s.likesCount = s.likesCount - 1 WHERE s.id = :id AND s.likesCount > 0")
    void decrementLikes(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ChallengeStory s SET s.isActive = false WHERE s.expiresAt <= :now")
    int deactivateExpiredStories(@Param("now") LocalDateTime now);

    // ==================== STATISTICS ====================

    @Query("SELECT COUNT(s) FROM ChallengeStory s WHERE s.challenge.id = :challengeId AND s.isActive = true AND s.expiresAt > :now")
    long countActiveByChallengeId(@Param("challengeId") Long challengeId, @Param("now") LocalDateTime now);

    @Query("SELECT SUM(s.viewsCount) FROM ChallengeStory s WHERE s.challenge.id = :challengeId")
    Long getTotalViewsByChallengeId(@Param("challengeId") Long challengeId);

    @Query("SELECT s FROM ChallengeStory s WHERE s.challenge.id = :challengeId ORDER BY s.viewsCount DESC")
    List<ChallengeStory> findMostViewedByChallengeId(@Param("challengeId") Long challengeId, Pageable pageable);
}
