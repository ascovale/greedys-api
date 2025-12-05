package com.application.challenge.persistence.model;

import com.application.challenge.persistence.model.enums.VoterType;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.customer.persistence.model.Customer;
import com.application.restaurant.persistence.model.Restaurant;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * RankingVote - Voto di un cliente per un ristorante.
 * <p>
 * Ogni voto è collegato a una prenotazione SEATED.
 * Include rating 1-5 e categorie opzionali.
 */
@Entity
@Table(name = "ranking_vote", indexes = {
    @Index(name = "idx_vote_restaurant", columnList = "restaurant_id, voted_at"),
    @Index(name = "idx_vote_customer", columnList = "voter_id, restaurant_id"),
    @Index(name = "idx_vote_reservation", columnList = "reservation_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Cliente che ha votato
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private Customer voter;

    /**
     * Ristorante votato
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    /**
     * Prenotazione che abilita il voto (DEVE essere SEATED)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    /**
     * Ranking in cui il voto è conteggiato (opzionale)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ranking_id")
    private Ranking ranking;

    /**
     * Tipo di votante (LOCAL, TOURIST, VERIFIED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "voter_type", nullable = false, length = 20)
    private VoterType voterType;

    // ==================== RATING ====================

    /**
     * Rating generale (1-5 stelle)
     */
    @Column(nullable = false)
    private Integer rating;

    /**
     * Rating cibo (opzionale)
     */
    @Column(name = "food_rating")
    private Integer foodRating;

    /**
     * Rating servizio (opzionale)
     */
    @Column(name = "service_rating")
    private Integer serviceRating;

    /**
     * Rating ambiente (opzionale)
     */
    @Column(name = "ambience_rating")
    private Integer ambienceRating;

    /**
     * Rating rapporto qualità/prezzo (opzionale)
     */
    @Column(name = "value_rating")
    private Integer valueRating;

    // ==================== COMMENTO ====================

    /**
     * Recensione testuale (opzionale)
     */
    @Column(columnDefinition = "TEXT")
    private String comment;

    // ==================== TIMESTAMP ====================

    /**
     * Data e ora del voto
     */
    @Column(name = "voted_at", nullable = false)
    private LocalDateTime votedAt;

    // ==================== ANTI-FRAUD ====================

    /**
     * Indirizzo IP del votante
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Device fingerprint per rilevare account multipli
     */
    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    /**
     * Voto verificato manualmente (se sospetto)
     */
    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    /**
     * Flag se rilevato come potenziale fraud
     */
    @Column(name = "is_suspicious")
    @Builder.Default
    private Boolean isSuspicious = false;

    /**
     * Motivo del flag suspicious
     */
    @Column(name = "suspicious_reason", length = 255)
    private String suspiciousReason;

    // ==================== SCORE & WEIGHT ====================

    /**
     * Score calcolato per il ranking (può differire dal rating raw)
     */
    @Column(name = "score", precision = 10, scale = 4)
    private java.math.BigDecimal score;

    /**
     * Peso del voto (es: voti verificati pesano di più)
     */
    @Column(name = "vote_weight", precision = 5, scale = 2)
    @Builder.Default
    private java.math.BigDecimal voteWeight = java.math.BigDecimal.ONE;

    /**
     * Categoria votata (per ranking per categoria)
     */
    @Column(name = "category_voted", length = 50)
    private String categoryVoted;

    // ==================== LIFECYCLE ====================

    @PrePersist
    protected void onCreate() {
        if (votedAt == null) {
            votedAt = LocalDateTime.now();
        }
    }

    // ==================== UTILITY ====================

    /**
     * Verifica se tutti i rating dettagliati sono stati compilati
     */
    public boolean hasDetailedRatings() {
        return foodRating != null && serviceRating != null && 
               ambienceRating != null && valueRating != null;
    }

    /**
     * Calcola la media dei rating dettagliati
     */
    public Double getDetailedAverage() {
        if (!hasDetailedRatings()) {
            return null;
        }
        return (foodRating + serviceRating + ambienceRating + valueRating) / 4.0;
    }

    /**
     * Verifica se il voto è recente (ultimi 30 giorni)
     */
    public boolean isRecent() {
        return votedAt != null && 
               votedAt.isAfter(LocalDateTime.now().minusDays(30));
    }

    /**
     * Verifica se il voto è valido per il calcolo ranking
     */
    public boolean isValidForRanking() {
        return !isSuspicious && rating != null && rating >= 1 && rating <= 5;
    }
}
