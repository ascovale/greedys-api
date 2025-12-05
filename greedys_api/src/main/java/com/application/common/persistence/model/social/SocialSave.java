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
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.CreationTimestamp;

/**
 * ⭐ SOCIAL SAVE ENTITY
 * 
 * Rappresenta un post salvato/preferito da un utente.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "social_saves", 
    indexes = {
        @Index(name = "idx_save_user", columnList = "user_id"),
        @Index(name = "idx_save_post", columnList = "post_id"),
        @Index(name = "idx_save_collection", columnList = "collection_name"),
        @Index(name = "idx_save_created", columnList = "created_at DESC")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_post_save", columnNames = {"user_id", "post_id"})
    }
)
public class SocialSave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Post salvato
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private SocialPost post;

    /**
     * Utente che ha salvato
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Tipo utente
     */
    @Column(name = "user_type", nullable = false, length = 20)
    private String userType;

    /**
     * Nome della raccolta (per organizzare i salvati)
     */
    @Column(name = "collection_name", length = 100)
    private String collectionName;

    /**
     * Note personali
     */
    @Column(name = "notes", length = 500)
    private String notes;

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

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
