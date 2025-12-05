package com.application.customer.controller;

import com.application.challenge.persistence.model.Ranking;
import com.application.challenge.persistence.model.RankingEntry;
import com.application.common.service.challenge.RankingService;
import com.application.customer.persistence.model.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Customer Ranking Controller
 * Handles ranking operations for customers (view rankings, leaderboards)
 */
@RestController
@RequestMapping("/customer/rankings")
@RequiredArgsConstructor
@Slf4j
public class CustomerRankingController {

    private final RankingService rankingService;

    // ==================== RANKING DISCOVERY ====================

    /**
     * Get active rankings
     */
    @GetMapping
    public ResponseEntity<Page<Ranking>> getActiveRankings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Fetching active rankings");
        Page<Ranking> rankings = rankingService.findActiveRankings(PageRequest.of(page, size));
        return ResponseEntity.ok(rankings);
    }

    /**
     * Get ranking details by ID
     */
    @GetMapping("/{rankingId}")
    public ResponseEntity<Ranking> getRankingDetails(@PathVariable Long rankingId) {
        log.debug("Fetching ranking details: {}", rankingId);
        Ranking ranking = rankingService.findById(rankingId);
        return ResponseEntity.ok(ranking);
    }

    /**
     * Get rankings by city
     */
    @GetMapping("/by-city/{city}")
    public ResponseEntity<List<Ranking>> getRankingsByCity(@PathVariable String city) {
        log.debug("Fetching rankings for city: {}", city);
        List<Ranking> rankings = rankingService.findByCity(city);
        return ResponseEntity.ok(rankings);
    }

    // ==================== LEADERBOARD ====================

    /**
     * Get leaderboard (top entries) for a ranking
     */
    @GetMapping("/{rankingId}/leaderboard")
    public ResponseEntity<List<RankingEntry>> getLeaderboard(
            @PathVariable Long rankingId,
            @RequestParam(defaultValue = "10") int limit) {
        log.debug("Fetching leaderboard for ranking: {} (top {})", rankingId, limit);
        List<RankingEntry> entries = rankingService.getTopEntries(rankingId, limit);
        return ResponseEntity.ok(entries);
    }

    /**
     * Get podium (top 3)
     */
    @GetMapping("/{rankingId}/podium")
    public ResponseEntity<List<RankingEntry>> getPodium(@PathVariable Long rankingId) {
        log.debug("Fetching podium for ranking: {}", rankingId);
        List<RankingEntry> entries = rankingService.getPodium(rankingId);
        return ResponseEntity.ok(entries);
    }

    /**
     * Get full ranking entries (paginated)
     */
    @GetMapping("/{rankingId}/entries")
    public ResponseEntity<Page<RankingEntry>> getRankingEntries(
            @PathVariable Long rankingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.debug("Fetching entries for ranking: {}", rankingId);
        Page<RankingEntry> entries = rankingService.getEntries(rankingId, PageRequest.of(page, size));
        return ResponseEntity.ok(entries);
    }

    // ==================== MY POSITION ====================

    /**
     * Get customer's ranking entries across all active rankings
     */
    @GetMapping("/my-entries")
    public ResponseEntity<List<RankingEntry>> getMyEntries(
            @AuthenticationPrincipal Customer customer) {
        log.debug("Fetching all ranking entries for customer: {}", customer.getId());
        // Note: Customer is viewed as "restaurant" in rankings, this would need adaptation
        // For now, returns an empty list - rankings are for restaurants, not customers
        return ResponseEntity.ok(List.of());
    }

    /**
     * Get ranking statistics
     */
    @GetMapping("/{rankingId}/statistics")
    public ResponseEntity<?> getRankingStatistics(@PathVariable Long rankingId) {
        log.debug("Fetching statistics for ranking: {}", rankingId);
        return ResponseEntity.ok(rankingService.getRankingStats(rankingId));
    }

    /**
     * Get global rankings statistics
     */
    @GetMapping("/statistics/global")
    public ResponseEntity<?> getGlobalStatistics() {
        log.debug("Fetching global rankings statistics");
        return ResponseEntity.ok(rankingService.getGlobalStats());
    }
}
