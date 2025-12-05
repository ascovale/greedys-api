package com.application.challenge.persistence.model;

import com.application.challenge.persistence.model.enums.ContentType;
import com.application.customer.persistence.model.Customer;
import com.application.restaurant.persistence.model.Restaurant;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ChallengeStory - Storia/contenuto temporaneo associato a una Challenge.
 * <p>
 * Le storie sono contenuti brevi (immagine o video) che scadono dopo 24h.
 * Possono essere create da ristoranti (per promuovere la partecipazione)
 * o da clienti (per mostrare la loro esperienza).
 */
@Entity
@Table(name = "challenge_story", indexes = {
    @Index(name = "idx_story_challenge", columnList = "challenge_id"),
    @Index(name = "idx_story_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_story_customer", columnList = "customer_id"),
    @Index(name = "idx_story_active", columnList = "is_active, expires_at"),
    @Index(name = "idx_story_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeStory {

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
     * Tipo di contenuto
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentType contentType;

    /**
     * URL del media (immagine o video)
     */
    @Column(name = "media_url", nullable = false, length = 1000)
    private String mediaUrl;

    /**
     * URL della thumbnail (per video)
     */
    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    /**
     * Durata in secondi (per video)
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * Testo/caption
     */
    @Column(length = 500)
    private String caption;

    /**
     * Hashtags usati
     */
    @Column(length = 300)
    private String hashtags;

    /**
     * Link esterno (swipe up)
     */
    @Column(name = "external_link", length = 500)
    private String externalLink;

    // ==================== STATO ====================

    /**
     * Storia attiva
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Storia in evidenza (pinned)
     */
    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    /**
     * Approvata (moderazione)
     */
    @Column(name = "is_approved")
    @Builder.Default
    private Boolean isApproved = true;

    /**
     * Segnalata
     */
    @Column(name = "is_flagged")
    @Builder.Default
    private Boolean isFlagged = false;

    /**
     * Motivo segnalazione
     */
    @Column(name = "flag_reason", length = 500)
    private String flagReason;

    // ==================== STATISTICHE ====================

    /**
     * Numero visualizzazioni
     */
    @Column(name = "views_count")
    @Builder.Default
    private Integer viewsCount = 0;

    /**
     * Numero like
     */
    @Column(name = "likes_count")
    @Builder.Default
    private Integer likesCount = 0;

    /**
     * Numero condivisioni
     */
    @Column(name = "shares_count")
    @Builder.Default
    private Integer sharesCount = 0;

    /**
     * Numero click sul link esterno
     */
    @Column(name = "link_clicks")
    @Builder.Default
    private Integer linkClicks = 0;

    // ==================== TIMESTAMP ====================

    /**
     * Data creazione
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Data scadenza (24h dopo creazione di default)
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

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
        if (expiresAt == null) {
            // Default: scade dopo 24 ore
            expiresAt = createdAt.plusHours(24);
        }
    }

    // ==================== UTILITY ====================

    /**
     * Verifica se la storia è scaduta
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Verifica se la storia è visibile
     */
    public boolean isVisible() {
        return Boolean.TRUE.equals(isActive) && 
               Boolean.TRUE.equals(isApproved) &&
               !Boolean.TRUE.equals(isFlagged) &&
               !isExpired();
    }

    /**
     * Verifica se è una storia di ristorante
     */
    public boolean isRestaurantStory() {
        return restaurant != null;
    }

    /**
     * Verifica se è una storia di cliente
     */
    public boolean isCustomerStory() {
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
     * Incrementa condivisioni
     */
    public void incrementShares() {
        if (sharesCount == null) sharesCount = 0;
        sharesCount++;
    }

    /**
     * Incrementa click link
     */
    public void incrementLinkClicks() {
        if (linkClicks == null) linkClicks = 0;
        linkClicks++;
    }

    /**
     * Tempo rimanente prima della scadenza
     */
    public java.time.Duration getTimeRemaining() {
        if (isExpired()) {
            return java.time.Duration.ZERO;
        }
        return java.time.Duration.between(LocalDateTime.now(), expiresAt);
    }

    /**
     * Ottiene l'autore della storia
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
}
