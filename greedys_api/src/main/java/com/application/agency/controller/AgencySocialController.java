package com.application.agency.controller;

import com.application.agency.persistence.model.user.AgencyUser;
import com.application.common.persistence.model.social.PostType;
import com.application.common.persistence.model.social.PostVisibility;
import com.application.common.persistence.model.social.ReactionType;
import com.application.common.persistence.model.social.SocialComment;
import com.application.common.persistence.model.social.SocialFollow;
import com.application.common.persistence.model.social.SocialPost;
import com.application.common.persistence.model.social.SocialReaction;
import com.application.common.service.social.SocialPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Agency Social Controller
 * Handles social feed operations for agency users
 */
@RestController
@RequestMapping("/agency/social")
@RequiredArgsConstructor
@Slf4j
public class AgencySocialController {

    private final SocialPostService socialPostService;
    private static final String USER_TYPE_AGENCY = "AGENCY";

    // ==================== FEED ====================

    /**
     * Get social feed
     */
    @GetMapping("/feed")
    public ResponseEntity<Page<SocialPost>> getFeed(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Agency user {} fetching social feed", agencyUser.getId());
        Page<SocialPost> feed = socialPostService.getFeed(agencyUser.getId(), USER_TYPE_AGENCY, page, size);
        return ResponseEntity.ok(feed);
    }

    /**
     * Get posts from a specific restaurant
     */
    @GetMapping("/restaurants/{restaurantId}/posts")
    public ResponseEntity<Page<SocialPost>> getRestaurantPosts(
            @PathVariable Long restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SocialPost> posts = socialPostService.getPostsByRestaurant(restaurantId, page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * Get a specific post
     */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<SocialPost> getPost(@PathVariable Long postId) {
        return socialPostService.getPost(postId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get trending posts
     */
    @GetMapping("/trending")
    public ResponseEntity<Page<SocialPost>> getTrendingPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SocialPost> posts = socialPostService.getTrendingPosts(page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * Search posts
     */
    @GetMapping("/search")
    public ResponseEntity<Page<SocialPost>> searchPosts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SocialPost> posts = socialPostService.searchPosts(query, page, size);
        return ResponseEntity.ok(posts);
    }

    // ==================== AGENCY POSTS ====================

    /**
     * Create a post (agencies can post about their managed restaurants)
     */
    @PostMapping("/posts")
    public ResponseEntity<SocialPost> createPost(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @RequestBody CreatePostRequest request) {
        Long agencyId = agencyUser.getAgency() != null ? agencyUser.getAgency().getId() : agencyUser.getId();
        log.info("Agency {} creating post", agencyId);
        SocialPost post = socialPostService.createPost(
                agencyId,
                USER_TYPE_AGENCY,
                request.content(),
                request.postType() != null ? request.postType() : PostType.REGULAR,
                request.visibility() != null ? request.visibility() : PostVisibility.PUBLIC,
                request.restaurantId(),
                request.eventId()
        );
        return ResponseEntity.ok(post);
    }

    /**
     * Get own posts
     */
    @GetMapping("/posts")
    public ResponseEntity<Page<SocialPost>> getMyPosts(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long agencyId = agencyUser.getAgency() != null ? agencyUser.getAgency().getId() : agencyUser.getId();
        Page<SocialPost> posts = socialPostService.getPostsByAuthor(agencyId, USER_TYPE_AGENCY, page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * Delete own post
     */
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long postId) {
        Long agencyId = agencyUser.getAgency() != null ? agencyUser.getAgency().getId() : agencyUser.getId();
        return socialPostService.getPost(postId)
                .filter(post -> post.getAuthorId().equals(agencyId) 
                        && USER_TYPE_AGENCY.equals(post.getAuthorType()))
                .map(post -> {
                    log.info("Agency {} deleting post {}", agencyId, postId);
                    socialPostService.deletePost(postId);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.status(403).build());
    }

    // ==================== REACTIONS ====================

    /**
     * Add a reaction
     */
    @PostMapping("/posts/{postId}/reactions")
    public ResponseEntity<SocialReaction> addReaction(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long postId,
            @RequestBody ReactionRequest request) {
        log.info("Agency user {} adding {} reaction to post {}", 
                agencyUser.getId(), request.type(), postId);
        SocialReaction reaction = socialPostService.reactToPost(
                postId, agencyUser.getId(), request.type());
        return ResponseEntity.ok(reaction);
    }

    /**
     * Remove a reaction
     */
    @DeleteMapping("/posts/{postId}/reactions")
    public ResponseEntity<Void> removeReaction(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long postId) {
        log.info("Agency user {} removing reaction from post {}", agencyUser.getId(), postId);
        socialPostService.removeReaction(postId, agencyUser.getId());
        return ResponseEntity.noContent().build();
    }

    // ==================== COMMENTS ====================

    /**
     * Add a comment
     */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<SocialComment> addComment(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long postId,
            @RequestBody CommentRequest request) {
        log.info("Agency user {} commenting on post {}", agencyUser.getId(), postId);
        SocialComment comment = socialPostService.addComment(
                postId, agencyUser.getId(), request.content(), request.parentCommentId());
        return ResponseEntity.ok(comment);
    }

    /**
     * Delete own comment
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long commentId) {
        log.info("Agency user {} deleting comment {}", agencyUser.getId(), commentId);
        socialPostService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get comments for a post
     */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<Page<SocialComment>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SocialComment> comments = socialPostService.getPostComments(postId, page, size);
        return ResponseEntity.ok(comments);
    }

    // ==================== FOLLOWING ====================

    /**
     * Follow a restaurant
     */
    @PostMapping("/restaurants/{restaurantId}/follow")
    public ResponseEntity<SocialFollow> followRestaurant(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long restaurantId) {
        log.info("Agency user {} following restaurant {}", agencyUser.getId(), restaurantId);
        SocialFollow follow = socialPostService.follow(
                agencyUser.getId(), USER_TYPE_AGENCY,
                restaurantId, "RESTAURANT",
                false
        );
        return ResponseEntity.ok(follow);
    }

    /**
     * Unfollow a restaurant
     */
    @DeleteMapping("/restaurants/{restaurantId}/follow")
    public ResponseEntity<Void> unfollowRestaurant(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long restaurantId) {
        log.info("Agency user {} unfollowing restaurant {}", agencyUser.getId(), restaurantId);
        socialPostService.unfollow(agencyUser.getId(), USER_TYPE_AGENCY, restaurantId, "RESTAURANT");
        return ResponseEntity.noContent().build();
    }

    /**
     * Get following list
     */
    @GetMapping("/following")
    public ResponseEntity<Page<SocialFollow>> getFollowing(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SocialFollow> following = socialPostService.getFollowing(
                agencyUser.getId(), USER_TYPE_AGENCY, page, size);
        return ResponseEntity.ok(following);
    }

    /**
     * Check if following
     */
    @GetMapping("/restaurants/{restaurantId}/following")
    public ResponseEntity<Boolean> isFollowing(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long restaurantId) {
        boolean following = socialPostService.isFollowing(
                agencyUser.getId(), USER_TYPE_AGENCY, restaurantId, "RESTAURANT");
        return ResponseEntity.ok(following);
    }

    /**
     * Get follow counts
     */
    @GetMapping("/follow-counts")
    public ResponseEntity<Map<String, Long>> getFollowCounts(
            @AuthenticationPrincipal AgencyUser agencyUser) {
        Map<String, Long> counts = socialPostService.getFollowCounts(agencyUser.getId(), USER_TYPE_AGENCY);
        return ResponseEntity.ok(counts);
    }

    // ==================== REQUEST DTOs ====================

    public record ReactionRequest(ReactionType type) {}

    public record CommentRequest(
            String content,
            Long parentCommentId
    ) {}

    public record CreatePostRequest(
            String content,
            PostType postType,
            PostVisibility visibility,
            Long restaurantId,
            Long eventId
    ) {}
}
