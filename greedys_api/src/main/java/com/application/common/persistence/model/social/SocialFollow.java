package com.application.common.persistence.model.social;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * ⭐ SOCIAL FOLLOW ENTITY
 * 
 * Rappresenta una relazione di follow tra utenti o verso ristoranti.
 * 
 * Possibili relazioni:
 * - Customer segue Restaurant
 * - Customer segue Customer
 * - Restaurant segue Restaurant
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "social_follows", 
    indexes = {
        @Index(name = "idx_follow_follower", columnList = "follower_id, follower_type"),
        @Index(name = "idx_follow_following", columnList = "following_id, following_type"),
        @Index(name = "idx_follow_status", columnList = "status"),
        @Index(name = "idx_follow_created", columnList = "created_at DESC")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_follow_relation", 
            columnNames = {"follower_id", "follower_type", "following_id", "following_type"})
    }
)
public class SocialFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== FOLLOWER ====================
    
    /**
     * ID di chi segue
     */
    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    /**
     * Tipo di chi segue: CUSTOMER o RESTAURANT
     */
    @Column(name = "follower_type", nullable = false, length = 20)
    private String followerType;

    // ==================== FOLLOWING ====================
    
    /**
     * ID di chi viene seguito
     */
    @Column(name = "following_id", nullable = false)
    private Long followingId;

    /**
     * Tipo di chi viene seguito: CUSTOMER o RESTAURANT
     */
    @Column(name = "following_type", nullable = false, length = 20)
    private String followingType;

    // ==================== STATUS ====================
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FollowStatus status = FollowStatus.ACTIVE;

    /**
     * Notifiche attivate per questo follow
     */
    @Column(name = "notifications_enabled", nullable = false)
    private Boolean notificationsEnabled = true;

    /**
     * Mostra nel feed
     */
    @Column(name = "show_in_feed", nullable = false)
    private Boolean showInFeed = true;

    // ==================== TIMESTAMPS ====================
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "unfollowed_at")
    private Instant unfollowedAt;

    // ==================== GETTERS/SETTERS ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFollowerId() {
        return followerId;
    }

    public void setFollowerId(Long followerId) {
        this.followerId = followerId;
    }

    public String getFollowerType() {
        return followerType;
    }

    public void setFollowerType(String followerType) {
        this.followerType = followerType;
    }

    public Long getFollowingId() {
        return followingId;
    }

    public void setFollowingId(Long followingId) {
        this.followingId = followingId;
    }

    public String getFollowingType() {
        return followingType;
    }

    public void setFollowingType(String followingType) {
        this.followingType = followingType;
    }

    public FollowStatus getStatus() {
        return status;
    }

    public void setStatus(FollowStatus status) {
        this.status = status;
    }

    public Boolean getNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(Boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public Boolean getShowInFeed() {
        return showInFeed;
    }

    public void setShowInFeed(Boolean showInFeed) {
        this.showInFeed = showInFeed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getUnfollowedAt() {
        return unfollowedAt;
    }

    public void setUnfollowedAt(Instant unfollowedAt) {
        this.unfollowedAt = unfollowedAt;
    }

    // ==================== HELPER METHODS ====================

    public boolean isActive() {
        return status == FollowStatus.ACTIVE;
    }

    public boolean isPending() {
        return status == FollowStatus.PENDING;
    }

    public boolean isBlocked() {
        return status == FollowStatus.BLOCKED;
    }

    public void unfollow() {
        this.status = FollowStatus.REMOVED;
        this.unfollowedAt = Instant.now();
    }

    public void refollow() {
        this.status = FollowStatus.ACTIVE;
        this.unfollowedAt = null;
    }
}
