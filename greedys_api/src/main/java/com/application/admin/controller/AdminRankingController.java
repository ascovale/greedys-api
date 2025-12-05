package com.application.admin.controller;

import com.application.challenge.persistence.model.Ranking;
import com.application.challenge.persistence.model.RankingEntry;
import com.application.challenge.persistence.model.enums.RankingPeriod;
import com.application.challenge.persistence.model.enums.RankingScope;
import com.application.common.service.challenge.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Admin Ranking Controller
 * Handles ranking management operations for administrators
 */
@RestController
@RequestMapping("/admin/rankings")
@RequiredArgsConstructor
@Slf4j
public class AdminRankingController {

    private final RankingService rankingService;

    // ==================== CRUD ====================

    /**
     * Create a new ranking
     */
    @PostMapping
    public ResponseEntity<Ranking> createRanking(@RequestBody CreateRankingRequest request) {
        log.info("Admin creating ranking: {} scope={} period={}", request.name(), request.scope(), request.period());
        
        Ranking ranking = rankingService.createRanking(
                request.name(),
                request.description(),
                request.scope(),
                request.period(),
                request.city(),
                request.region(),
                request.zone()
        );
        return ResponseEntity.ok(ranking);
    }

    /**
     * Update an existing ranking
     */
    @PutMapping("/{rankingId}")
    public ResponseEntity<Ranking> updateRanking(
            @PathVariable Long rankingId,
            @RequestBody UpdateRankingRequest request) {
        log.info("Admin updating ranking: {}", rankingId);
        
        Ranking ranking = rankingService.updateRanking(rankingId, request.name(), request.description());
        return ResponseEntity.ok(ranking);
    }

    /**
     * Deactivate a ranking
     */
    @PostMapping("/{rankingId}/deactivate")
    public ResponseEntity<Void> deactivateRanking(@PathVariable Long rankingId) {
        log.info("Admin deactivating ranking: {}", rankingId);
        rankingService.deactivateRanking(rankingId);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete a ranking (soft delete)
     */
    @DeleteMapping("/{rankingId}")
    public ResponseEntity<Void> deleteRanking(@PathVariable Long rankingId) {
        log.info("Admin deleting ranking: {}", rankingId);
        rankingService.deleteRanking(rankingId);
        return ResponseEntity.ok().build();
    }

    // ==================== QUERY ====================

    /**
     * Get all active rankings
     */
    @GetMapping
    public ResponseEntity<Page<Ranking>> getAllRankings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Admin fetching all rankings");
        Page<Ranking> rankings = rankingService.findActiveRankings(PageRequest.of(page, size));
        return ResponseEntity.ok(rankings);
    }

    /**
     * Get ranking by ID
     */
    @GetMapping("/{rankingId}")
    public ResponseEntity<Ranking> getRankingById(@PathVariable Long rankingId) {
        log.debug("Admin fetching ranking: {}", rankingId);
        Ranking ranking = rankingService.findById(rankingId);
        return ResponseEntity.ok(ranking);
    }

    /**
     * Get ranking with entries
     */
    @GetMapping("/{rankingId}/full")
    public ResponseEntity<Ranking> getRankingWithEntries(@PathVariable Long rankingId) {
        log.debug("Admin fetching ranking with entries: {}", rankingId);
        Ranking ranking = rankingService.findByIdWithEntries(rankingId);
        return ResponseEntity.ok(ranking);
    }

    /**
     * Get rankings by scope and period
     */
    @GetMapping("/by-scope-period")
    public ResponseEntity<List<Ranking>> getRankingsByScopeAndPeriod(
            @RequestParam RankingScope scope,
            @RequestParam RankingPeriod period) {
        log.debug("Admin fetching rankings by scope={} period={}", scope, period);
        List<Ranking> rankings = rankingService.findByScopeAndPeriod(scope, period);
        return ResponseEntity.ok(rankings);
    }

    /**
     * Get rankings by city
     */
    @GetMapping("/by-city/{city}")
    public ResponseEntity<List<Ranking>> getRankingsByCity(@PathVariable String city) {
        log.debug("Admin fetching rankings for city: {}", city);
        List<Ranking> rankings = rankingService.findByCity(city);
        return ResponseEntity.ok(rankings);
    }

    /**
     * Get active ranking by city and period
     */
    @GetMapping("/active")
    public ResponseEntity<Ranking> getActiveRanking(
            @RequestParam String city,
            @RequestParam RankingPeriod period) {
        log.debug("Admin fetching active ranking for city={} period={}", city, period);
        Optional<Ranking> ranking = rankingService.findActiveByCityAndPeriod(city, period);
        return ranking.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search rankings with filters
     */
    @GetMapping("/search")
    public ResponseEntity<Page<Ranking>> searchRankings(
            @RequestParam(required = false) RankingScope scope,
            @RequestParam(required = false) RankingPeriod period,
            @RequestParam(required = false) String cuisineType,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Admin searching rankings with filters");
        Page<Ranking> rankings = rankingService.findByFilters(scope, period, cuisineType, city, PageRequest.of(page, size));
        return ResponseEntity.ok(rankings);
    }

    // ==================== ENTRIES MANAGEMENT ====================

    /**
     * Add restaurant to ranking
     */
    @PostMapping("/{rankingId}/restaurants/{restaurantId}")
    public ResponseEntity<RankingEntry> addRestaurantToRanking(
            @PathVariable Long rankingId,
            @PathVariable Long restaurantId) {
        log.info("Admin adding restaurant {} to ranking {}", restaurantId, rankingId);
        RankingEntry entry = rankingService.addRestaurantToRanking(rankingId, restaurantId);
        return ResponseEntity.ok(entry);
    }

    /**
     * Remove restaurant from ranking
     */
    @DeleteMapping("/{rankingId}/restaurants/{restaurantId}")
    public ResponseEntity<Void> removeRestaurantFromRanking(
            @PathVariable Long rankingId,
            @PathVariable Long restaurantId) {
        log.info("Admin removing restaurant {} from ranking {}", restaurantId, rankingId);
        rankingService.removeRestaurantFromRanking(rankingId, restaurantId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get ranking entries
     */
    @GetMapping("/{rankingId}/entries")
    public ResponseEntity<Page<RankingEntry>> getRankingEntries(
            @PathVariable Long rankingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.debug("Admin fetching entries for ranking: {}", rankingId);
        Page<RankingEntry> entries = rankingService.getEntries(rankingId, PageRequest.of(page, size));
        return ResponseEntity.ok(entries);
    }

    /**
     * Get top entries
     */
    @GetMapping("/{rankingId}/top/{limit}")
    public ResponseEntity<List<RankingEntry>> getTopEntries(
            @PathVariable Long rankingId,
            @PathVariable int limit) {
        log.debug("Admin fetching top {} entries for ranking: {}", limit, rankingId);
        List<RankingEntry> entries = rankingService.getTopEntries(rankingId, limit);
        return ResponseEntity.ok(entries);
    }

    /**
     * Get podium
     */
    @GetMapping("/{rankingId}/podium")
    public ResponseEntity<List<RankingEntry>> getPodium(@PathVariable Long rankingId) {
        log.debug("Admin fetching podium for ranking: {}", rankingId);
        List<RankingEntry> entries = rankingService.getPodium(rankingId);
        return ResponseEntity.ok(entries);
    }

    /**
     * Get leader
     */
    @GetMapping("/{rankingId}/leader")
    public ResponseEntity<RankingEntry> getLeader(@PathVariable Long rankingId) {
        log.debug("Admin fetching leader for ranking: {}", rankingId);
        Optional<RankingEntry> leader = rankingService.getLeader(rankingId);
        return leader.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get new entries
     */
    @GetMapping("/{rankingId}/new-entries")
    public ResponseEntity<List<RankingEntry>> getNewEntries(@PathVariable Long rankingId) {
        log.debug("Admin fetching new entries for ranking: {}", rankingId);
        List<RankingEntry> entries = rankingService.getNewEntries(rankingId);
        return ResponseEntity.ok(entries);
    }

    /**
     * Get biggest climbers
     */
    @GetMapping("/{rankingId}/climbers")
    public ResponseEntity<List<RankingEntry>> getBiggestClimbers(
            @PathVariable Long rankingId,
            @RequestParam(defaultValue = "10") int limit) {
        log.debug("Admin fetching biggest climbers for ranking: {}", rankingId);
        List<RankingEntry> entries = rankingService.getBiggestClimbers(rankingId, limit);
        return ResponseEntity.ok(entries);
    }

    // ==================== SCORE MANAGEMENT ====================

    /**
     * Recalculate ranking scores and positions
     */
    @PostMapping("/{rankingId}/recalculate")
    public ResponseEntity<Void> recalculateRanking(@PathVariable Long rankingId) {
        log.info("Admin triggering recalculation for ranking: {}", rankingId);
        rankingService.recalculateRanking(rankingId);
        return ResponseEntity.ok().build();
    }

    /**
     * Update entry votes manually
     */
    @PostMapping("/{rankingId}/restaurants/{restaurantId}/votes")
    public ResponseEntity<Void> updateEntryVotes(
            @PathVariable Long rankingId,
            @PathVariable Long restaurantId,
            @RequestBody UpdateVotesRequest request) {
        log.info("Admin updating votes for restaurant {} in ranking {}", restaurantId, rankingId);
        rankingService.updateEntryVotes(rankingId, restaurantId, request.votesToAdd(), 
                request.isLocal(), request.isVerified());
        return ResponseEntity.ok().build();
    }

    /**
     * Update entry rating manually
     */
    @PostMapping("/{rankingId}/restaurants/{restaurantId}/rating")
    public ResponseEntity<Void> updateEntryRating(
            @PathVariable Long rankingId,
            @PathVariable Long restaurantId,
            @RequestBody UpdateRatingRequest request) {
        log.info("Admin updating rating for restaurant {} in ranking {}", restaurantId, rankingId);
        rankingService.updateEntryRating(rankingId, restaurantId, request.newAverageRating());
        return ResponseEntity.ok().build();
    }

    // ==================== STATISTICS ====================

    /**
     * Get global rankings statistics
     */
    @GetMapping("/statistics/global")
    public ResponseEntity<Map<String, Object>> getGlobalStatistics() {
        log.debug("Admin fetching global rankings statistics");
        Map<String, Object> stats = rankingService.getGlobalStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get ranking statistics
     */
    @GetMapping("/{rankingId}/statistics")
    public ResponseEntity<Map<String, Object>> getRankingStatistics(@PathVariable Long rankingId) {
        log.debug("Admin fetching statistics for ranking: {}", rankingId);
        Map<String, Object> stats = rankingService.getRankingStats(rankingId);
        return ResponseEntity.ok(stats);
    }

    // ==================== DTOs ====================

    public record CreateRankingRequest(
            String name,
            String description,
            RankingScope scope,
            RankingPeriod period,
            String city,
            String region,
            String zone
    ) {}

    public record UpdateRankingRequest(
            String name,
            String description
    ) {}

    public record UpdateVotesRequest(
            int votesToAdd,
            boolean isLocal,
            boolean isVerified
    ) {}

    public record UpdateRatingRequest(
            BigDecimal newAverageRating
    ) {}
}
