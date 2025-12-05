package com.application.common.persistence.model.social;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * ⭐ SOCIAL COMMENT ENTITY
 * 
 * Rappresenta un commento a un post.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "social_comments", indexes = {
    @Index(name = "idx_comment_post", columnList = "post_id"),
    @Index(name = "idx_comment_author", columnList = "author_id"),
    @Index(name = "idx_comment_parent", columnList = "parent_comment_id"),
    @Index(name = "idx_comment_created", columnList = "created_at DESC")
})
public class SocialComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Post a cui appartiene il commento
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private SocialPost post;

    /**
     * Commento padre (per thread/risposte)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private SocialComment parentComment;

    /**
     * Risposte a questo commento
     */
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SocialComment> replies = new ArrayList<>();

    // ==================== AUTHOR ====================
    
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_type", nullable = false, length = 20)
    private String authorType;

    // ==================== CONTENT ====================
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // ==================== REACTIONS ====================
    
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SocialReaction> reactions = new ArrayList<>();

    @Column(name = "likes_count", nullable = false)
    private Integer likesCount = 0;

    @Column(name = "replies_count", nullable = false)
    private Integer repliesCount = 0;

    // ==================== STATUS ====================
    
    @Column(name = "is_edited", nullable = false)
    private Boolean isEdited = false;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned = false;

    // ==================== TIMESTAMPS ====================
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

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

    public SocialComment getParentComment() {
        return parentComment;
    }

    public void setParentComment(SocialComment parentComment) {
        this.parentComment = parentComment;
    }

    public List<SocialComment> getReplies() {
        return replies;
    }

    public void setReplies(List<SocialComment> replies) {
        this.replies = replies;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getAuthorType() {
        return authorType;
    }

    public void setAuthorType(String authorType) {
        this.authorType = authorType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<SocialReaction> getReactions() {
        return reactions;
    }

    public void setReactions(List<SocialReaction> reactions) {
        this.reactions = reactions;
    }

    public Integer getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(Integer likesCount) {
        this.likesCount = likesCount;
    }

    public Integer getRepliesCount() {
        return repliesCount;
    }

    public void setRepliesCount(Integer repliesCount) {
        this.repliesCount = repliesCount;
    }

    public Boolean getIsEdited() {
        return isEdited;
    }

    public void setIsEdited(Boolean isEdited) {
        this.isEdited = isEdited;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Boolean getIsPinned() {
        return isPinned;
    }

    public void setIsPinned(Boolean isPinned) {
        this.isPinned = isPinned;
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

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    // ==================== HELPER METHODS ====================

    public void incrementLikes() {
        this.likesCount++;
    }

    public void decrementLikes() {
        if (this.likesCount > 0) this.likesCount--;
    }

    public void incrementReplies() {
        this.repliesCount++;
    }

    public void decrementReplies() {
        if (this.repliesCount > 0) this.repliesCount--;
    }

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = Instant.now();
        this.content = "[Commento eliminato]";
    }

    public boolean isTopLevel() {
        return this.parentComment == null;
    }
}
