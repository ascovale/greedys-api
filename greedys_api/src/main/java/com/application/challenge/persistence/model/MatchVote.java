package com.application.challenge.persistence.model;

import com.application.challenge.persistence.model.enums.VoterType;
import com.application.customer.persistence.model.Customer;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.restaurant.persistence.model.Restaurant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MatchVote - Voto di un utente in un match di torneo.
 * <p>
 * Ogni utente può votare una sola volta per match.
 * Il voto è pesato in base al tipo di votante (LOCAL, TOURIST, etc.)
 * e può essere verificato tramite prenotazione "seated".
 */
@Entity
@Table(name = "match_vote", 
    indexes = {
        @Index(name = "idx_match_vote_match", columnList = "match_id"),
        @Index(name = "idx_match_vote_customer", columnList = "customer_id"),
        @Index(name = "idx_match_vote_unique", columnList = "match_id, customer_id", unique = true)
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_match_vote_customer",
            columnNames = {"match_id", "customer_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Match votato
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private TournamentMatch match;

    /**
     * Cliente che ha votato
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /**
     * Ristorante votato (restaurant1 o restaurant2 del match)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voted_restaurant_id", nullable = false)
    private Restaurant votedRestaurant;

    // ==================== VERIFICA E PESO ====================

    /**
     * Tipo di votante (calcolato al momento del voto)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "voter_type", nullable = false, length = 20)
    private VoterType voterType;

    /**
     * Peso del voto (derivato da voterType)
     */
    @Column(name = "vote_weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal voteWeight;

    /**
     * Voto verificato tramite prenotazione
     */
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    /**
     * Prenotazione che verifica il voto (seated presso votedRestaurant)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verifying_reservation_id")
    private Reservation verifyingReservation;

    /**
     * Data della visita verificata
     */
    @Column(name = "visit_date")
    private LocalDateTime visitDate;

    // ==================== GEOLOCALIZZAZIONE ====================

    /**
     * Latitudine dell'utente al momento del voto
     */
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    /**
     * Longitudine dell'utente al momento del voto
     */
    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    /**
     * Distanza dal ristorante votato (km)
     */
    @Column(name = "distance_km", precision = 8, scale = 2)
    private BigDecimal distanceKm;

    // ==================== METADATI ====================

    /**
     * Timestamp del voto
     */
    @Column(name = "voted_at", nullable = false, updatable = false)
    private LocalDateTime votedAt;

    /**
     * IP address (per audit/antifrode)
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent (per audit/antifrode)
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Device ID (per prevenire voti multipli)
     */
    @Column(name = "device_id", length = 100)
    private String deviceId;

    // ==================== LIFECYCLE ====================

    @PrePersist
    protected void onCreate() {
        if (votedAt == null) {
            votedAt = LocalDateTime.now();
        }
        if (voteWeight == null && voterType != null) {
            voteWeight = voterType.getVoteWeight();
        }
    }

    // ==================== UTILITY ====================

    /**
     * Verifica se il voto è per restaurant1 del match
     */
    public boolean isVoteForRestaurant1() {
        return match != null && 
               match.getRestaurant1() != null && 
               votedRestaurant != null &&
               match.getRestaurant1().getId().equals(votedRestaurant.getId());
    }

    /**
     * Verifica se il voto è per restaurant2 del match
     */
    public boolean isVoteForRestaurant2() {
        return match != null && 
               match.getRestaurant2() != null && 
               votedRestaurant != null &&
               match.getRestaurant2().getId().equals(votedRestaurant.getId());
    }

    /**
     * Verifica se il voto è locale (entro raggio)
     */
    public boolean isLocalVote() {
        return voterType == VoterType.LOCAL;
    }

    /**
     * Verifica se il voto ha peso maggiorato
     */
    public boolean hasBonus() {
        return voteWeight != null && 
               voteWeight.compareTo(BigDecimal.ONE) > 0;
    }

    /**
     * Ritorna il peso effettivo (verificato o base)
     */
    public BigDecimal getEffectiveWeight() {
        if (Boolean.TRUE.equals(isVerified)) {
            // Voto verificato: peso base + 50% bonus
            return voteWeight.multiply(new BigDecimal("1.5"));
        }
        return voteWeight;
    }
}
