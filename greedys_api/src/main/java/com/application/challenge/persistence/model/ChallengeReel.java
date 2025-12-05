package com.application.challenge.persistence.model;

import com.application.customer.persistence.model.Customer;
import com.application.restaurant.persistence.model.Restaurant;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ChallengeReel - Video reel associato a una Challenge.
 * <p>
 * I reel sono video brevi (15-90 secondi) che rimangono permanenti.
 * A differenza delle storie, non scadono e possono accumulare engagement.
 */
@Entity
@Table(name = "challenge_reel", indexes = {
    @Index(name = "idx_reel_challenge", columnList = "challenge_id"),
    @Index(name = "idx_reel_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_reel_customer", columnList = "customer_id"),
    @Index(name = "idx_reel_status", columnList = "is_active, is_approved"),
    @Index(name = "idx_reel_popular", columnList = "views_count, likes_count"),
    @Index(name = "idx_reel_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeReel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Challenge di riferimento
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    /**
     * Ristorante autore (se creato dal ristorante)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    /**
     * Cliente autore (se creato da un cliente)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // ==================== CONTENUTO ====================

    /**
     * Titolo del reel
     */
    @Column(length = 200)
    private String title;

    /**
     * URL del video
     */
    @Column(name = "video_url", nullable = false, length = 1000)
    private String videoUrl;

    /**
     * URL della thumbnail
     */
    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    /**
     * Durata in secondi
     */
    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    /**
     * Descrizione/caption
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Hashtags usati
     */
    @Column(length = 500)
    private String hashtags;

    /**
     * Musica/audio usato
     */
    @Column(name = "audio_track", length = 200)
    private String audioTrack;

    /**
     * Link esterno
     */
    @Column(name = "external_link", length = 500)
    private String externalLink;

    // ==================== STATO ====================

    /**
     * Reel attivo
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Reel in evidenza
     */
    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    /**
     * Approvato (moderazione)
     */
    @Column(name = "is_approved")
    @Builder.Default
    private Boolean isApproved = true;

    /**
     * Segnalato
     */
    @Column(name = "is_flagged")
    @Builder.Default
    private Boolean isFlagged = false;

    /**
     * Motivo segnalazione
     */
    @Column(name = "flag_reason", length = 500)
    private String flagReason;

    /**
     * Reel vincitore (della challenge)
     */
    @Column(name = "is_winner")
    @Builder.Default
    private Boolean isWinner = false;

    // ==================== STATISTICHE ====================

    /**
     * Numero visualizzazioni
     */
    @Column(name = "views_count")
    @Builder.Default
    private Integer viewsCount = 0;

    /**
     * Numero visualizzazioni complete (>90% del video)
     */
    @Column(name = "complete_views")
    @Builder.Default
    private Integer completeViews = 0;

    /**
     * Numero like
     */
    @Column(name = "likes_count")
    @Builder.Default
    private Integer likesCount = 0;

    /**
     * Numero commenti
     */
    @Column(name = "comments_count")
    @Builder.Default
    private Integer commentsCount = 0;

    /**
     * Numero condivisioni
     */
    @Column(name = "shares_count")
    @Builder.Default
    private Integer sharesCount = 0;

    /**
     * Numero salvataggi
     */
    @Column(name = "saves_count")
    @Builder.Default
    private Integer savesCount = 0;

    /**
     * Click sul link esterno
     */
    @Column(name = "link_clicks")
    @Builder.Default
    private Integer linkClicks = 0;

    /**
     * Engagement rate (calcolato)
     */
    @Column(name = "engagement_rate", precision = 5, scale = 2)
    private java.math.BigDecimal engagementRate;

    // ==================== TIMESTAMP ====================

    /**
     * Data creazione
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Data ultima modifica
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Data pubblicazione
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * Data ultima visualizzazione
     */
    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;

    // ==================== LIFECYCLE ====================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateEngagementRate();
    }

    // ==================== UTILITY ====================

    /**
     * Verifica se il reel è visibile
     */
    public boolean isVisible() {
        return Boolean.TRUE.equals(isActive) &&
               Boolean.TRUE.equals(isApproved) &&
               !Boolean.TRUE.equals(isFlagged);
    }

    /**
     * Verifica se è un reel di ristorante
     */
    public boolean isRestaurantReel() {
        return restaurant != null;
    }

    /**
     * Verifica se è un reel di cliente
     */
    public boolean isCustomerReel() {
        return customer != null;
    }

    /**
     * Incrementa visualizzazioni
     */
    public void incrementViews() {
        if (viewsCount == null) viewsCount = 0;
        viewsCount++;
        lastViewedAt = LocalDateTime.now();
    }

    /**
     * Incrementa visualizzazioni complete
     */
    public void incrementCompleteViews() {
        if (completeViews == null) completeViews = 0;
        completeViews++;
    }

    /**
     * Incrementa like
     */
    public void incrementLikes() {
        if (likesCount == null) likesCount = 0;
        likesCount++;
    }

    /**
     * Decrementa like
     */
    public void decrementLikes() {
        if (likesCount == null || likesCount <= 0) return;
        likesCount--;
    }

    /**
     * Incrementa commenti
     */
    public void incrementComments() {
        if (commentsCount == null) commentsCount = 0;
        commentsCount++;
    }

    /**
     * Decrementa commenti
     */
    public void decrementComments() {
        if (commentsCount == null || commentsCount <= 0) return;
        commentsCount--;
    }

    /**
     * Incrementa condivisioni
     */
    public void incrementShares() {
        if (sharesCount == null) sharesCount = 0;
        sharesCount++;
    }

    /**
     * Incrementa salvataggi
     */
    public void incrementSaves() {
        if (savesCount == null) savesCount = 0;
        savesCount++;
    }

    /**
     * Incrementa click link
     */
    public void incrementLinkClicks() {
        if (linkClicks == null) linkClicks = 0;
        linkClicks++;
    }

    /**
     * Calcola l'engagement rate
     * Formula: (likes + comments + shares + saves) / views * 100
     */
    public void calculateEngagementRate() {
        if (viewsCount == null || viewsCount == 0) {
            engagementRate = java.math.BigDecimal.ZERO;
            return;
        }
        int totalEngagement = (likesCount != null ? likesCount : 0) +
                             (commentsCount != null ? commentsCount : 0) +
                             (sharesCount != null ? sharesCount : 0) +
                             (savesCount != null ? savesCount : 0);
        engagementRate = java.math.BigDecimal.valueOf(totalEngagement * 100.0 / viewsCount)
                                             .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Tasso di completamento
     */
    public double getCompletionRate() {
        if (viewsCount == null || viewsCount == 0) return 0;
        return (completeViews != null ? completeViews : 0) * 100.0 / viewsCount;
    }

    /**
     * Ottiene l'autore del reel
     */
    public String getAuthorName() {
        if (restaurant != null) {
            return restaurant.getName();
        }
        if (customer != null) {
            return customer.getName() + " " + customer.getSurname();
        }
        return "Anonimo";
    }

    /**
     * Durata formattata (es. "1:30")
     */
    public String getFormattedDuration() {
        if (durationSeconds == null) return "0:00";
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Totale interazioni
     */
    public int getTotalEngagement() {
        return (likesCount != null ? likesCount : 0) +
               (commentsCount != null ? commentsCount : 0) +
               (sharesCount != null ? sharesCount : 0) +
               (savesCount != null ? savesCount : 0);
    }
}
