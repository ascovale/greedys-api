package com.application.challenge.persistence.model;

import com.application.challenge.persistence.model.enums.ChallengeStatus;
import com.application.challenge.persistence.model.enums.ChallengeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Challenge - Sfida/competizione tra ristoranti.
 * <p>
 * Una Challenge è una competizione tematica (es. "Miglior Pizza", "Cucina Giapponese")
 * che può essere indipendente o parte di un torneo più ampio.
 * <p>
 * Flusso: DRAFT → UPCOMING → REGISTRATION → PRELIMINARY → ACTIVE → VOTING → COMPLETED
 */
@Entity
@Table(name = "challenge", indexes = {
    @Index(name = "idx_challenge_status", columnList = "status"),
    @Index(name = "idx_challenge_type", columnList = "challenge_type"),
    @Index(name = "idx_challenge_dates", columnList = "start_date, end_date"),
    @Index(name = "idx_challenge_location", columnList = "city, region")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== INFORMAZIONI BASE ====================

    /**
     * Nome della challenge
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Slug URL-friendly
     */
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    /**
     * Descrizione breve
     */
    @Column(length = 500)
    private String description;

    /**
     * Descrizione dettagliata (regolamento, premi, etc.)
     */
    @Column(name = "full_description", columnDefinition = "TEXT")
    private String fullDescription;

    /**
     * Immagine di copertina URL
     */
    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    // ==================== TIPO E CATEGORIA ====================

    /**
     * Tipo di challenge
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_type", nullable = false, length = 30)
    private ChallengeType challengeType;

    /**
     * Categoria specifica (es. "Pizza", "Sushi", "Dessert")
     */
    @Column(name = "category_filter", length = 100)
    private String categoryFilter;

    /**
     * Sottocategoria
     */
    @Column(name = "subcategory_filter", length = 100)
    private String subcategoryFilter;

    /**
     * Hashtag ufficiale
     */
    @Column(length = 50)
    private String hashtag;

    // ==================== STATO ====================

    /**
     * Stato corrente della challenge
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ChallengeStatus status = ChallengeStatus.DRAFT;

    /**
     * Challenge in evidenza
     */
    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    /**
     * Challenge sponsorizzata
     */
    @Column(name = "is_sponsored")
    @Builder.Default
    private Boolean isSponsored = false;

    /**
     * Sponsor della challenge
     */
    @Column(name = "sponsor_name", length = 200)
    private String sponsorName;

    /**
     * Logo sponsor URL
     */
    @Column(name = "sponsor_logo_url", length = 500)
    private String sponsorLogoUrl;

    // ==================== DATE ====================

    /**
     * Inizio periodo di registrazione
     */
    @Column(name = "registration_start_date")
    private LocalDate registrationStartDate;

    /**
     * Fine periodo di registrazione
     */
    @Column(name = "registration_end_date")
    private LocalDate registrationEndDate;

    /**
     * Data inizio challenge
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Data fine challenge
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Inizio fase preliminare (se prevista)
     */
    @Column(name = "preliminary_start_date")
    private LocalDate preliminaryStartDate;

    /**
     * Fine fase preliminare
     */
    @Column(name = "preliminary_end_date")
    private LocalDate preliminaryEndDate;

    /**
     * Inizio votazione finale
     */
    @Column(name = "voting_start_date")
    private LocalDate votingStartDate;

    /**
     * Fine votazione finale
     */
    @Column(name = "voting_end_date")
    private LocalDate votingEndDate;

    // ==================== LOCALIZZAZIONE ====================

    /**
     * Nazione (es. "IT")
     */
    @Column(length = 10)
    private String country;

    /**
     * Regione
     */
    @Column(length = 100)
    private String region;

    /**
     * Città
     */
    @Column(length = 100)
    private String city;

    /**
     * Zona/quartiere
     */
    @Column(length = 100)
    private String zone;

    /**
     * Latitudine centro area
     */
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    /**
     * Longitudine centro area
     */
    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    /**
     * Raggio di ricerca (km)
     */
    @Column(name = "radius_km", precision = 8, scale = 2)
    private BigDecimal radiusKm;

    // ==================== REQUISITI PARTECIPAZIONE ====================

    /**
     * Numero minimo di partecipanti per avviare
     */
    @Column(name = "min_participants")
    @Builder.Default
    private Integer minParticipants = 2;

    /**
     * Numero massimo di partecipanti
     */
    @Column(name = "max_participants")
    private Integer maxParticipants;

    /**
     * Rating minimo ristorante per partecipare
     */
    @Column(name = "min_rating", precision = 3, scale = 2)
    private BigDecimal minRating;

    /**
     * Numero minimo di recensioni richieste
     */
    @Column(name = "min_reviews")
    private Integer minReviews;

    /**
     * Solo ristoranti verificati
     */
    @Column(name = "verified_only")
    @Builder.Default
    private Boolean verifiedOnly = false;

    // ==================== CONFIGURAZIONE VOTAZIONE ====================

    /**
     * Solo voti verificati (prenotazione seated)
     */
    @Column(name = "verified_votes_only")
    @Builder.Default
    private Boolean verifiedVotesOnly = false;

    /**
     * Peso voti local
     */
    @Column(name = "local_vote_weight", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal localVoteWeight = new BigDecimal("1.5");

    /**
     * Numero massimo di finalisti
     */
    @Column(name = "max_finalists")
    @Builder.Default
    private Integer maxFinalists = 10;

    // ==================== PREMI ====================

    /**
     * Descrizione premio primo posto
     */
    @Column(name = "prize_first", length = 500)
    private String prizeFirst;

    /**
     * Descrizione premio secondo posto
     */
    @Column(name = "prize_second", length = 500)
    private String prizeSecond;

    /**
     * Descrizione premio terzo posto
     */
    @Column(name = "prize_third", length = 500)
    private String prizeThird;

    /**
     * Badge/riconoscimento assegnato
     */
    @Column(name = "badge_name", length = 100)
    private String badgeName;

    /**
     * URL icona badge
     */
    @Column(name = "badge_icon_url", length = 500)
    private String badgeIconUrl;

    // ==================== STATISTICHE ====================

    /**
     * Numero partecipanti registrati
     */
    @Column(name = "participants_count")
    @Builder.Default
    private Integer participantsCount = 0;

    /**
     * Numero totale voti
     */
    @Column(name = "total_votes")
    @Builder.Default
    private Integer totalVotes = 0;

    /**
     * Numero visualizzazioni
     */
    @Column(name = "views_count")
    @Builder.Default
    private Integer viewsCount = 0;

    /**
     * Numero storie create
     */
    @Column(name = "stories_count")
    @Builder.Default
    private Integer storiesCount = 0;

    // ==================== TIMESTAMP ====================

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // ==================== RELAZIONI ====================

    /**
     * Partecipazioni alla challenge
     */
    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChallengeParticipation> participations = new ArrayList<>();

    /**
     * Torneo associato (se la challenge fa parte di un torneo)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    // ==================== LIFECYCLE ====================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
        if (slug == null || slug.isBlank()) {
            slug = generateSlug();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== UTILITY ====================

    /**
     * Genera slug dal nome
     */
    private String generateSlug() {
        if (name == null) return "challenge-" + System.currentTimeMillis();
        return name.toLowerCase()
                   .replaceAll("[^a-z0-9\\s-]", "")
                   .replaceAll("\\s+", "-")
                   .replaceAll("-+", "-")
                   .replaceAll("^-|-$", "");
    }

    /**
     * Verifica se la challenge è attiva
     */
    public boolean isActive() {
        return status == ChallengeStatus.ACTIVE || 
               status == ChallengeStatus.VOTING;
    }

    /**
     * Verifica se è in fase di registrazione
     */
    public boolean isRegistrationOpen() {
        if (status != ChallengeStatus.REGISTRATION) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return (registrationStartDate == null || !today.isBefore(registrationStartDate)) &&
               (registrationEndDate == null || !today.isAfter(registrationEndDate));
    }

    /**
     * Verifica se è completa
     */
    public boolean isCompleted() {
        return status == ChallengeStatus.COMPLETED;
    }

    /**
     * Verifica se è annullata
     */
    public boolean isCancelled() {
        return status == ChallengeStatus.CANCELLED;
    }

    /**
     * Verifica se può accettare nuovi partecipanti
     */
    public boolean canAcceptParticipants() {
        if (!isRegistrationOpen()) {
            return false;
        }
        if (maxParticipants != null && participantsCount >= maxParticipants) {
            return false;
        }
        return true;
    }

    /**
     * Verifica se la votazione è aperta
     */
    public boolean isVotingOpen() {
        if (status != ChallengeStatus.VOTING) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return (votingStartDate == null || !today.isBefore(votingStartDate)) &&
               (votingEndDate == null || !today.isAfter(votingEndDate));
    }

    /**
     * Incrementa contatore partecipanti
     */
    public void incrementParticipantsCount() {
        if (participantsCount == null) participantsCount = 0;
        participantsCount++;
    }

    /**
     * Decrementa contatore partecipanti
     */
    public void decrementParticipantsCount() {
        if (participantsCount == null || participantsCount <= 0) return;
        participantsCount--;
    }

    /**
     * Incrementa visualizzazioni
     */
    public void incrementViewsCount() {
        if (viewsCount == null) viewsCount = 0;
        viewsCount++;
    }

    /**
     * Durata in giorni
     */
    public long getDurationDays() {
        if (startDate == null || endDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Giorni rimanenti
     */
    public long getRemainingDays() {
        if (endDate == null) return 0;
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), endDate);
        return Math.max(0, days);
    }

    /**
     * Ha premi
     */
    public boolean hasPrizes() {
        return (prizeFirst != null && !prizeFirst.isBlank()) ||
               (prizeSecond != null && !prizeSecond.isBlank()) ||
               (prizeThird != null && !prizeThird.isBlank());
    }
}
