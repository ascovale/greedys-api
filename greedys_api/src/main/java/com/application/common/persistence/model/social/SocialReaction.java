package com.application.common.persistence.model.social;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.CreationTimestamp;

/**
 * ⭐ SOCIAL REACTION ENTITY
 * 
 * Rappresenta una reazione a un post (like, love, etc.).
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "social_reactions", 
    indexes = {
        @Index(name = "idx_reaction_post", columnList = "post_id"),
        @Index(name = "idx_reaction_comment", columnList = "comment_id"),
        @Index(name = "idx_reaction_user", columnList = "user_id"),
        @Index(name = "idx_reaction_type", columnList = "reaction_type")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_post_reaction", columnNames = {"user_id", "post_id"}),
        @UniqueConstraint(name = "uk_user_comment_reaction", columnNames = {"user_id", "comment_id"})
    }
)
public class SocialReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Post a cui è stata data la reazione
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private SocialPost post;

    /**
     * Commento a cui è stata data la reazione (alternativa a post)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private SocialComment comment;

    /**
     * Utente che ha reagito
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Tipo utente: CUSTOMER o RESTAURANT
     */
    @Column(name = "user_type", nullable = false, length = 20)
    private String userType;

    /**
     * Tipo di reazione
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 20)
    private ReactionType reactionType = ReactionType.LIKE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ==================== GETTERS/SETTERS ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SocialPost getPost() {
        return post;
    }

    public void setPost(SocialPost post) {
        this.post = post;
    }

    public SocialComment getComment() {
        return comment;
    }

    public void setComment(SocialComment comment) {
        this.comment = comment;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public ReactionType getReactionType() {
        return reactionType;
    }

    public void setReactionType(ReactionType reactionType) {
        this.reactionType = reactionType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
