package com.application.common.persistence.dao.social;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.social.ReactionType;
import com.application.common.persistence.model.social.SocialReaction;

/**
 * ⭐ SOCIAL REACTION DAO
 * 
 * Repository per la gestione delle reazioni (like, love, etc.).
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Repository
public interface SocialReactionDAO extends JpaRepository<SocialReaction, Long> {

    /**
     * Trova reazione utente su un post
     */
    @Query("SELECT r FROM SocialReaction r " +
           "WHERE r.post.id = :postId " +
           "AND r.userId = :userId")
    Optional<SocialReaction> findByPostIdAndUserId(
        @Param("postId") Long postId, 
        @Param("userId") Long userId
    );

    /**
     * Trova reazione utente su un commento
     */
    @Query("SELECT r FROM SocialReaction r " +
           "WHERE r.comment.id = :commentId " +
           "AND r.userId = :userId")
    Optional<SocialReaction> findByCommentIdAndUserId(
        @Param("commentId") Long commentId, 
        @Param("userId") Long userId
    );

    /**
     * Tutte le reazioni a un post
     */
    @Query("SELECT r FROM SocialReaction r " +
           "WHERE r.post.id = :postId " +
           "ORDER BY r.createdAt DESC")
    Page<SocialReaction> findByPostId(@Param("postId") Long postId, Pageable pageable);

    /**
     * Conta reazioni per tipo su un post
     */
    @Query("SELECT r.reactionType, COUNT(r) FROM SocialReaction r " +
           "WHERE r.post.id = :postId " +
           "GROUP BY r.reactionType")
    List<Object[]> countByPostIdGroupByType(@Param("postId") Long postId);

    /**
     * Conta totale reazioni a un post
     */
    Long countByPostId(Long postId);

    /**
     * Conta totale reazioni a un commento
     */
    Long countByCommentId(Long commentId);

    /**
     * Verifica se utente ha reagito a un post
     */
    @Query("SELECT COUNT(r) > 0 FROM SocialReaction r " +
           "WHERE r.post.id = :postId AND r.userId = :userId")
    boolean hasUserReactedToPost(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * Utenti che hanno reagito (per "Piace a X, Y e altri...")
     */
    @Query("SELECT r FROM SocialReaction r " +
           "WHERE r.post.id = :postId " +
           "ORDER BY r.createdAt DESC")
    List<SocialReaction> findRecentByPostId(@Param("postId") Long postId, Pageable pageable);

    /**
     * Reazioni di un utente (per profilo)
     */
    @Query("SELECT r FROM SocialReaction r " +
           "WHERE r.userId = :userId " +
           "ORDER BY r.createdAt DESC")
    Page<SocialReaction> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Elimina tutte le reazioni di un post
     */
    @Modifying
    @Query("DELETE FROM SocialReaction r WHERE r.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    /**
     * Elimina tutte le reazioni di un commento
     */
    @Modifying
    @Query("DELETE FROM SocialReaction r WHERE r.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") Long commentId);

    /**
     * Trova reazioni per tipo
     */
    @Query("SELECT r FROM SocialReaction r " +
           "WHERE r.post.id = :postId " +
           "AND r.reactionType = :reactionType")
    List<SocialReaction> findByPostIdAndReactionType(
        @Param("postId") Long postId, 
        @Param("reactionType") ReactionType reactionType
    );
}
