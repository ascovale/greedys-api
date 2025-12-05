package com.application.common.persistence.model.social;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * ⭐ SOCIAL POST ENTITY
 * 
 * Rappresenta un post nel feed social.
 * Un post può essere creato da:
 * - Customer (recensioni, check-in, foto)
 * - Restaurant (annunci, promozioni, eventi)
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "social_posts", indexes = {
    @Index(name = "idx_post_author", columnList = "author_id, author_type"),
    @Index(name = "idx_post_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_post_visibility", columnList = "visibility"),
    @Index(name = "idx_post_type", columnList = "post_type"),
    @Index(name = "idx_post_created", columnList = "created_at DESC"),
    @Index(name = "idx_post_active", columnList = "is_active, is_deleted")
})
public class SocialPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== AUTHOR ====================
    
    /**
     * ID dell'autore (Customer o Restaurant)
     */
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    /**
     * Tipo autore: CUSTOMER o RESTAURANT
     */
    @Column(name = "author_type", nullable = false, length = 20)
    private String authorType;

    // ==================== CONTENT ====================
    
    /**
     * Testo del post
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * Tipo di post
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false, length = 20)
    private PostType postType = PostType.REGULAR;

    /**
     * Visibilità del post
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private PostVisibility visibility = PostVisibility.PUBLIC;

    // ==================== REFERENCES ====================
    
    /**
     * Ristorante taggato/menzionato (per check-in, recensioni)
     */
    @Column(name = "restaurant_id")
    private Long restaurantId;

    /**
     * Prenotazione collegata (per post auto-generati)
     */
    @Column(name = "reservation_id")
    private Long reservationId;

    /**
     * Evento collegato
     */
    @Column(name = "event_id")
    private Long eventId;

    /**
     * Post originale (se questo è una condivisione)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_from_post_id")
    private SocialPost sharedFromPost;

    // ==================== MEDIA ====================
    
    /**
     * Media allegati (immagini/video)
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SocialMedia> media = new ArrayList<>();

    // ==================== INTERACTIONS ====================
    
    /**
     * Reazioni al post
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SocialReaction> reactions = new ArrayList<>();

    /**
     * Commenti al post
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SocialComment> comments = new ArrayList<>();

    /**
     * Menzioni nel post
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SocialMention> mentions = new ArrayList<>();

    /**
     * Hashtags del post
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SocialHashtag> hashtags = new ArrayList<>();

    // ==================== COUNTERS (denormalized) ====================
    
    @Column(name = "likes_count", nullable = false)
    private Integer likesCount = 0;

    @Column(name = "comments_count", nullable = false)
    private Integer commentsCount = 0;

    @Column(name = "shares_count", nullable = false)
    private Integer sharesCount = 0;

    @Column(name = "views_count", nullable = false)
    private Integer viewsCount = 0;

    @Column(name = "saves_count", nullable = false)
    private Integer savesCount = 0;

    // ==================== LOCATION ====================
    
    @Column(name = "location_name", length = 200)
    private String locationName;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    // ==================== STATUS ====================
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned = false;

    @Column(name = "is_sponsored", nullable = false)
    private Boolean isSponsored = false;

    @Column(name = "allows_comments", nullable = false)
    private Boolean allowsComments = true;

    // ==================== TIMESTAMPS ====================
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public PostType getPostType() {
        return postType;
    }

    public void setPostType(PostType postType) {
        this.postType = postType;
    }

    public PostVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(PostVisibility visibility) {
        this.visibility = visibility;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public SocialPost getSharedFromPost() {
        return sharedFromPost;
    }

    public void setSharedFromPost(SocialPost sharedFromPost) {
        this.sharedFromPost = sharedFromPost;
    }

    public List<SocialMedia> getMedia() {
        return media;
    }

    public void setMedia(List<SocialMedia> media) {
        this.media = media;
    }

    public List<SocialReaction> getReactions() {
        return reactions;
    }

    public void setReactions(List<SocialReaction> reactions) {
        this.reactions = reactions;
    }

    public List<SocialComment> getComments() {
        return comments;
    }

    public void setComments(List<SocialComment> comments) {
        this.comments = comments;
    }

    public List<SocialMention> getMentions() {
        return mentions;
    }

    public void setMentions(List<SocialMention> mentions) {
        this.mentions = mentions;
    }

    public List<SocialHashtag> getHashtags() {
        return hashtags;
    }

    public void setHashtags(List<SocialHashtag> hashtags) {
        this.hashtags = hashtags;
    }

    public Integer getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(Integer likesCount) {
        this.likesCount = likesCount;
    }

    public Integer getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(Integer commentsCount) {
        this.commentsCount = commentsCount;
    }

    public Integer getSharesCount() {
        return sharesCount;
    }

    public void setSharesCount(Integer sharesCount) {
        this.sharesCount = sharesCount;
    }

    public Integer getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(Integer viewsCount) {
        this.viewsCount = viewsCount;
    }

    public Integer getSavesCount() {
        return savesCount;
    }

    public void setSavesCount(Integer savesCount) {
        this.savesCount = savesCount;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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

    public Boolean getIsSponsored() {
        return isSponsored;
    }

    public void setIsSponsored(Boolean isSponsored) {
        this.isSponsored = isSponsored;
    }

    public Boolean getAllowsComments() {
        return allowsComments;
    }

    public void setAllowsComments(Boolean allowsComments) {
        this.allowsComments = allowsComments;
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

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    // ==================== HELPER METHODS ====================

    public void incrementLikes() {
        this.likesCount++;
    }

    public void decrementLikes() {
        if (this.likesCount > 0) this.likesCount--;
    }

    public void incrementComments() {
        this.commentsCount++;
    }

    public void decrementComments() {
        if (this.commentsCount > 0) this.commentsCount--;
    }

    public void incrementShares() {
        this.sharesCount++;
    }

    public void incrementViews() {
        this.viewsCount++;
    }

    public void incrementSaves() {
        this.savesCount++;
    }

    public void decrementSaves() {
        if (this.savesCount > 0) this.savesCount--;
    }

    public void softDelete() {
        this.isDeleted = true;
        this.isActive = false;
        this.deletedAt = Instant.now();
    }

    public boolean isSharedPost() {
        return this.sharedFromPost != null;
    }

    public boolean hasMedia() {
        return this.media != null && !this.media.isEmpty();
    }
}
