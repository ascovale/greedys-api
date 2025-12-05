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
 * ⭐ SOCIAL HASHTAG ENTITY
 * 
 * Rappresenta un hashtag (#) in un post.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "social_hashtags", indexes = {
    @Index(name = "idx_hashtag_post", columnList = "post_id"),
    @Index(name = "idx_hashtag_tag", columnList = "tag_name"),
    @Index(name = "idx_hashtag_trending", columnList = "usage_count DESC")
})
public class SocialHashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Post in cui appare l'hashtag
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private SocialPost post;

    /**
     * Nome dell'hashtag (senza #)
     */
    @Column(name = "tag_name", nullable = false, length = 100)
    private String tagName;

    /**
     * Nome normalizzato (lowercase, per ricerche)
     */
    @Column(name = "tag_normalized", nullable = false, length = 100)
    private String tagNormalized;

    /**
     * Contatore utilizzo (denormalizzato per trending)
     */
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 1;

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

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
        this.tagNormalized = tagName != null ? tagName.toLowerCase() : null;
    }

    public String getTagNormalized() {
        return tagNormalized;
    }

    public void setTagNormalized(String tagNormalized) {
        this.tagNormalized = tagNormalized;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
