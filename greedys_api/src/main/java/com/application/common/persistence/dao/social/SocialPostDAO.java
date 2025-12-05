package com.application.common.persistence.dao.social;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.social.PostType;
import com.application.common.persistence.model.social.PostVisibility;
import com.application.common.persistence.model.social.SocialPost;

/**
 * ⭐ SOCIAL POST DAO
 * 
 * Repository per la gestione dei post social.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Repository
public interface SocialPostDAO extends JpaRepository<SocialPost, Long> {

    /**
     * Trova post di un autore (paginati)
     */
    @Query("SELECT p FROM SocialPost p " +
           "WHERE p.authorId = :authorId " +
           "AND p.authorType = :authorType " +
           "AND p.isDeleted = false " +
           "AND p.isActive = true " +
           "ORDER BY p.createdAt DESC")
    Page<SocialPost> findByAuthor(
        @Param("authorId") Long authorId, 
        @Param("authorType") String authorType,
        Pageable pageable
    );

    /**
     * Trova post pubblici (per feed discovery)
     */
    @Query("SELECT p FROM SocialPost p " +
           "WHERE p.visibility = :visibility " +
           "AND p.isDeleted = false " +
           "AND p.isActive = true " +
           "ORDER BY p.createdAt DESC")
    Page<SocialPost> findByVisibility(@Param("visibility") PostVisibility visibility, Pageable pageable);

    /**
     * Trova post per ristorante (check-in, recensioni)
     */
    @Query("SELECT p FROM SocialPost p " +
           "WHERE p.restaurantId = :restaurantId " +
           "AND p.isDeleted = false " +
           "AND p.isActive = true " +
           "ORDER BY p.createdAt DESC")
    Page<SocialPost> findByRestaurantId(@Param("restaurantId") Long restaurantId, Pageable pageable);

    /**
     * Trova post per tipo
     */
    @Query("SELECT p FROM SocialPost p " +
           "WHERE p.postType = :postType " +
           "AND p.isDeleted = false " +
           "AND p.isActive = true " +
           "ORDER BY p.createdAt DESC")
    Page<SocialPost> findByPostType(@Param("postType") PostType postType, Pageable pageable);

    /**
     * Feed personalizzato (post degli utenti seguiti)
     */
    @Query("SELECT p FROM SocialPost p " +
           "WHERE (p.authorId, p.authorType) IN " +
           "(SELECT f.followingId, f.followingType FROM SocialFollow f " +
           " WHERE f.followerId = :userId AND f.followerType = :userType AND f.status = 'ACTIVE' AND f.showInFeed = true) " +
           "AND p.isDeleted = false " +
           "AND p.isActive = true " +
           "AND (p.visibility = 'PUBLIC' OR p.visibility = 'FOLLOWERS_ONLY') " +
           "ORDER BY p.createdAt DESC")
    Page<SocialPost> findFeedForUser(
        @Param("userId") Long userId, 
        @Param("userType") String userType,
        Pageable pageable
    );

    /**
     * Post trending (più engagement recente)
     */
    @Query("SELECT p FROM SocialPost p " +
           "WHERE p.visibility = 'PUBLIC' " +
           "AND p.isDeleted = false " +
           "AND p.isActive = true " +
           "AND p.createdAt >= :since " +
           "ORDER BY (p.likesCount + p.commentsCount * 2 + p.sharesCount * 3) DESC")
    Page<SocialPost> findTrending(@Param("since") java.time.Instant since, Pageable pageable);

    /**
     * Post pinnati di un ristorante
     */
    @Query("SELECT p FROM SocialPost p " +
           "WHERE p.authorId = :restaurantId " +
           "AND p.authorType = 'RESTAURANT' " +
           "AND p.isPinned = true " +
           "AND p.isDeleted = false " +
           "ORDER BY p.createdAt DESC")
    List<SocialPost> findPinnedByRestaurant(@Param("restaurantId") Long restaurantId);

    /**
     * Post sponsorizzati
     */
    @Query("SELECT p FROM SocialPost p " +
           "WHERE p.isSponsored = true " +
           "AND p.isDeleted = false " +
           "AND p.isActive = true " +
           "ORDER BY RAND()")
    Page<SocialPost> findSponsored(Pageable pageable);

    /**
     * Cerca post per contenuto
     */
    @Query("SELECT p FROM SocialPost p " +
           "WHERE p.isDeleted = false " +
           "AND p.isActive = true " +
           "AND p.visibility = 'PUBLIC' " +
           "AND LOWER(p.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY p.createdAt DESC")
    Page<SocialPost> searchByContent(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Conta post per ristorante
     */
    @Query("SELECT COUNT(p) FROM SocialPost p " +
           "WHERE p.restaurantId = :restaurantId " +
           "AND p.isDeleted = false")
    Long countByRestaurantId(@Param("restaurantId") Long restaurantId);

    /**
     * Incrementa views
     */
    @Modifying
    @Query("UPDATE SocialPost p SET p.viewsCount = p.viewsCount + 1 WHERE p.id = :postId")
    void incrementViews(@Param("postId") Long postId);

    /**
     * Incrementa likes
     */
    @Modifying
    @Query("UPDATE SocialPost p SET p.likesCount = p.likesCount + 1 WHERE p.id = :postId")
    void incrementLikes(@Param("postId") Long postId);

    /**
     * Decrementa likes
     */
    @Modifying
    @Query("UPDATE SocialPost p SET p.likesCount = GREATEST(0, p.likesCount - 1) WHERE p.id = :postId")
    void decrementLikes(@Param("postId") Long postId);

    /**
     * Post collegati a un evento
     */
    @Query("SELECT p FROM SocialPost p " +
           "WHERE p.eventId = :eventId " +
           "AND p.isDeleted = false")
    List<SocialPost> findByEventId(@Param("eventId") Long eventId);
}
