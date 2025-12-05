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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

/**
 * ⭐ SOCIAL STORY ENTITY
 * 
 * Rappresenta una story (contenuto temporaneo 24h).
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "social_stories", indexes = {
    @Index(name = "idx_story_author", columnList = "author_id, author_type"),
    @Index(name = "idx_story_expires", columnList = "expires_at"),
    @Index(name = "idx_story_active", columnList = "is_active")
})
public class SocialStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== AUTHOR ====================
    
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_type", nullable = false, length = 20)
    private String authorType;

    // ==================== MEDIA ====================
    
    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SocialMedia> media = new ArrayList<>();

    // ==================== CONTENT ====================
    
    @Column(name = "text_overlay", length = 500)
    private String textOverlay;

    @Column(name = "background_color", length = 20)
    private String backgroundColor;

    @Column(name = "font_style", length = 50)
    private String fontStyle;

    // ==================== LINK ====================
    
    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "link_text", length = 100)
    private String linkText;

    // ==================== REFERENCES ====================
    
    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(name = "event_id")
    private Long eventId;

    // ==================== COUNTERS ====================
    
    @Column(name = "views_count", nullable = false)
    private Integer viewsCount = 0;

    @Column(name = "reactions_count", nullable = false)
    private Integer reactionsCount = 0;

    @Column(name = "replies_count", nullable = false)
    private Integer repliesCount = 0;

    // ==================== STATUS ====================
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_highlight", nullable = false)
    private Boolean isHighlight = false;

    @Column(name = "highlight_name", length = 100)
    private String highlightName;

    // ==================== TIMESTAMPS ====================
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    // ==================== GETTERS/SETTERS ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public List<SocialMedia> getMedia() {
        return media;
    }

    public void setMedia(List<SocialMedia> media) {
        this.media = media;
    }

    public String getTextOverlay() {
        return textOverlay;
    }

    public void setTextOverlay(String textOverlay) {
        this.textOverlay = textOverlay;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getFontStyle() {
        return fontStyle;
    }

    public void setFontStyle(String fontStyle) {
        this.fontStyle = fontStyle;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getLinkText() {
        return linkText;
    }

    public void setLinkText(String linkText) {
        this.linkText = linkText;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Integer getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(Integer viewsCount) {
        this.viewsCount = viewsCount;
    }

    public Integer getReactionsCount() {
        return reactionsCount;
    }

    public void setReactionsCount(Integer reactionsCount) {
        this.reactionsCount = reactionsCount;
    }

    public Integer getRepliesCount() {
        return repliesCount;
    }

    public void setRepliesCount(Integer repliesCount) {
        this.repliesCount = repliesCount;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsHighlight() {
        return isHighlight;
    }

    public void setIsHighlight(Boolean isHighlight) {
        this.isHighlight = isHighlight;
    }

    public String getHighlightName() {
        return highlightName;
    }

    public void setHighlightName(String highlightName) {
        this.highlightName = highlightName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    // ==================== HELPER METHODS ====================

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void incrementViews() {
        this.viewsCount++;
    }

    public void incrementReactions() {
        this.reactionsCount++;
    }

    public void incrementReplies() {
        this.repliesCount++;
    }
}
