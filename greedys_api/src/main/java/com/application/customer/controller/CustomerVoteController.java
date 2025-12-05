package com.application.customer.controller;

import com.application.challenge.persistence.model.MatchVote;
import com.application.challenge.persistence.model.RankingVote;
import com.application.challenge.persistence.model.enums.VoterType;
import com.application.common.service.challenge.VoteService;
import com.application.customer.persistence.model.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Customer Vote Controller
 * Handles voting operations for customers (vote in matches, rankings)
 */
@RestController
@RequestMapping("/customer/votes")
@RequiredArgsConstructor
@Slf4j
public class CustomerVoteController {

    private final VoteService voteService;

    // ==================== MATCH VOTING ====================

    /**
     * Cast a vote in a tournament match
     */
    @PostMapping("/match")
    public ResponseEntity<MatchVote> castMatchVote(
            @AuthenticationPrincipal Customer customer,
            @RequestBody CastMatchVoteRequest request,
            HttpServletRequest httpRequest) {
        log.info("Customer {} casting match vote for match {} - restaurant {}",
                customer.getId(), request.matchId(), request.votedRestaurantId());
        MatchVote vote = voteService.voteInMatch(
                request.matchId(),
                customer.getId(),
                request.votedRestaurantId(),
                request.voterType() != null ? request.voterType() : VoterType.TOURIST,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(vote);
    }

    /**
     * Get customer's vote in a specific match
     */
    @GetMapping("/match/{matchId}/my-vote")
    public ResponseEntity<MatchVote> getMyMatchVote(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long matchId) {
        log.debug("Fetching vote for customer {} in match {}", customer.getId(), matchId);
        MatchVote vote = voteService.getCustomerVoteInMatch(matchId, customer.getId());
        if (vote != null) {
            return ResponseEntity.ok(vote);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Check if customer can vote in a match
     */
    @GetMapping("/match/{matchId}/can-vote")
    public ResponseEntity<CanVoteResponse> canVoteInMatch(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long matchId) {
        log.debug("Checking if customer {} can vote in match {}", customer.getId(), matchId);
        boolean canVote = voteService.canVoteInMatch(matchId, customer.getId());
        return ResponseEntity.ok(new CanVoteResponse(canVote, "match", matchId));
    }

    /**
     * Get all votes for a match
     */
    @GetMapping("/match/{matchId}")
    public ResponseEntity<List<MatchVote>> getMatchVotes(@PathVariable Long matchId) {
        log.debug("Fetching all votes for match {}", matchId);
        List<MatchVote> votes = voteService.getMatchVotes(matchId);
        return ResponseEntity.ok(votes);
    }

    /**
     * Get match vote statistics
     */
    @GetMapping("/match/{matchId}/statistics")
    public ResponseEntity<Map<String, Object>> getMatchVoteStatistics(@PathVariable Long matchId) {
        log.debug("Fetching vote statistics for match {}", matchId);
        Map<String, Object> stats = voteService.getMatchVoteStatistics(matchId);
        return ResponseEntity.ok(stats);
    }

    // ==================== RANKING VOTING ====================

    /**
     * Cast a vote in a ranking
     */
    @PostMapping("/ranking")
    public ResponseEntity<RankingVote> castRankingVote(
            @AuthenticationPrincipal Customer customer,
            @RequestBody CastRankingVoteRequest request,
            HttpServletRequest httpRequest) {
        log.info("Customer {} casting ranking vote for ranking {} - restaurant {} with rating {}",
                customer.getId(), request.rankingId(), request.votedRestaurantId(), request.rating());
        RankingVote vote = voteService.voteInRanking(
                request.rankingId(),
                request.votedRestaurantId(),
                customer.getId(),
                request.reservationId(),
                request.rating(),
                request.voterType() != null ? request.voterType() : VoterType.TOURIST,
                httpRequest.getRemoteAddr(),
                request.comment()
        );
        return ResponseEntity.ok(vote);
    }

    /**
     * Get customer's vote for a restaurant in a ranking
     */
    @GetMapping("/ranking/{rankingId}/restaurant/{restaurantId}/my-vote")
    public ResponseEntity<RankingVote> getMyRankingVote(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long rankingId,
            @PathVariable Long restaurantId) {
        log.debug("Fetching vote for customer {} for restaurant {} in ranking {}", 
                customer.getId(), restaurantId, rankingId);
        RankingVote vote = voteService.getCustomerVoteInRanking(rankingId, restaurantId, customer.getId());
        if (vote != null) {
            return ResponseEntity.ok(vote);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Check if customer can vote for a restaurant in a ranking
     */
    @GetMapping("/ranking/{rankingId}/restaurant/{restaurantId}/can-vote")
    public ResponseEntity<CanVoteResponse> canVoteInRanking(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long rankingId,
            @PathVariable Long restaurantId) {
        log.debug("Checking if customer {} can vote for restaurant {} in ranking {}",
                customer.getId(), restaurantId, rankingId);
        boolean canVote = voteService.canVoteInRanking(rankingId, restaurantId, customer.getId());
        return ResponseEntity.ok(new CanVoteResponse(canVote, "ranking", rankingId));
    }

    /**
     * Get votes for a restaurant in a ranking
     */
    @GetMapping("/ranking/{rankingId}/restaurant/{restaurantId}")
    public ResponseEntity<List<RankingVote>> getRankingVotesForRestaurant(
            @PathVariable Long rankingId,
            @PathVariable Long restaurantId) {
        log.debug("Fetching votes for restaurant {} in ranking {}", restaurantId, rankingId);
        List<RankingVote> votes = voteService.getRankingVotesForRestaurant(rankingId, restaurantId);
        return ResponseEntity.ok(votes);
    }

    /**
     * Get ranking vote statistics
     */
    @GetMapping("/ranking/{rankingId}/restaurant/{restaurantId}/statistics")
    public ResponseEntity<Map<String, Object>> getRankingVoteStatistics(
            @PathVariable Long rankingId,
            @PathVariable Long restaurantId) {
        log.debug("Fetching vote statistics for restaurant {} in ranking {}", restaurantId, rankingId);
        Map<String, Object> stats = voteService.getRankingVoteStatistics(rankingId, restaurantId);
        return ResponseEntity.ok(stats);
    }

    // ==================== DTOs ====================

    public record CastMatchVoteRequest(
            Long matchId,
            Long votedRestaurantId,
            VoterType voterType
    ) {}

    public record CastRankingVoteRequest(
            Long rankingId,
            Long votedRestaurantId,
            Long reservationId,
            Integer rating,
            VoterType voterType,
            String comment
    ) {}

    public record CanVoteResponse(
            boolean canVote,
            String voteType,
            Long referenceId
    ) {}
}
