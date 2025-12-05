package com.application.common.persistence.dao.social;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.social.FollowStatus;
import com.application.common.persistence.model.social.SocialFollow;

/**
 * ⭐ SOCIAL FOLLOW DAO
 * 
 * Repository per la gestione delle relazioni di follow.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Repository
public interface SocialFollowDAO extends JpaRepository<SocialFollow, Long> {

    /**
     * Trova relazione tra due entità
     */
    @Query("SELECT f FROM SocialFollow f " +
           "WHERE f.followerId = :followerId " +
           "AND f.followerType = :followerType " +
           "AND f.followingId = :followingId " +
           "AND f.followingType = :followingType")
    Optional<SocialFollow> findRelation(
        @Param("followerId") Long followerId,
        @Param("followerType") String followerType,
        @Param("followingId") Long followingId,
        @Param("followingType") String followingType
    );

    /**
     * Verifica se esiste un follow attivo
     */
    @Query("SELECT COUNT(f) > 0 FROM SocialFollow f " +
           "WHERE f.followerId = :followerId " +
           "AND f.followerType = :followerType " +
           "AND f.followingId = :followingId " +
           "AND f.followingType = :followingType " +
           "AND f.status = 'ACTIVE'")
    boolean isFollowing(
        @Param("followerId") Long followerId,
        @Param("followerType") String followerType,
        @Param("followingId") Long followingId,
        @Param("followingType") String followingType
    );

    /**
     * Lista follower di un'entità
     */
    @Query("SELECT f FROM SocialFollow f " +
           "WHERE f.followingId = :followingId " +
           "AND f.followingType = :followingType " +
           "AND f.status = 'ACTIVE' " +
           "ORDER BY f.createdAt DESC")
    Page<SocialFollow> findFollowers(
        @Param("followingId") Long followingId,
        @Param("followingType") String followingType,
        Pageable pageable
    );

    /**
     * Lista following di un'entità
     */
    @Query("SELECT f FROM SocialFollow f " +
           "WHERE f.followerId = :followerId " +
           "AND f.followerType = :followerType " +
           "AND f.status = 'ACTIVE' " +
           "ORDER BY f.createdAt DESC")
    Page<SocialFollow> findFollowing(
        @Param("followerId") Long followerId,
        @Param("followerType") String followerType,
        Pageable pageable
    );

    /**
     * Conta follower
     */
    @Query("SELECT COUNT(f) FROM SocialFollow f " +
           "WHERE f.followingId = :followingId " +
           "AND f.followingType = :followingType " +
           "AND f.status = 'ACTIVE'")
    Long countFollowers(
        @Param("followingId") Long followingId,
        @Param("followingType") String followingType
    );

    /**
     * Conta following
     */
    @Query("SELECT COUNT(f) FROM SocialFollow f " +
           "WHERE f.followerId = :followerId " +
           "AND f.followerType = :followerType " +
           "AND f.status = 'ACTIVE'")
    Long countFollowing(
        @Param("followerId") Long followerId,
        @Param("followerType") String followerType
    );

    /**
     * Follower con notifiche attive (per push notifications)
     */
    @Query("SELECT f FROM SocialFollow f " +
           "WHERE f.followingId = :followingId " +
           "AND f.followingType = :followingType " +
           "AND f.status = 'ACTIVE' " +
           "AND f.notificationsEnabled = true")
    List<SocialFollow> findFollowersWithNotifications(
        @Param("followingId") Long followingId,
        @Param("followingType") String followingType
    );

    /**
     * Richieste di follow pendenti (per profili privati)
     */
    @Query("SELECT f FROM SocialFollow f " +
           "WHERE f.followingId = :followingId " +
           "AND f.followingType = :followingType " +
           "AND f.status = 'PENDING' " +
           "ORDER BY f.createdAt ASC")
    Page<SocialFollow> findPendingRequests(
        @Param("followingId") Long followingId,
        @Param("followingType") String followingType,
        Pageable pageable
    );

    /**
     * Utenti bloccati
     */
    @Query("SELECT f FROM SocialFollow f " +
           "WHERE f.followerId = :userId " +
           "AND f.followerType = :userType " +
           "AND f.status = 'BLOCKED'")
    List<SocialFollow> findBlockedByUser(
        @Param("userId") Long userId,
        @Param("userType") String userType
    );

    /**
     * Mutual followers (follower reciproci)
     */
    @Query("SELECT f1 FROM SocialFollow f1 " +
           "WHERE f1.followerId = :userId " +
           "AND f1.followerType = :userType " +
           "AND f1.status = 'ACTIVE' " +
           "AND EXISTS (SELECT 1 FROM SocialFollow f2 " +
           "            WHERE f2.followerId = f1.followingId " +
           "            AND f2.followerType = f1.followingType " +
           "            AND f2.followingId = :userId " +
           "            AND f2.followingType = :userType " +
           "            AND f2.status = 'ACTIVE')")
    Page<SocialFollow> findMutualFollows(
        @Param("userId") Long userId,
        @Param("userType") String userType,
        Pageable pageable
    );

    /**
     * Aggiorna status
     */
    @Modifying
    @Query("UPDATE SocialFollow f " +
           "SET f.status = :status, f.updatedAt = :now " +
           "WHERE f.id = :followId")
    void updateStatus(@Param("followId") Long followId, @Param("status") FollowStatus status, @Param("now") Instant now);

    /**
     * Follow di ristoranti per un customer
     */
    @Query("SELECT f FROM SocialFollow f " +
           "WHERE f.followerId = :customerId " +
           "AND f.followerType = 'CUSTOMER' " +
           "AND f.followingType = 'RESTAURANT' " +
           "AND f.status = 'ACTIVE' " +
           "ORDER BY f.createdAt DESC")
    Page<SocialFollow> findRestaurantFollowsByCustomer(
        @Param("customerId") Long customerId,
        Pageable pageable
    );
}
