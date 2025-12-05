# 🎮 Greedys Challenge System - Design Document

## Documento di Analisi e Progettazione

**Data**: 3 Dicembre 2025  
**Versione**: 1.0  
**Modulo**: Greedys Challenge  
**Dipendenze**: Reservation, Restaurant, Customer, ServiceVersion, Event System

---

## 📋 Indice

1. [Panoramica del Sistema](#1-panoramica-del-sistema)
2. [Architettura Generale](#2-architettura-generale)
3. [Sistema Ranking](#3-sistema-ranking)
4. [Sistema Tournament](#4-sistema-tournament)
5. [Sistema Challenge](#5-sistema-challenge)
6. [Sistema Voti](#6-sistema-voti)
7. [Contenuti Social (Story/Reel)](#7-contenuti-social-storyreel)
8. [Regole del Gioco](#8-regole-del-gioco)
9. [Sistema Notifiche](#9-sistema-notifiche)
10. [Modello Dati Completo](#10-modello-dati-completo)
11. [API Endpoints](#11-api-endpoints)
12. [Piano Implementazione](#12-piano-implementazione)

---

## 1. Panoramica del Sistema

### 1.1 Obiettivo

Greedys Challenge introduce **gamification** per ristoranti e utenti:
- **Classifiche** (Ranking) per zona/città/piatto
- **Tornei** con fase a gironi + eliminazione diretta
- **Sfide** settimanali/mensili
- **Voti verificati** basati su prenotazioni reali (SEATED)
- **Contenuti social** (storie/reel) collegati alle sfide

### 1.2 Attori

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ATTORI                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  CUSTOMER (Votante)                                                          │
│  ├── LOCAL: ha prenotato ≥3 volte nella zona negli ultimi 12 mesi           │
│  └── TOURIST: ha prenotato <3 volte nella zona                              │
│                                                                              │
│  RESTAURANT (Partecipante)                                                   │
│  ├── Iscrizione automatica alla classifica di zona                          │
│  ├── Partecipazione opzionale ai tornei                                     │
│  └── Pubblicazione contenuti social                                         │
│                                                                              │
│  SYSTEM (Orchestratore)                                                      │
│  ├── Calcolo ranking periodico                                              │
│  ├── Gestione fasi torneo                                                   │
│  └── Verifica eleggibilità voti                                             │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.3 Flusso Generale

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         FLUSSO CHALLENGE                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. RANKING CONTINUO (sempre attivo)                                         │
│     └── Ogni prenotazione SEATED = potenziale voto                          │
│                                                                              │
│  2. TOURNAMENT (periodico)                                                   │
│     ├── FASE 1: Qualificazione (ranking top N della zona)                   │
│     ├── FASE 2: Gironi (round-robin tra qualificati)                        │
│     ├── FASE 3: Quarti/Semifinale/Finale (scontri diretti)                  │
│     └── FASE 4: Premiazione + badge vincitore                               │
│                                                                              │
│  3. CHALLENGE (sfide specifiche)                                             │
│     ├── "Miglior Pizza Napoletana Dicembre 2025"                            │
│     ├── "Top 10 Carbonara Roma Centro"                                       │
│     └── Sfide sponsorizzate/tematiche                                        │
│                                                                              │
│  4. SOCIAL CONTENT                                                           │
│     ├── Story/Reel collegati a challenge                                    │
│     ├── Boost visibilità per partecipanti                                   │
│     └── Feed personalizzato per zona                                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Architettura Generale

### 2.1 Package Structure

```
com.application.challenge/
├── persistence/
│   ├── model/
│   │   ├── Ranking.java
│   │   ├── RankingEntry.java
│   │   ├── RankingVote.java
│   │   ├── Tournament.java
│   │   ├── TournamentMatch.java
│   │   ├── MatchVote.java
│   │   ├── Challenge.java
│   │   ├── ChallengeParticipation.java
│   │   ├── ChallengeStory.java
│   │   ├── ChallengeReel.java
│   │   └── enums/
│   │       ├── ChallengeType.java
│   │       ├── ChallengeStatus.java
│   │       ├── TournamentPhase.java
│   │       ├── TournamentStatus.java
│   │       ├── MatchStatus.java
│   │       ├── RankingPeriod.java
│   │       ├── RankingScope.java
│   │       ├── VoteType.java
│   │       ├── VoterType.java
│   │       └── ContentType.java
│   └── dao/
│       ├── RankingDAO.java
│       ├── RankingEntryDAO.java
│       ├── RankingVoteDAO.java
│       ├── TournamentDAO.java
│       ├── TournamentMatchDAO.java
│       ├── MatchVoteDAO.java
│       ├── ChallengeDAO.java
│       ├── ChallengeParticipationDAO.java
│       ├── ChallengeStoryDAO.java
│       └── ChallengeReelDAO.java
├── service/
│   ├── RankingService.java
│   ├── RankingCalculationService.java
│   ├── TournamentService.java
│   ├── TournamentMatchService.java
│   ├── ChallengeService.java
│   ├── VoteEligibilityService.java
│   ├── VoterClassificationService.java
│   └── ChallengeSocialService.java
├── controller/
│   ├── RankingController.java
│   ├── TournamentController.java
│   ├── ChallengeController.java
│   ├── ChallengeVoteController.java
│   └── ChallengeSocialController.java
└── web/
    └── dto/
        ├── RankingDTO.java
        ├── RankingEntryDTO.java
        ├── TournamentDTO.java
        ├── MatchDTO.java
        ├── ChallengeDTO.java
        ├── VoteDTO.java
        └── ...
```

### 2.2 Integrazione con Sistema Esistente

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    INTEGRAZIONE CHALLENGE                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  RESERVATION (esistente)                                                     │
│  └── status = SEATED → trigger per RankingVote                              │
│                                                                              │
│  RESTAURANT (esistente)                                                      │
│  ├── city, zone → usati per Ranking scope                                   │
│  └── cuisineType → usato per Challenge tematiche                            │
│                                                                              │
│  CUSTOMER (esistente)                                                        │
│  └── storico prenotazioni → classificazione LOCAL/TOURIST                   │
│                                                                              │
│  EVENT SYSTEM (esistente)                                                    │
│  └── EventType + RabbitMQ → notifiche challenge                             │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Sistema Ranking

### 3.1 Concetto

Il **Ranking** è una classifica continua che aggrega i voti dei clienti per zona/città/piatto.

### 3.2 Struttura Gerarchica

```
RANKING HIERARCHY
─────────────────
NATIONAL (Italia)
  └── REGIONAL (Lazio)
        └── CITY (Roma)
              └── ZONE (Trastevere)
                    └── CUISINE (Pizza, Sushi, etc.)
```

### 3.3 Entità Ranking

```java
@Entity
@Table(name = "ranking")
public class Ranking {
    @Id
    private Long id;
    
    private String name;                    // "Top Pizza Roma Centro Q4 2025"
    
    @Enumerated(EnumType.STRING)
    private RankingScope scope;             // NATIONAL, REGIONAL, CITY, ZONE
    
    @Enumerated(EnumType.STRING)
    private RankingPeriod period;           // WEEKLY, MONTHLY, QUARTERLY, YEARLY, ALL_TIME
    
    private String country;                 // "IT"
    private String region;                  // "Lazio"
    private String city;                    // "Roma"
    private String zone;                    // "Centro"
    
    private String cuisineType;             // null = tutte, "PIZZA", "SUSHI", etc.
    
    private LocalDate periodStart;
    private LocalDate periodEnd;
    
    private Boolean isActive;
    private LocalDateTime lastCalculatedAt;
    
    @OneToMany(mappedBy = "ranking")
    private List<RankingEntry> entries;
}
```

### 3.4 Entità RankingEntry

```java
@Entity
@Table(name = "ranking_entry")
public class RankingEntry {
    @Id
    private Long id;
    
    @ManyToOne
    private Ranking ranking;
    
    @ManyToOne
    private Restaurant restaurant;
    
    private Integer position;               // 1, 2, 3, ...
    private Integer previousPosition;       // per mostrare trend ↑↓
    
    private BigDecimal score;               // punteggio calcolato
    private Integer totalVotes;             // voti totali
    private Integer localVotes;             // voti da LOCAL
    private Integer touristVotes;           // voti da TOURIST
    
    private BigDecimal averageRating;       // media voti 1-5
    private Integer seatedReservations;     // prenotazioni SEATED nel periodo
    
    private LocalDateTime calculatedAt;
}
```

### 3.5 Calcolo Score

```
FORMULA SCORE
─────────────
score = (avgRating * 0.4) + (localVoteWeight * 0.35) + (touristVoteWeight * 0.15) + (trendBonus * 0.1)

Dove:
- avgRating: media voti 1-5 normalizzata a 100
- localVoteWeight: (localVotes / totalVotes) * avgRating * 100
- touristVoteWeight: (touristVotes / totalVotes) * avgRating * 100
- trendBonus: +10 se in crescita, -5 se in calo, 0 stabile
```

---

## 4. Sistema Tournament

### 4.1 Concetto

Il **Tournament** è una competizione periodica con:
- Fase qualificazione (top N dal ranking)
- Fase gironi (round-robin)
- Fase eliminazione (quarti → semifinale → finale)

### 4.2 Fasi Tournament

```
TOURNAMENT PHASES
─────────────────
┌──────────────────────────────────────────────────────────────────────────┐
│  REGISTRATION          │  Iscrizione ristoranti (auto o manuale)        │
├──────────────────────────────────────────────────────────────────────────┤
│  QUALIFICATION         │  Top 16/32 dal ranking zona entrano            │
├──────────────────────────────────────────────────────────────────────────┤
│  GROUP_STAGE           │  4 gironi da 4, round-robin                    │
│                        │  Top 2 di ogni girone passa                    │
├──────────────────────────────────────────────────────────────────────────┤
│  QUARTER_FINALS        │  8 ristoranti, scontri diretti                 │
├──────────────────────────────────────────────────────────────────────────┤
│  SEMI_FINALS           │  4 ristoranti                                  │
├──────────────────────────────────────────────────────────────────────────┤
│  FINALS                │  2 ristoranti, finale + finalina 3°/4°         │
├──────────────────────────────────────────────────────────────────────────┤
│  COMPLETED             │  Vincitore + badge + premio                    │
└──────────────────────────────────────────────────────────────────────────┘
```

### 4.3 Entità Tournament

```java
@Entity
@Table(name = "tournament")
public class Tournament {
    @Id
    private Long id;
    
    private String name;                    // "Torneo Pizza Roma Q4 2025"
    private String description;
    
    @Enumerated(EnumType.STRING)
    private TournamentStatus status;        // DRAFT, REGISTRATION, ONGOING, COMPLETED, CANCELLED
    
    @Enumerated(EnumType.STRING)
    private TournamentPhase currentPhase;
    
    // Scope geografico
    private String city;
    private String zone;                    // null = tutta la città
    private String cuisineType;             // null = tutte le cucine
    
    // Date
    private LocalDate registrationStart;
    private LocalDate registrationEnd;
    private LocalDate tournamentStart;
    private LocalDate tournamentEnd;
    
    // Configurazione
    private Integer maxParticipants;        // 16, 32, 64
    private Integer groupCount;             // numero gironi
    private Integer groupSize;              // ristoranti per girone
    
    // Premi
    private String firstPrizeBadge;         // "🏆 Campione Pizza Roma 2025"
    private String secondPrizeBadge;
    private String thirdPrizeBadge;
    
    @ManyToOne
    private Ranking sourceRanking;          // ranking da cui prendere qualificati
    
    @OneToMany(mappedBy = "tournament")
    private List<TournamentMatch> matches;
    
    @OneToMany(mappedBy = "tournament")
    private List<ChallengeParticipation> participants;
}
```

### 4.4 Entità TournamentMatch

```java
@Entity
@Table(name = "tournament_match")
public class TournamentMatch {
    @Id
    private Long id;
    
    @ManyToOne
    private Tournament tournament;
    
    @Enumerated(EnumType.STRING)
    private TournamentPhase phase;          // GROUP_STAGE, QUARTER_FINALS, etc.
    
    private Integer groupNumber;            // per fase gironi (1, 2, 3, 4)
    private Integer matchNumber;            // ordine nel bracket
    
    @ManyToOne
    private Restaurant restaurant1;
    
    @ManyToOne
    private Restaurant restaurant2;
    
    @Enumerated(EnumType.STRING)
    private MatchStatus status;             // SCHEDULED, VOTING, COMPLETED, CANCELLED
    
    private LocalDateTime votingStartsAt;
    private LocalDateTime votingEndsAt;
    
    // Risultati
    private Integer votes1;                 // voti per restaurant1
    private Integer votes2;                 // voti per restaurant2
    
    @ManyToOne
    private Restaurant winner;
    
    private Boolean isDraw;                 // per fase gironi può esserci pareggio
    
    @OneToMany(mappedBy = "match")
    private List<MatchVote> votes;
}
```

### 4.5 Entità MatchVote

```java
@Entity
@Table(name = "match_vote")
public class MatchVote {
    @Id
    private Long id;
    
    @ManyToOne
    private TournamentMatch match;
    
    @ManyToOne
    private Customer voter;
    
    @ManyToOne
    private Restaurant votedFor;            // quale ristorante ha votato
    
    @Enumerated(EnumType.STRING)
    private VoterType voterType;            // LOCAL, TOURIST
    
    // Prova di eleggibilità
    @ManyToOne
    private Reservation reservation1;       // prenotazione SEATED in restaurant1
    
    @ManyToOne
    private Reservation reservation2;       // prenotazione SEATED in restaurant2
    
    private LocalDateTime votedAt;
    
    private String ipAddress;               // anti-fraud
    private String deviceFingerprint;       // anti-fraud
}
```

---

## 5. Sistema Challenge

### 5.1 Concetto

Le **Challenge** sono sfide tematiche/temporali separate dai tornei:
- "Miglior Carbonara Novembre 2025"
- "Top 10 Sushi Milano"
- Challenge sponsorizzate

### 5.2 Entità Challenge

```java
@Entity
@Table(name = "challenge")
public class Challenge {
    @Id
    private Long id;
    
    private String name;
    private String description;
    private String rules;                   // regolamento
    
    @Enumerated(EnumType.STRING)
    private ChallengeType type;             // DISH_BATTLE, CUISINE_BEST, SEASONAL, SPONSORED
    
    @Enumerated(EnumType.STRING)
    private ChallengeStatus status;         // DRAFT, ACTIVE, VOTING, COMPLETED, CANCELLED
    
    // Scope
    private String city;
    private String zone;
    private String cuisineType;
    private String dishCategory;            // "Pizza", "Pasta", "Sushi"
    
    // Date
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private LocalDateTime votingStartsAt;
    private LocalDateTime votingEndsAt;
    
    // Limiti
    private Integer maxParticipants;
    private Integer minReservationsToVote;  // min prenotazioni SEATED per votare
    
    // Premi
    private String winnerBadge;
    private String winnerReward;            // descrizione premio
    
    // Sponsor (opzionale)
    private String sponsorName;
    private String sponsorLogo;
    
    @OneToMany(mappedBy = "challenge")
    private List<ChallengeParticipation> participations;
    
    @OneToMany(mappedBy = "challenge")
    private List<ChallengeStory> stories;
    
    @OneToMany(mappedBy = "challenge")
    private List<ChallengeReel> reels;
}
```

### 5.3 Entità ChallengeParticipation

```java
@Entity
@Table(name = "challenge_participation")
public class ChallengeParticipation {
    @Id
    private Long id;
    
    @ManyToOne
    private Challenge challenge;
    
    @ManyToOne
    private Tournament tournament;          // null se partecipa a challenge semplice
    
    @ManyToOne
    private Restaurant restaurant;
    
    private LocalDateTime joinedAt;
    
    @Enumerated(EnumType.STRING)
    private ParticipationStatus status;     // REGISTERED, ACTIVE, ELIMINATED, WINNER, WITHDRAWN
    
    // Statistiche
    private Integer totalVotes;
    private Integer localVotes;
    private Integer touristVotes;
    private BigDecimal averageRating;
    
    private Integer finalPosition;          // posizione finale (1°, 2°, 3°...)
    
    // Per tornei - fase gironi
    private Integer groupNumber;
    private Integer groupPoints;            // punti nel girone
    private Integer groupWins;
    private Integer groupDraws;
    private Integer groupLosses;
}
```

---

## 6. Sistema Voti

### 6.1 Regola Fondamentale

> **Un utente può votare SOLO se ha una prenotazione con status SEATED nel ristorante.**

### 6.2 Voto per Ranking (continuo)

```java
@Entity
@Table(name = "ranking_vote")
public class RankingVote {
    @Id
    private Long id;
    
    @ManyToOne
    private Customer voter;
    
    @ManyToOne
    private Restaurant restaurant;
    
    @ManyToOne
    private Reservation reservation;        // DEVE essere SEATED
    
    @Enumerated(EnumType.STRING)
    private VoterType voterType;            // LOCAL, TOURIST
    
    private Integer rating;                 // 1-5 stelle
    
    private String comment;                 // recensione opzionale
    
    // Categorie specifiche (opzionali)
    private Integer foodRating;             // 1-5
    private Integer serviceRating;          // 1-5
    private Integer ambienceRating;         // 1-5
    private Integer valueRating;            // 1-5
    
    private LocalDateTime votedAt;
    
    // Anti-fraud
    private String ipAddress;
    private String deviceFingerprint;
    private Boolean isVerified;             // verificato manualmente se sospetto
}
```

### 6.3 Voto per Match (scontro diretto)

Per votare in uno scontro diretto, l'utente deve aver mangiato in **ENTRAMBI** i ristoranti:

```java
// VoteEligibilityService.java
public boolean canVoteMatch(Customer customer, TournamentMatch match) {
    // Verifica prenotazione SEATED in restaurant1
    boolean hasSeated1 = reservationDAO.existsByCustomerAndRestaurantAndStatus(
        customer.getId(), 
        match.getRestaurant1().getId(), 
        Reservation.Status.SEATED
    );
    
    // Verifica prenotazione SEATED in restaurant2
    boolean hasSeated2 = reservationDAO.existsByCustomerAndRestaurantAndStatus(
        customer.getId(), 
        match.getRestaurant2().getId(), 
        Reservation.Status.SEATED
    );
    
    return hasSeated1 && hasSeated2;
}
```

### 6.4 Classificazione LOCAL vs TOURIST

```java
// VoterClassificationService.java
public VoterType classifyVoter(Customer customer, String zone) {
    // Conta prenotazioni SEATED nella zona negli ultimi 12 mesi
    LocalDate oneYearAgo = LocalDate.now().minusMonths(12);
    
    long seatedCount = reservationDAO.countByCustomerAndZoneAndStatusAndDateAfter(
        customer.getId(),
        zone,
        Reservation.Status.SEATED,
        oneYearAgo
    );
    
    // LOCAL = almeno 3 prenotazioni nella zona
    return seatedCount >= 3 ? VoterType.LOCAL : VoterType.TOURIST;
}
```

---

## 7. Contenuti Social (Story/Reel)

### 7.1 Concetto

I ristoranti possono pubblicare contenuti social collegati alle challenge per aumentare visibilità.

### 7.2 Entità ChallengeStory

```java
@Entity
@Table(name = "challenge_story")
public class ChallengeStory {
    @Id
    private Long id;
    
    @ManyToOne
    private Challenge challenge;
    
    @ManyToOne
    private Restaurant restaurant;
    
    @Enumerated(EnumType.STRING)
    private ContentType contentType;        // IMAGE, VIDEO, TEXT
    
    private String mediaUrl;                // URL del media
    private String thumbnailUrl;
    private String caption;
    
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;        // +24h standard
    
    private Boolean isActive;
    
    // Metriche
    private Integer viewCount;
    private Integer likeCount;
    private Integer replyCount;
    
    @OneToMany(mappedBy = "story")
    private List<StoryView> views;
    
    @OneToMany(mappedBy = "story")
    private List<StoryLike> likes;
    
    @OneToMany(mappedBy = "story")
    private List<StoryReply> replies;
}
```

### 7.3 Entità ChallengeReel

```java
@Entity
@Table(name = "challenge_reel")
public class ChallengeReel {
    @Id
    private Long id;
    
    @ManyToOne
    private Challenge challenge;
    
    @ManyToOne
    private Restaurant restaurant;
    
    private String title;
    private String description;
    
    private String videoUrl;
    private String thumbnailUrl;
    private Integer durationSeconds;
    
    private LocalDateTime publishedAt;
    
    private Boolean isActive;
    private Boolean isFeatured;             // in evidenza
    
    // Metriche
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    
    // Tags
    @ElementCollection
    private List<String> hashtags;
}
```

---

## 8. Regole del Gioco

### 8.1 Regole Partecipazione Ristoranti

```
REGOLE RISTORANTI
─────────────────
1. Max 3 challenge contemporanee
2. Max 1 torneo attivo per categoria (es: solo 1 torneo pizza alla volta)
3. Iscrizione automatica al ranking di zona (opt-out possibile)
4. Per tornei: minimo 10 prenotazioni SEATED nell'ultimo mese
5. Ristoranti "piccoli" (<50 coperti) → classifiche "aree estese"
```

### 8.2 Regole Voto

```
REGOLE VOTO
───────────
1. RANKING VOTE:
   - Richiede 1 prenotazione SEATED nel ristorante
   - Max 1 voto per ristorante ogni 30 giorni
   - Peso: LOCAL = 1.0, TOURIST = 0.7

2. MATCH VOTE (scontro diretto):
   - Richiede prenotazione SEATED in ENTRAMBI i ristoranti
   - Prenotazioni valide: ultimi 6 mesi
   - Max 1 voto per match
   - Peso: LOCAL = 1.0, TOURIST = 0.5

3. ANTI-FRAUD:
   - Stesso IP può votare max 3 ristoranti/giorno
   - Device fingerprint per rilevare account multipli
   - Review manuale per pattern sospetti
```

### 8.3 Regole Classificazione Votante

```
CLASSIFICAZIONE VOTER
─────────────────────
LOCAL (peso maggiore):
  - ≥3 prenotazioni SEATED nella zona in 12 mesi
  - Residenza verificata (opzionale)

TOURIST (peso minore):
  - <3 prenotazioni nella zona
  - Prima visita nella città
  
VERIFIED (badge speciale):
  - >10 prenotazioni totali
  - Account verificato con documento
  - Nessun flag anti-fraud
```

### 8.4 Gestione Prenotazioni Fuori Orario

```
OFF_SCHEDULE_GROUP
──────────────────
Le prenotazioni di gruppo/fuori orario (SpecialBookingType.OFF_HOURS, GROUP_BOOKING)
sono VALIDE per:
  ✓ Eleggibilità voto
  ✓ Conteggio LOCAL/TOURIST
  ✓ Ranking score

Sono escluse da:
  ✗ Conteggio "prenotazioni standard" per qualifica torneo
```

---

## 9. Sistema Notifiche

### 9.1 Nuovi EventType

```java
// Da aggiungere a EventType.java

// ─────── CHALLENGE EVENTS ───────

CHALLENGE_CREATED("challenge.created"),
CHALLENGE_STARTED("challenge.started"),
CHALLENGE_ENDING_SOON("challenge.ending_soon"),
CHALLENGE_COMPLETED("challenge.completed"),
CHALLENGE_RESTAURANT_JOINED("challenge.restaurant_joined"),
CHALLENGE_NEW_VOTE("challenge.new_vote"),
CHALLENGE_POSITION_CHANGED("challenge.position_changed"),

// ─────── TOURNAMENT EVENTS ───────

TOURNAMENT_CREATED("tournament.created"),
TOURNAMENT_REGISTRATION_OPEN("tournament.registration_open"),
TOURNAMENT_REGISTRATION_CLOSING("tournament.registration_closing"),
TOURNAMENT_STARTED("tournament.started"),
TOURNAMENT_PHASE_CHANGED("tournament.phase_changed"),
TOURNAMENT_MATCH_SCHEDULED("tournament.match_scheduled"),
TOURNAMENT_MATCH_VOTING_OPEN("tournament.match_voting_open"),
TOURNAMENT_MATCH_COMPLETED("tournament.match_completed"),
TOURNAMENT_RESTAURANT_ADVANCED("tournament.restaurant_advanced"),
TOURNAMENT_RESTAURANT_ELIMINATED("tournament.restaurant_eliminated"),
TOURNAMENT_COMPLETED("tournament.completed"),

// ─────── RANKING EVENTS ───────

RANKING_UPDATED("ranking.updated"),
RANKING_POSITION_UP("ranking.position_up"),
RANKING_POSITION_DOWN("ranking.position_down"),
RANKING_TOP10_ENTERED("ranking.top10_entered"),
RANKING_NEW_LEADER("ranking.new_leader"),

// ─────── CHALLENGE SOCIAL EVENTS ───────

CHALLENGE_STORY_PUBLISHED("challenge.story_published"),
CHALLENGE_REEL_PUBLISHED("challenge.reel_published"),
CHALLENGE_CONTENT_FEATURED("challenge.content_featured"),
```

### 9.2 Payload Eventi

```java
// Esempio payload TOURNAMENT_MATCH_COMPLETED
{
    "eventType": "tournament.match_completed",
    "tournamentId": 123,
    "tournamentName": "Torneo Pizza Roma 2025",
    "matchId": 456,
    "phase": "QUARTER_FINALS",
    "restaurant1Id": 10,
    "restaurant1Name": "Pizzeria Da Mario",
    "restaurant1Votes": 45,
    "restaurant2Id": 20,
    "restaurant2Name": "Pizza Express",
    "restaurant2Votes": 38,
    "winnerId": 10,
    "winnerName": "Pizzeria Da Mario"
}
```

---

## 10. Modello Dati Completo

### 10.1 Diagramma ER

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ENTITY RELATIONSHIPS                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  RANKING ────────< RANKING_ENTRY >──────── RESTAURANT                       │
│     │                    │                                                   │
│     │                    └──< RANKING_VOTE >──── CUSTOMER                   │
│     │                              │                                         │
│     │                              └──── RESERVATION (SEATED)               │
│     │                                                                        │
│  TOURNAMENT ─────< TOURNAMENT_MATCH >────── RESTAURANT (x2)                 │
│     │                    │                                                   │
│     │                    └──< MATCH_VOTE >────── CUSTOMER                   │
│     │                              │                                         │
│     │                              └──── RESERVATION (x2)                   │
│     │                                                                        │
│  CHALLENGE ─────< CHALLENGE_PARTICIPATION >──── RESTAURANT                  │
│     │                                                                        │
│     ├─────< CHALLENGE_STORY >──── RESTAURANT                                │
│     │                                                                        │
│     └─────< CHALLENGE_REEL >───── RESTAURANT                                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 10.2 SQL DDL

```sql
-- ═══════════════════════════════════════════════════════════════════════════
-- RANKING TABLES
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE ranking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    scope ENUM('NATIONAL', 'REGIONAL', 'CITY', 'ZONE') NOT NULL,
    period ENUM('WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY', 'ALL_TIME') NOT NULL,
    country VARCHAR(2),
    region VARCHAR(100),
    city VARCHAR(100),
    zone VARCHAR(100),
    cuisine_type VARCHAR(50),
    period_start DATE,
    period_end DATE,
    is_active BOOLEAN DEFAULT TRUE,
    last_calculated_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ranking_scope (scope, city, zone),
    INDEX idx_ranking_active (is_active, period_end)
);

CREATE TABLE ranking_entry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ranking_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    position INT NOT NULL,
    previous_position INT,
    score DECIMAL(10,4) NOT NULL,
    total_votes INT DEFAULT 0,
    local_votes INT DEFAULT 0,
    tourist_votes INT DEFAULT 0,
    average_rating DECIMAL(3,2),
    seated_reservations INT DEFAULT 0,
    calculated_at DATETIME NOT NULL,
    FOREIGN KEY (ranking_id) REFERENCES ranking(id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurant(id),
    UNIQUE KEY uk_ranking_restaurant (ranking_id, restaurant_id),
    INDEX idx_entry_position (ranking_id, position)
);

CREATE TABLE ranking_vote (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    voter_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    reservation_id BIGINT NOT NULL,
    voter_type ENUM('LOCAL', 'TOURIST', 'VERIFIED') NOT NULL,
    rating TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    food_rating TINYINT,
    service_rating TINYINT,
    ambience_rating TINYINT,
    value_rating TINYINT,
    voted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    device_fingerprint VARCHAR(255),
    is_verified BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (voter_id) REFERENCES customer(id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurant(id),
    FOREIGN KEY (reservation_id) REFERENCES reservation(id),
    INDEX idx_vote_restaurant (restaurant_id, voted_at),
    INDEX idx_vote_customer (voter_id, restaurant_id)
);

-- ═══════════════════════════════════════════════════════════════════════════
-- TOURNAMENT TABLES
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE tournament (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status ENUM('DRAFT', 'REGISTRATION', 'ONGOING', 'COMPLETED', 'CANCELLED') NOT NULL,
    current_phase ENUM('REGISTRATION', 'QUALIFICATION', 'GROUP_STAGE', 
                       'QUARTER_FINALS', 'SEMI_FINALS', 'FINALS', 'COMPLETED'),
    city VARCHAR(100) NOT NULL,
    zone VARCHAR(100),
    cuisine_type VARCHAR(50),
    registration_start DATE,
    registration_end DATE,
    tournament_start DATE NOT NULL,
    tournament_end DATE,
    max_participants INT DEFAULT 16,
    group_count INT DEFAULT 4,
    group_size INT DEFAULT 4,
    first_prize_badge VARCHAR(100),
    second_prize_badge VARCHAR(100),
    third_prize_badge VARCHAR(100),
    source_ranking_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (source_ranking_id) REFERENCES ranking(id),
    INDEX idx_tournament_status (status, city),
    INDEX idx_tournament_dates (tournament_start, tournament_end)
);

CREATE TABLE tournament_match (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tournament_id BIGINT NOT NULL,
    phase ENUM('GROUP_STAGE', 'QUARTER_FINALS', 'SEMI_FINALS', 'FINALS', 'THIRD_PLACE') NOT NULL,
    group_number INT,
    match_number INT NOT NULL,
    restaurant1_id BIGINT NOT NULL,
    restaurant2_id BIGINT NOT NULL,
    status ENUM('SCHEDULED', 'VOTING', 'COMPLETED', 'CANCELLED') NOT NULL,
    voting_starts_at DATETIME,
    voting_ends_at DATETIME,
    votes1 INT DEFAULT 0,
    votes2 INT DEFAULT 0,
    winner_id BIGINT,
    is_draw BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (tournament_id) REFERENCES tournament(id),
    FOREIGN KEY (restaurant1_id) REFERENCES restaurant(id),
    FOREIGN KEY (restaurant2_id) REFERENCES restaurant(id),
    FOREIGN KEY (winner_id) REFERENCES restaurant(id),
    INDEX idx_match_tournament (tournament_id, phase),
    INDEX idx_match_voting (status, voting_ends_at)
);

CREATE TABLE match_vote (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    voter_id BIGINT NOT NULL,
    voted_for_id BIGINT NOT NULL,
    voter_type ENUM('LOCAL', 'TOURIST', 'VERIFIED') NOT NULL,
    reservation1_id BIGINT NOT NULL,
    reservation2_id BIGINT NOT NULL,
    voted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    device_fingerprint VARCHAR(255),
    FOREIGN KEY (match_id) REFERENCES tournament_match(id),
    FOREIGN KEY (voter_id) REFERENCES customer(id),
    FOREIGN KEY (voted_for_id) REFERENCES restaurant(id),
    FOREIGN KEY (reservation1_id) REFERENCES reservation(id),
    FOREIGN KEY (reservation2_id) REFERENCES reservation(id),
    UNIQUE KEY uk_match_voter (match_id, voter_id),
    INDEX idx_matchvote_match (match_id)
);

-- ═══════════════════════════════════════════════════════════════════════════
-- CHALLENGE TABLES
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE challenge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    rules TEXT,
    type ENUM('DISH_BATTLE', 'CUISINE_BEST', 'SEASONAL', 'SPONSORED', 'WEEKLY', 'MONTHLY') NOT NULL,
    status ENUM('DRAFT', 'ACTIVE', 'VOTING', 'COMPLETED', 'CANCELLED') NOT NULL,
    city VARCHAR(100),
    zone VARCHAR(100),
    cuisine_type VARCHAR(50),
    dish_category VARCHAR(100),
    starts_at DATETIME NOT NULL,
    ends_at DATETIME NOT NULL,
    voting_starts_at DATETIME,
    voting_ends_at DATETIME,
    max_participants INT,
    min_reservations_to_vote INT DEFAULT 1,
    winner_badge VARCHAR(100),
    winner_reward TEXT,
    sponsor_name VARCHAR(100),
    sponsor_logo VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_challenge_status (status, ends_at),
    INDEX idx_challenge_scope (city, zone, cuisine_type)
);

CREATE TABLE challenge_participation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    challenge_id BIGINT,
    tournament_id BIGINT,
    restaurant_id BIGINT NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status ENUM('REGISTERED', 'ACTIVE', 'ELIMINATED', 'WINNER', 'RUNNER_UP', 'WITHDRAWN') NOT NULL,
    total_votes INT DEFAULT 0,
    local_votes INT DEFAULT 0,
    tourist_votes INT DEFAULT 0,
    average_rating DECIMAL(3,2),
    final_position INT,
    group_number INT,
    group_points INT DEFAULT 0,
    group_wins INT DEFAULT 0,
    group_draws INT DEFAULT 0,
    group_losses INT DEFAULT 0,
    FOREIGN KEY (challenge_id) REFERENCES challenge(id),
    FOREIGN KEY (tournament_id) REFERENCES tournament(id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurant(id),
    UNIQUE KEY uk_challenge_restaurant (challenge_id, restaurant_id),
    UNIQUE KEY uk_tournament_restaurant (tournament_id, restaurant_id),
    INDEX idx_participation_status (status)
);

-- ═══════════════════════════════════════════════════════════════════════════
-- SOCIAL CONTENT TABLES
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE challenge_story (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    challenge_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    content_type ENUM('IMAGE', 'VIDEO', 'TEXT') NOT NULL,
    media_url VARCHAR(500),
    thumbnail_url VARCHAR(500),
    caption TEXT,
    published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    reply_count INT DEFAULT 0,
    FOREIGN KEY (challenge_id) REFERENCES challenge(id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurant(id),
    INDEX idx_story_active (is_active, expires_at),
    INDEX idx_story_challenge (challenge_id, published_at DESC)
);

CREATE TABLE challenge_reel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    challenge_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    title VARCHAR(200),
    description TEXT,
    video_url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    duration_seconds INT,
    published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    is_featured BOOLEAN DEFAULT FALSE,
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    share_count INT DEFAULT 0,
    hashtags JSON,
    FOREIGN KEY (challenge_id) REFERENCES challenge(id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurant(id),
    INDEX idx_reel_featured (is_featured, is_active),
    INDEX idx_reel_challenge (challenge_id, published_at DESC)
);

CREATE TABLE story_view (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    story_id BIGINT NOT NULL,
    viewer_id BIGINT NOT NULL,
    viewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (story_id) REFERENCES challenge_story(id),
    FOREIGN KEY (viewer_id) REFERENCES customer(id),
    UNIQUE KEY uk_story_viewer (story_id, viewer_id)
);

CREATE TABLE story_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    story_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    liked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (story_id) REFERENCES challenge_story(id),
    FOREIGN KEY (user_id) REFERENCES customer(id),
    UNIQUE KEY uk_story_like (story_id, user_id)
);

CREATE TABLE story_reply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    story_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    replied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (story_id) REFERENCES challenge_story(id),
    FOREIGN KEY (user_id) REFERENCES customer(id)
);
```

---

## 11. API Endpoints

### 11.1 Ranking API

| Method | Endpoint | Descrizione | Roles |
|--------|----------|-------------|-------|
| GET | `/api/ranking` | Lista ranking attivi | PUBLIC |
| GET | `/api/ranking/{id}` | Dettaglio ranking con entries | PUBLIC |
| GET | `/api/ranking/city/{city}` | Ranking per città | PUBLIC |
| GET | `/api/ranking/zone/{city}/{zone}` | Ranking per zona | PUBLIC |
| GET | `/api/ranking/cuisine/{cuisine}` | Ranking per tipo cucina | PUBLIC |
| GET | `/api/ranking/restaurant/{restaurantId}` | Posizioni ristorante in tutti i ranking | RESTAURANT |
| POST | `/api/ranking/vote` | Vota un ristorante | CUSTOMER |
| GET | `/api/ranking/vote/eligibility/{restaurantId}` | Verifica se può votare | CUSTOMER |

### 11.2 Tournament API

| Method | Endpoint | Descrizione | Roles |
|--------|----------|-------------|-------|
| GET | `/api/tournament` | Lista tornei attivi | PUBLIC |
| GET | `/api/tournament/{id}` | Dettaglio torneo | PUBLIC |
| GET | `/api/tournament/{id}/bracket` | Bracket completo | PUBLIC |
| GET | `/api/tournament/{id}/matches` | Lista match | PUBLIC |
| POST | `/api/tournament/{id}/register` | Iscrivi ristorante | RESTAURANT |
| GET | `/api/tournament/match/{matchId}` | Dettaglio match | PUBLIC |
| POST | `/api/tournament/match/{matchId}/vote` | Vota match | CUSTOMER |
| GET | `/api/tournament/match/{matchId}/eligibility` | Verifica eleggibilità voto | CUSTOMER |

### 11.3 Challenge API

| Method | Endpoint | Descrizione | Roles |
|--------|----------|-------------|-------|
| GET | `/api/challenge` | Lista challenge attive | PUBLIC |
| GET | `/api/challenge/{id}` | Dettaglio challenge | PUBLIC |
| GET | `/api/challenge/{id}/leaderboard` | Classifica challenge | PUBLIC |
| POST | `/api/challenge/{id}/join` | Partecipa a challenge | RESTAURANT |
| POST | `/api/challenge/{id}/vote` | Vota in challenge | CUSTOMER |
| GET | `/api/challenge/restaurant/{restaurantId}` | Challenge del ristorante | RESTAURANT |

### 11.4 Social API

| Method | Endpoint | Descrizione | Roles |
|--------|----------|-------------|-------|
| POST | `/api/challenge/{id}/story` | Pubblica story | RESTAURANT |
| POST | `/api/challenge/{id}/reel` | Pubblica reel | RESTAURANT |
| GET | `/api/challenge/{id}/stories` | Stories della challenge | PUBLIC |
| GET | `/api/challenge/{id}/reels` | Reels della challenge | PUBLIC |
| GET | `/api/feed/city/{city}` | Feed social per città | PUBLIC |
| GET | `/api/feed/zone/{city}/{zone}` | Feed social per zona | PUBLIC |
| POST | `/api/story/{id}/view` | Registra view | CUSTOMER |
| POST | `/api/story/{id}/like` | Like story | CUSTOMER |
| POST | `/api/story/{id}/reply` | Rispondi a story | CUSTOMER |
| POST | `/api/reel/{id}/like` | Like reel | CUSTOMER |
| POST | `/api/reel/{id}/comment` | Commenta reel | CUSTOMER |

---

## 12. Piano Implementazione

### Fase 1: Fondamenta (Settimana 1)
- [x] Design Document
- [ ] Enums
- [ ] Entity Ranking (Ranking, RankingEntry, RankingVote)

### Fase 2: Tournament & Challenge (Settimana 2)
- [ ] Entity Tournament (Tournament, TournamentMatch, MatchVote)
- [ ] Entity Challenge (Challenge, ChallengeParticipation)
- [ ] Entity Social (ChallengeStory, ChallengeReel)

### Fase 3: Persistence (Settimana 2)
- [ ] Tutti i DAO
- [ ] Query ottimizzate per ranking

### Fase 4: Business Logic (Settimana 3)
- [ ] RankingService + RankingCalculationService
- [ ] TournamentService + TournamentMatchService
- [ ] ChallengeService
- [ ] VoteEligibilityService + VoterClassificationService

### Fase 5: Eventi & Notifiche (Settimana 3)
- [ ] Nuovi EventType
- [ ] Integration con sistema esistente

### Fase 6: API Layer (Settimana 4)
- [ ] Controllers
- [ ] DTOs
- [ ] Validazioni

### Fase 7: Testing & Refinement (Settimana 4-5)
- [ ] Unit tests
- [ ] Integration tests
- [ ] Performance tuning ranking calculation

---

## Note Finali

### Considerazioni Performance

1. **Ranking Calculation**: Eseguire come job schedulato (ogni ora per WEEKLY, ogni giorno per MONTHLY)
2. **Caching**: Redis per ranking entries più acceduti
3. **Pagination**: Tutte le liste paginate
4. **Indexing**: Indici su tutte le FK e campi di ricerca

### Compatibilità

Il sistema è progettato per integrarsi con:
- ✅ Reservation (verifica SEATED)
- ✅ Restaurant (scope geografico)
- ✅ Customer (votante)
- ✅ Event System (notifiche)
- ✅ SpecialBookingType (OFF_HOURS, GROUP_BOOKING)

---

*Documento generato il 3 Dicembre 2025*
