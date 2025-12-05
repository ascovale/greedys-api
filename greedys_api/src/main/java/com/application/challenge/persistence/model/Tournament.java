package com.application.challenge.persistence.model;

import com.application.challenge.persistence.model.enums.TournamentPhase;
import com.application.challenge.persistence.model.enums.TournamentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tournament - Torneo tra ristoranti.
 * <p>
 * Competizione con fasi: qualificazione → gironi → eliminazione diretta → finale.
 */
@Entity
@Table(name = "tournament", indexes = {
    @Index(name = "idx_tournament_status", columnList = "status, city"),
    @Index(name = "idx_tournament_dates", columnList = "tournament_start, tournament_end"),
    @Index(name = "idx_tournament_cuisine", columnList = "cuisine_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nome del torneo (es: "Torneo Pizza Roma Q4 2025")
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Descrizione del torneo
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Regolamento del torneo
     */
    @Column(columnDefinition = "TEXT")
    private String rules;

    // ==================== STATO ====================

    /**
     * Stato del torneo
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TournamentStatus status;

    /**
     * Fase corrente del torneo
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_phase", length = 20)
    private TournamentPhase currentPhase;

    // ==================== SCOPE GEOGRAFICO ====================

    /**
     * Città del torneo
     */
    @Column(nullable = false, length = 100)
    private String city;

    /**
     * Zona specifica (null = tutta la città)
     */
    @Column(length = 100)
    private String zone;

    /**
     * Tipo di cucina (null = tutte)
     */
    @Column(name = "cuisine_type", length = 50)
    private String cuisineType;

    /**
     * Categoria piatto (es: "Pizza", "Pasta")
     */
    @Column(name = "dish_category", length = 100)
    private String dishCategory;

    // ==================== DATE ====================

    /**
     * Inizio registrazione
     */
    @Column(name = "registration_start")
    private LocalDate registrationStart;

    /**
     * Fine registrazione
     */
    @Column(name = "registration_end")
    private LocalDate registrationEnd;

    /**
     * Inizio torneo
     */
    @Column(name = "tournament_start", nullable = false)
    private LocalDate tournamentStart;

    /**
     * Fine torneo
     */
    @Column(name = "tournament_end")
    private LocalDate tournamentEnd;

    // ==================== CONFIGURAZIONE ====================

    /**
     * Numero massimo partecipanti (16, 32, 64)
     */
    @Column(name = "max_participants")
    @Builder.Default
    private Integer maxParticipants = 16;

    /**
     * Numero di gironi
     */
    @Column(name = "group_count")
    @Builder.Default
    private Integer groupCount = 4;

    /**
     * Ristoranti per girone
     */
    @Column(name = "group_size")
    @Builder.Default
    private Integer groupSize = 4;

    /**
     * Quanti passano per girone (di solito 2)
     */
    @Column(name = "qualifiers_per_group")
    @Builder.Default
    private Integer qualifiersPerGroup = 2;

    /**
     * Durata votazione match in ore
     */
    @Column(name = "match_voting_hours")
    @Builder.Default
    private Integer matchVotingHours = 48;

    // ==================== PREMI ====================

    /**
     * Badge vincitore
     */
    @Column(name = "first_prize_badge", length = 100)
    private String firstPrizeBadge;

    /**
     * Badge secondo posto
     */
    @Column(name = "second_prize_badge", length = 100)
    private String secondPrizeBadge;

    /**
     * Badge terzo posto
     */
    @Column(name = "third_prize_badge", length = 100)
    private String thirdPrizeBadge;

    /**
     * Descrizione premio vincitore
     */
    @Column(name = "first_prize_description", length = 500)
    private String firstPrizeDescription;

    // ==================== RANKING FONTE ====================

    /**
     * Ranking da cui prendere i qualificati
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_ranking_id")
    private Ranking sourceRanking;

    /**
     * Numero minimo posizione nel ranking per qualificarsi
     */
    @Column(name = "min_ranking_position")
    @Builder.Default
    private Integer minRankingPosition = 32;

    // ==================== TIMESTAMP ====================

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== RELAZIONI ====================

    /**
     * Match del torneo
     */
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TournamentMatch> matches = new ArrayList<>();

    /**
     * Partecipazioni al torneo
     */
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChallengeParticipation> participants = new ArrayList<>();

    // ==================== LIFECYCLE ====================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = TournamentStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== UTILITY ====================

    /**
     * Verifica se è in fase di registrazione
     */
    public boolean isRegistrationOpen() {
        if (status != TournamentStatus.REGISTRATION) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return (registrationStart == null || !today.isBefore(registrationStart)) &&
               (registrationEnd == null || !today.isAfter(registrationEnd));
    }

    /**
     * Conta i partecipanti attuali
     */
    public int getParticipantCount() {
        return participants != null ? participants.size() : 0;
    }

    /**
     * Verifica se ci sono posti disponibili
     */
    public boolean hasAvailableSlots() {
        return getParticipantCount() < maxParticipants;
    }

    /**
     * Verifica se è una fase knockout
     */
    public boolean isInKnockoutPhase() {
        return currentPhase != null && currentPhase.isKnockoutPhase();
    }
}
