package com.application.restaurant.persistence.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.restaurant.persistence.model.RestaurantVerification;
import com.application.restaurant.web.dto.verification.VerificationStatus;

/**
 * Repository for restaurant verification operations
 * 
 * @author Restaurant Verification Team
 * @since 1.0
 */
@Repository
@Transactional(readOnly = true)
public interface RestaurantVerificationDAO extends JpaRepository<RestaurantVerification, Long> {

    /**
     * Find active verification for a restaurant
     * Active means PENDING status and not expired
     */
    @Query("SELECT v FROM RestaurantVerification v WHERE v.restaurant.id = :restaurantId " +
           "AND v.status = 'PENDING' AND v.expiresAt > :now ORDER BY v.createdAt DESC")
    Optional<RestaurantVerification> findActiveVerificationByRestaurant(
        @Param("restaurantId") Long restaurantId, 
        @Param("now") LocalDateTime now
    );

    /**
     * Find active verification for a restaurant (using current time)
     */
    default Optional<RestaurantVerification> findActiveVerificationByRestaurant(Long restaurantId) {
        return findActiveVerificationByRestaurant(restaurantId, LocalDateTime.now());
    }

    /**
     * Find latest verification for a restaurant regardless of status
     */
    @Query("SELECT v FROM RestaurantVerification v WHERE v.restaurant.id = :restaurantId " +
           "ORDER BY v.createdAt DESC")
    Optional<RestaurantVerification> findLatestVerificationByRestaurant(@Param("restaurantId") Long restaurantId);

    /**
     * Find all verifications for a restaurant
     */
    @Query("SELECT v FROM RestaurantVerification v WHERE v.restaurant.id = :restaurantId " +
           "ORDER BY v.createdAt DESC")
    List<RestaurantVerification> findAllByRestaurant(@Param("restaurantId") Long restaurantId);

    /**
     * Find verification by SID
     */
    Optional<RestaurantVerification> findByVerificationSid(String verificationSid);

    /**
     * Expire old pending verifications
     */
    @Modifying
    @Transactional
    @Query("UPDATE RestaurantVerification v SET v.status = 'EXPIRED' " +
           "WHERE v.status = 'PENDING' AND v.expiresAt < :now")
    int expireOldVerifications(@Param("now") LocalDateTime now);

    /**
     * Cancel all pending verifications for a restaurant
     */
    @Modifying
    @Transactional
    @Query("UPDATE RestaurantVerification v SET v.status = 'CANCELLED' " +
           "WHERE v.restaurant.id = :restaurantId AND v.status = 'PENDING'")
    int cancelPendingVerificationsByRestaurant(@Param("restaurantId") Long restaurantId);

    /**
     * Count verifications by status for a restaurant
     */
    @Query("SELECT COUNT(v) FROM RestaurantVerification v " +
           "WHERE v.restaurant.id = :restaurantId AND v.status = :status")
    long countByRestaurantAndStatus(@Param("restaurantId") Long restaurantId, 
                                   @Param("status") VerificationStatus status);

    /**
     * Check if restaurant has verified phone number
     */
    @Query("SELECT COUNT(v) > 0 FROM RestaurantVerification v " +
           "WHERE v.restaurant.id = :restaurantId AND v.status = 'VERIFIED'")
    boolean hasVerifiedPhone(@Param("restaurantId") Long restaurantId);

    /**
     * Find verifications created in a time range
     */
    @Query("SELECT v FROM RestaurantVerification v " +
           "WHERE v.createdAt BETWEEN :start AND :end ORDER BY v.createdAt DESC")
    List<RestaurantVerification> findByCreatedAtBetween(@Param("start") LocalDateTime start, 
                                                       @Param("end") LocalDateTime end);
}
