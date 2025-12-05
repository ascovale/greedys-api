package com.application.common.persistence.model.social;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

/**
 * ⭐ SOCIAL MENTION ENTITY
 * 
 * Rappresenta una menzione (@) in un post o commento.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "social_mentions", indexes = {
    @Index(name = "idx_mention_post", columnList = "post_id"),
    @Index(name = "idx_mention_user", columnList = "mentioned_user_id"),
    @Index(name = "idx_mention_restaurant", columnList = "mentioned_restaurant_id")
})
public class SocialMention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Post in cui appare la menzione
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private SocialPost post;

    /**
     * Commento in cui appare la menzione
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private SocialComment comment;

    /**
     * ID dell'utente menzionato (se Customer)
     */
    @Column(name = "mentioned_user_id")
    private Long mentionedUserId;

    /**
     * ID del ristorante menzionato
     */
    @Column(name = "mentioned_restaurant_id")
    private Long mentionedRestaurantId;

    /**
     * Testo originale della menzione (es. @nomeristorante)
     */
    @Column(name = "mention_text", nullable = false, length = 100)
    private String mentionText;

    /**
     * Posizione nel testo (per highlight)
     */
    @Column(name = "start_position")
    private Integer startPosition;

    @Column(name = "end_position")
    private Integer endPosition;

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

    public Long getMentionedUserId() {
        return mentionedUserId;
    }

    public void setMentionedUserId(Long mentionedUserId) {
        this.mentionedUserId = mentionedUserId;
    }

    public Long getMentionedRestaurantId() {
        return mentionedRestaurantId;
    }

    public void setMentionedRestaurantId(Long mentionedRestaurantId) {
        this.mentionedRestaurantId = mentionedRestaurantId;
    }

    public String getMentionText() {
        return mentionText;
    }

    public void setMentionText(String mentionText) {
        this.mentionText = mentionText;
    }

    public Integer getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(Integer startPosition) {
        this.startPosition = startPosition;
    }

    public Integer getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(Integer endPosition) {
        this.endPosition = endPosition;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // ==================== HELPER METHODS ====================

    public boolean isUserMention() {
        return mentionedUserId != null;
    }

    public boolean isRestaurantMention() {
        return mentionedRestaurantId != null;
    }
}
