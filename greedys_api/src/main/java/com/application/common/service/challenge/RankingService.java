package com.application.common.service.challenge;

import com.application.challenge.persistence.dao.RankingEntryRepository;
import com.application.challenge.persistence.dao.RankingRepository;
import com.application.challenge.persistence.model.Ranking;
import com.application.challenge.persistence.model.RankingEntry;
import com.application.challenge.persistence.model.enums.RankingPeriod;
import com.application.challenge.persistence.model.enums.RankingScope;
import com.application.common.domain.event.EventType;
import com.application.common.persistence.dao.EventOutboxDAO;
import com.application.common.persistence.model.notification.EventOutbox;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.model.Restaurant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * RankingService - Gestione classifiche dei ristoranti.
 * <p>
 * Responsabilità:
 * - CRUD classifiche
 * - Calcolo e aggiornamento posizioni
 * - Query classifiche per scope geografico e periodo
 * - Gestione entries dei ristoranti
 * - Calcolo score e statistiche
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RankingService {

    private final RankingRepository rankingRepository;
    private final RankingEntryRepository rankingEntryRepository;
    private final RestaurantDAO restaurantDAO;
    private final EventOutboxDAO eventOutboxDAO;

    // ==================== CRUD CLASSIFICHE ====================

    /**
     * Crea una nuova classifica
     */
    public Ranking createRanking(String name, String description, RankingScope scope,
                                  RankingPeriod period, String city, String region, String zone) {
        log.info("Creazione classifica: {} scope={} period={}", name, scope, period);

        Ranking ranking = Ranking.builder()
                .name(name)
                .description(description)
                .scope(scope)
                .period(period)
                .city(city)
                .region(region)
                .zone(zone)
                .country("IT")
                .isActive(true)
                .build();

        // Imposta date periodo in base al tipo
        setRankingPeriodDates(ranking, period);

        ranking = rankingRepository.save(ranking);

        log.info("Classifica creata con ID: {}", ranking.getId());
        return ranking;
    }

    /**
     * Trova classifica per ID
     */
    @Transactional(readOnly = true)
    public Ranking findById(Long id) {
        return rankingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ranking not found with ID: " + id));
    }

    /**
     * Trova classifica con entries (fetch eager)
     */
    @Transactional(readOnly = true)
    public Ranking findByIdWithEntries(Long id) {
        Ranking ranking = findById(id);
        // Force loading entries
        ranking.getEntries().size();
        return ranking;
    }

    /**
     * Aggiorna classifica
     */
    public Ranking updateRanking(Long id, String name, String description) {
        Ranking ranking = findById(id);
        
        if (name != null) {
            ranking.setName(name);
        }
        if (description != null) {
            ranking.setDescription(description);
        }

        return rankingRepository.save(ranking);
    }

    /**
     * Disattiva classifica
     */
    public void deactivateRanking(Long rankingId) {
        Ranking ranking = findById(rankingId);
        ranking.setIsActive(false);
        rankingRepository.save(ranking);
        log.info("Classifica {} disattivata", rankingId);
    }

    /**
     * Elimina classifica (soft delete via deactivate)
     */
    public void deleteRanking(Long rankingId) {
        deactivateRanking(rankingId);
    }

    // ==================== QUERY CLASSIFICHE ====================

    /**
     * Trova tutte le classifiche attive
     */
    @Transactional(readOnly = true)
    public List<Ranking> findActiveRankings() {
        return rankingRepository.findByIsActiveTrue();
    }

    /**
     * Trova classifiche attive paginate
     */
    @Transactional(readOnly = true)
    public Page<Ranking> findActiveRankings(Pageable pageable) {
        return rankingRepository.findByIsActiveTrue(pageable);
    }

    /**
     * Trova classifiche per città
     */
    @Transactional(readOnly = true)
    public List<Ranking> findByCity(String city) {
        return rankingRepository.findByCityAndIsActiveTrue(city);
    }

    /**
     * Trova classifiche per scope e periodo
     */
    @Transactional(readOnly = true)
    public List<Ranking> findByScopeAndPeriod(RankingScope scope, RankingPeriod period) {
        return rankingRepository.findByScopeAndPeriod(scope, period);
    }

    /**
     * Trova classifica attiva per città e periodo
     */
    @Transactional(readOnly = true)
    public Optional<Ranking> findActiveByCityAndPeriod(String city, RankingPeriod period) {
        return rankingRepository.findActiveByCityAndPeriod(city, period);
    }

    /**
     * Trova classifiche con filtri
     */
    @Transactional(readOnly = true)
    public Page<Ranking> findByFilters(RankingScope scope, RankingPeriod period,
                                        String cuisineType, String city, Pageable pageable) {
        return rankingRepository.findByFilters(scope, period, cuisineType, city, pageable);
    }

    // ==================== GESTIONE ENTRIES ====================

    /**
     * Aggiunge un ristorante alla classifica
     */
    public RankingEntry addRestaurantToRanking(Long rankingId, Long restaurantId) {
        Ranking ranking = findById(rankingId);
        Restaurant restaurant = restaurantDAO.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found with ID: " + restaurantId));

        // Verifica se già presente
        if (rankingEntryRepository.existsByRankingIdAndRestaurantId(rankingId, restaurantId)) {
            log.warn("Ristorante {} già presente in classifica {}", restaurantId, rankingId);
            return rankingEntryRepository.findByRankingIdAndRestaurantId(rankingId, restaurantId)
                    .orElseThrow();
        }

        // Calcola posizione iniziale (ultima)
        Integer maxPosition = rankingEntryRepository.getMaxPosition(rankingId);
        int newPosition = (maxPosition != null) ? maxPosition + 1 : 1;

        RankingEntry entry = RankingEntry.builder()
                .ranking(ranking)
                .restaurant(restaurant)
                .position(newPosition)
                .score(BigDecimal.ZERO)
                .totalVotes(0)
                .localVotes(0)
                .touristVotes(0)
                .verifiedVotes(0)
                .seatedReservations(0)
                .calculatedAt(LocalDateTime.now())
                .build();

        entry = rankingEntryRepository.save(entry);
        log.info("Ristorante {} aggiunto a classifica {} in posizione {}", restaurantId, rankingId, newPosition);

        return entry;
    }

    /**
     * Rimuove un ristorante dalla classifica
     */
    public void removeRestaurantFromRanking(Long rankingId, Long restaurantId) {
        RankingEntry entry = rankingEntryRepository.findByRankingIdAndRestaurantId(rankingId, restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("RankingEntry not found for restaurant: " + restaurantId));

        int removedPosition = entry.getPosition();
        rankingEntryRepository.delete(entry);

        // Ricalcola posizioni successive
        List<RankingEntry> entriesAfter = rankingEntryRepository.findByPositionRange(rankingId, removedPosition + 1, Integer.MAX_VALUE);
        for (RankingEntry e : entriesAfter) {
            e.setPosition(e.getPosition() - 1);
            rankingEntryRepository.save(e);
        }

        log.info("Ristorante {} rimosso da classifica {}", restaurantId, rankingId);
    }

    /**
     * Ottieni entry di un ristorante in una classifica
     */
    @Transactional(readOnly = true)
    public Optional<RankingEntry> getRestaurantEntry(Long rankingId, Long restaurantId) {
        return rankingEntryRepository.findByRankingIdAndRestaurantId(rankingId, restaurantId);
    }

    /**
     * Ottieni posizione di un ristorante
     */
    @Transactional(readOnly = true)
    public Optional<Integer> getRestaurantPosition(Long rankingId, Long restaurantId) {
        return rankingEntryRepository.getRestaurantPosition(rankingId, restaurantId);
    }

    // ==================== QUERY ENTRIES ====================

    /**
     * Ottieni top N della classifica
     */
    @Transactional(readOnly = true)
    public List<RankingEntry> getTopEntries(Long rankingId, int topN) {
        return rankingEntryRepository.findTopN(rankingId, topN);
    }

    /**
     * Ottieni podio (top 3)
     */
    @Transactional(readOnly = true)
    public List<RankingEntry> getPodium(Long rankingId) {
        return rankingEntryRepository.findPodium(rankingId);
    }

    /**
     * Ottieni leader (primo classificato)
     */
    @Transactional(readOnly = true)
    public Optional<RankingEntry> getLeader(Long rankingId) {
        return rankingEntryRepository.findLeader(rankingId);
    }

    /**
     * Ottieni entries paginate
     */
    @Transactional(readOnly = true)
    public Page<RankingEntry> getEntries(Long rankingId, Pageable pageable) {
        return rankingEntryRepository.findByRankingOrderedByPosition(rankingId, pageable);
    }

    /**
     * Ottieni nuove entries (senza posizione precedente)
     */
    @Transactional(readOnly = true)
    public List<RankingEntry> getNewEntries(Long rankingId) {
        return rankingEntryRepository.findNewEntries(rankingId);
    }

    /**
     * Ottieni i migliori scalatori
     */
    @Transactional(readOnly = true)
    public List<RankingEntry> getBiggestClimbers(Long rankingId, int limit) {
        return rankingEntryRepository.findBiggestClimbers(rankingId, PageRequest.of(0, limit));
    }

    /**
     * Ottieni classifiche attive di un ristorante
     */
    @Transactional(readOnly = true)
    public List<RankingEntry> getRestaurantActiveRankings(Long restaurantId) {
        return rankingEntryRepository.findActiveByRestaurant(restaurantId);
    }

    // ==================== CALCOLO SCORE ====================

    /**
     * Ricalcola score e posizioni di tutta la classifica
     */
    public void recalculateRanking(Long rankingId) {
        log.info("Ricalcolo classifica {}", rankingId);
        Ranking ranking = findById(rankingId);

        List<RankingEntry> entries = rankingEntryRepository.findByRankingOrderedByPosition(rankingId);
        if (entries.isEmpty()) {
            log.warn("Classifica {} vuota, nessun calcolo", rankingId);
            return;
        }

        // Calcola score per ogni entry
        for (RankingEntry entry : entries) {
            BigDecimal score = calculateScore(entry);
            entry.setPreviousPosition(entry.getPosition());
            entry.setScore(score);
            entry.setCalculatedAt(LocalDateTime.now());
        }

        // Ordina per score decrescente e assegna posizioni
        entries.sort((a, b) -> b.getScore().compareTo(a.getScore()));

        int position = 1;
        for (RankingEntry entry : entries) {
            entry.setPosition(position++);
            rankingEntryRepository.save(entry);
        }

        // Aggiorna timestamp calcolo ranking
        ranking.setLastCalculatedAt(LocalDateTime.now());
        rankingRepository.save(ranking);

        // Pubblica evento
        publishEvent(EventType.RANKING_UPDATED, "RANKING", ranking.getId(),
                buildRankingPayload(ranking));

        log.info("Classifica {} ricalcolata con {} entries", rankingId, entries.size());
    }

    /**
     * Calcola score per un entry (formula ponderata)
     */
    private BigDecimal calculateScore(RankingEntry entry) {
        BigDecimal voteWeight = new BigDecimal("10");
        BigDecimal ratingWeight = new BigDecimal("20");
        BigDecimal reservationWeight = new BigDecimal("5");
        BigDecimal verifiedBonus = new BigDecimal("1.5");
        BigDecimal localBonus = new BigDecimal("1.2");

        BigDecimal score = BigDecimal.ZERO;

        // Componente voti
        int totalVotes = entry.getTotalVotes() != null ? entry.getTotalVotes() : 0;
        score = score.add(BigDecimal.valueOf(totalVotes).multiply(voteWeight));

        // Componente rating
        BigDecimal avgRating = entry.getAverageRating();
        if (avgRating != null && avgRating.compareTo(BigDecimal.ZERO) > 0) {
            score = score.add(avgRating.multiply(ratingWeight));
        }

        // Componente prenotazioni
        int reservations = entry.getSeatedReservations() != null ? entry.getSeatedReservations() : 0;
        score = score.add(BigDecimal.valueOf(reservations).multiply(reservationWeight));

        // Bonus voti verificati
        int verifiedVotes = entry.getVerifiedVotes() != null ? entry.getVerifiedVotes() : 0;
        if (verifiedVotes > 0) {
            score = score.add(BigDecimal.valueOf(verifiedVotes).multiply(verifiedBonus));
        }

        // Bonus voti locali
        int localVotes = entry.getLocalVotes() != null ? entry.getLocalVotes() : 0;
        if (localVotes > 0) {
            score = score.add(BigDecimal.valueOf(localVotes).multiply(localBonus));
        }

        return score.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Aggiorna statistiche voti per un entry
     */
    public void updateEntryVotes(Long rankingId, Long restaurantId, 
                                  int votesToAdd, boolean isLocal, boolean isVerified) {
        RankingEntry entry = rankingEntryRepository.findByRankingIdAndRestaurantId(rankingId, restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("RankingEntry not found for restaurant: " + restaurantId));

        entry.setTotalVotes(entry.getTotalVotes() + votesToAdd);
        if (isLocal) {
            entry.setLocalVotes(entry.getLocalVotes() + votesToAdd);
        } else {
            entry.setTouristVotes(entry.getTouristVotes() + votesToAdd);
        }
        if (isVerified) {
            entry.setVerifiedVotes(entry.getVerifiedVotes() + votesToAdd);
        }

        rankingEntryRepository.save(entry);
    }

    /**
     * Aggiorna rating medio di un entry
     */
    public void updateEntryRating(Long rankingId, Long restaurantId, BigDecimal newAverageRating) {
        RankingEntry entry = rankingEntryRepository.findByRankingIdAndRestaurantId(rankingId, restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("RankingEntry not found for restaurant: " + restaurantId));

        entry.setAverageRating(newAverageRating);
        rankingEntryRepository.save(entry);
    }

    /**
     * Incrementa prenotazioni SEATED
     */
    public void incrementSeatedReservations(Long rankingId, Long restaurantId) {
        RankingEntry entry = rankingEntryRepository.findByRankingIdAndRestaurantId(rankingId, restaurantId)
                .orElse(null);
        
        if (entry != null) {
            entry.setSeatedReservations(entry.getSeatedReservations() + 1);
            rankingEntryRepository.save(entry);
        }
    }

    // ==================== STATISTICHE ====================

    /**
     * Statistiche globali classifiche
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getGlobalStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActiveRankings", rankingRepository.countActive());
        stats.put("byScope", rankingRepository.countByScope());
        stats.put("byPeriod", rankingRepository.countByPeriod());
        return stats;
    }

    /**
     * Statistiche di una classifica
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRankingStats(Long rankingId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", rankingEntryRepository.countByRankingId(rankingId));
        stats.put("totalVotes", rankingEntryRepository.getTotalVotes(rankingId));
        stats.put("averageScore", rankingEntryRepository.getAverageScore(rankingId));
        return stats;
    }

    // ==================== UTILITY PRIVATE ====================

    /**
     * Imposta date inizio/fine in base al periodo
     */
    private void setRankingPeriodDates(Ranking ranking, RankingPeriod period) {
        LocalDate today = LocalDate.now();
        
        switch (period) {
            case WEEKLY:
                ranking.setPeriodStart(today.with(java.time.DayOfWeek.MONDAY));
                ranking.setPeriodEnd(today.with(java.time.DayOfWeek.SUNDAY));
                break;
            case MONTHLY:
                ranking.setPeriodStart(today.withDayOfMonth(1));
                ranking.setPeriodEnd(today.withDayOfMonth(today.lengthOfMonth()));
                break;
            case QUARTERLY:
                int quarter = (today.getMonthValue() - 1) / 3;
                LocalDate quarterStart = today.withMonth(quarter * 3 + 1).withDayOfMonth(1);
                LocalDate quarterEnd = quarterStart.plusMonths(3).minusDays(1);
                ranking.setPeriodStart(quarterStart);
                ranking.setPeriodEnd(quarterEnd);
                break;
            case YEARLY:
                ranking.setPeriodStart(today.withDayOfYear(1));
                ranking.setPeriodEnd(today.withMonth(12).withDayOfMonth(31));
                break;
            case ALL_TIME:
                // Nessuna data limite
                break;
            default:
                break;
        }
    }

    /**
     * Pubblica evento
     */
    private void publishEvent(EventType eventType, String aggregateType, Long aggregateId, String payload) {
        String eventId = UUID.randomUUID().toString();
        
        EventOutbox event = EventOutbox.builder()
                .eventId(eventId)
                .eventType(eventType.name())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payload)
                .status(EventOutbox.Status.PENDING)
                .createdAt(Instant.now())
                .build();
        
        eventOutboxDAO.save(event);
        log.debug("Published event {}: {}", eventType, eventId);
    }

    /**
     * Build ranking payload
     */
    private String buildRankingPayload(Ranking ranking) {
        return String.format(
                "{\"rankingId\":%d,\"name\":\"%s\",\"scope\":\"%s\",\"period\":\"%s\",\"entriesCount\":%d}",
                ranking.getId(),
                ranking.getName(),
                ranking.getScope().name(),
                ranking.getPeriod().name(),
                ranking.getEntries().size()
        );
    }
}
