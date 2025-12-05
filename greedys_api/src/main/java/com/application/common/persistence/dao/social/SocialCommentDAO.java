package com.application.common.persistence.dao.social;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.social.SocialComment;

/**
 * ⭐ SOCIAL COMMENT DAO
 * 
 * Repository per la gestione dei commenti.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Repository
public interface SocialCommentDAO extends JpaRepository<SocialComment, Long> {

    /**
     * Commenti top-level di un post (no replies)
     */
    @Query("SELECT c FROM SocialComment c " +
           "WHERE c.post.id = :postId " +
           "AND c.parentComment IS NULL " +
           "AND c.isDeleted = false " +
           "ORDER BY c.isPinned DESC, c.createdAt DESC")
    Page<SocialComment> findTopLevelByPostId(@Param("postId") Long postId, Pageable pageable);

    /**
     * Risposte a un commento
     */
    @Query("SELECT c FROM SocialComment c " +
           "WHERE c.parentComment.id = :parentId " +
           "AND c.isDeleted = false " +
           "ORDER BY c.createdAt ASC")
    List<SocialComment> findRepliesByParentId(@Param("parentId") Long parentId);

    /**
     * Tutti i commenti di un post (paginati)
     */
    @Query("SELECT c FROM SocialComment c " +
           "WHERE c.post.id = :postId " +
           "AND c.isDeleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<SocialComment> findByPostId(@Param("postId") Long postId, Pageable pageable);

    /**
     * Commenti di un utente
     */
    @Query("SELECT c FROM SocialComment c " +
           "WHERE c.authorId = :authorId " +
           "AND c.isDeleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<SocialComment> findByAuthorId(@Param("authorId") Long authorId, Pageable pageable);

    /**
     * Conta commenti top-level di un post
     */
    @Query("SELECT COUNT(c) FROM SocialComment c " +
           "WHERE c.post.id = :postId " +
           "AND c.parentComment IS NULL " +
           "AND c.isDeleted = false")
    Long countTopLevelByPostId(@Param("postId") Long postId);

    /**
     * Conta risposte a un commento
     */
    @Query("SELECT COUNT(c) FROM SocialComment c " +
           "WHERE c.parentComment.id = :parentId " +
           "AND c.isDeleted = false")
    Long countRepliesByParentId(@Param("parentId") Long parentId);

    /**
     * Commenti pinnati
     */
    @Query("SELECT c FROM SocialComment c " +
           "WHERE c.post.id = :postId " +
           "AND c.isPinned = true " +
           "AND c.isDeleted = false")
    List<SocialComment> findPinnedByPostId(@Param("postId") Long postId);

    /**
     * Soft delete
     */
    @Modifying
    @Query("UPDATE SocialComment c " +
           "SET c.isDeleted = true, c.deletedAt = :now, c.content = '[Commento eliminato]' " +
           "WHERE c.id = :commentId")
    void softDelete(@Param("commentId") Long commentId, @Param("now") Instant now);

    /**
     * Incrementa like count
     */
    @Modifying
    @Query("UPDATE SocialComment c SET c.likesCount = c.likesCount + 1 WHERE c.id = :commentId")
    void incrementLikes(@Param("commentId") Long commentId);

    /**
     * Decrementa like count
     */
    @Modifying
    @Query("UPDATE SocialComment c SET c.likesCount = GREATEST(0, c.likesCount - 1) WHERE c.id = :commentId")
    void decrementLikes(@Param("commentId") Long commentId);

    /**
     * Incrementa replies count
     */
    @Modifying
    @Query("UPDATE SocialComment c SET c.repliesCount = c.repliesCount + 1 WHERE c.id = :commentId")
    void incrementReplies(@Param("commentId") Long commentId);

    /**
     * Decrementa replies count
     */
    @Modifying
    @Query("UPDATE SocialComment c SET c.repliesCount = GREATEST(0, c.repliesCount - 1) WHERE c.id = :commentId")
    void decrementReplies(@Param("commentId") Long commentId);
}
