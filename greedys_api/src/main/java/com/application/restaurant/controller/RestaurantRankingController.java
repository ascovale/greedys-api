package com.application.restaurant.controller;

import com.application.challenge.persistence.model.Ranking;
import com.application.challenge.persistence.model.RankingEntry;
import com.application.common.service.challenge.RankingService;
import com.application.restaurant.persistence.model.user.RUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Restaurant Ranking Controller
 * Handles ranking operations for restaurants (view rankings, see own position)
 */
@RestController
@RequestMapping("/restaurant/rankings")
@RequiredArgsConstructor
@Slf4j
public class RestaurantRankingController {

    private final RankingService rankingService;

    // ==================== RANKING DISCOVERY ====================

    /**
     * Get active rankings
     */
    @GetMapping
    public ResponseEntity<Page<Ranking>> getActiveRankings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Fetching active rankings for restaurants");
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
     * Get ranking entries (paginated)
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
     * Get restaurant's position in a ranking
     */
    @GetMapping("/{rankingId}/my-position")
    public ResponseEntity<RestaurantPositionResponse> getMyPosition(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long rankingId) {
        Long restaurantId = restaurantUser.getRestaurant().getId();
        log.debug("Fetching position for restaurant {} in ranking {}", restaurantId, rankingId);
        
        Optional<RankingEntry> entry = rankingService.getRestaurantEntry(rankingId, restaurantId);
        
        if (entry.isPresent()) {
            RankingEntry e = entry.get();
            return ResponseEntity.ok(new RestaurantPositionResponse(
                    true,
                    e.getPosition(),
                    e.getScore(),
                    e.getTotalVotes(),
                    e.getAverageRating(),
                    rankingId,
                    restaurantId
            ));
        } else {
            return ResponseEntity.ok(new RestaurantPositionResponse(
                    false, null, null, null, null, rankingId, restaurantId
            ));
        }
    }

    /**
     * Get restaurant's entries across all active rankings
     */
    @GetMapping("/my-entries")
    public ResponseEntity<List<RankingEntry>> getMyEntries(
            @AuthenticationPrincipal RUser restaurantUser) {
        Long restaurantId = restaurantUser.getRestaurant().getId();
        log.debug("Fetching all ranking entries for restaurant: {}", restaurantId);
        List<RankingEntry> entries = rankingService.getRestaurantActiveRankings(restaurantId);
        return ResponseEntity.ok(entries);
    }

    // ==================== STATISTICS ====================

    /**
     * Get ranking statistics
     */
    @GetMapping("/{rankingId}/statistics")
    public ResponseEntity<Map<String, Object>> getRankingStatistics(@PathVariable Long rankingId) {
        log.debug("Fetching statistics for ranking: {}", rankingId);
        Map<String, Object> stats = rankingService.getRankingStats(rankingId);
        return ResponseEntity.ok(stats);
    }

    // ==================== DTOs ====================

    public record RestaurantPositionResponse(
            boolean isRanked,
            Integer position,
            java.math.BigDecimal score,
            Integer totalVotes,
            java.math.BigDecimal averageRating,
            Long rankingId,
            Long restaurantId
    ) {}
}
