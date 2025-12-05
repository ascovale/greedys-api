package com.application.customer.controller;

import com.application.common.persistence.model.social.PostType;
import com.application.common.persistence.model.social.PostVisibility;
import com.application.common.persistence.model.social.ReactionType;
import com.application.common.persistence.model.social.SocialComment;
import com.application.common.persistence.model.social.SocialFollow;
import com.application.common.persistence.model.social.SocialPost;
import com.application.common.persistence.model.social.SocialReaction;
import com.application.common.service.social.SocialPostService;
import com.application.customer.persistence.model.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Customer Social Controller
 * Handles social feed operations for customers (read-only posts, can interact)
 */
@RestController
@RequestMapping("/customer/social")
@RequiredArgsConstructor
@Slf4j
public class CustomerSocialController {

    private final SocialPostService socialPostService;
    private static final String USER_TYPE_CUSTOMER = "CUSTOMER";

    // ==================== FEED ====================

    /**
     * Get personalized feed for the customer
     */
    @GetMapping("/feed")
    public ResponseEntity<Page<SocialPost>> getFeed(
            @AuthenticationPrincipal Customer customer,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Customer {} fetching social feed", customer.getId());
        Page<SocialPost> feed = socialPostService.getFeed(customer.getId(), USER_TYPE_CUSTOMER, page, size);
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

    // ==================== CUSTOMER POSTS (Check-in) ====================

    /**
     * Create a check-in post at a restaurant
     */
    @PostMapping("/checkin")
    public ResponseEntity<SocialPost> createCheckin(
            @AuthenticationPrincipal Customer customer,
            @RequestBody CheckinRequest request) {
        log.info("Customer {} checking in at restaurant {}", customer.getId(), request.restaurantId());
        SocialPost post = socialPostService.createCheckinPost(
                customer.getId(),
                request.restaurantId(),
                request.content(),
                request.reservationId()
        );
        return ResponseEntity.ok(post);
    }

    /**
     * Create a general social post
     */
    @PostMapping("/posts")
    public ResponseEntity<SocialPost> createPost(
            @AuthenticationPrincipal Customer customer,
            @RequestBody CreatePostRequest request) {
        log.info("Customer {} creating post", customer.getId());
        SocialPost post = socialPostService.createPost(
                customer.getId(),
                USER_TYPE_CUSTOMER,
                request.content(),
                request.postType() != null ? request.postType() : PostType.REGULAR,
                request.visibility() != null ? request.visibility() : PostVisibility.PUBLIC,
                request.restaurantId(),
                request.eventId()
        );
        return ResponseEntity.ok(post);
    }

    /**
     * Delete own post
     */
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long postId) {
        return socialPostService.getPost(postId)
                .filter(post -> post.getAuthorId().equals(customer.getId()) 
                        && USER_TYPE_CUSTOMER.equals(post.getAuthorType()))
                .map(post -> {
                    log.info("Customer {} deleting post {}", customer.getId(), postId);
                    socialPostService.deletePost(postId);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.status(403).build());
    }

    // ==================== REACTIONS ====================

    /**
     * Add a reaction to a post
     */
    @PostMapping("/posts/{postId}/reactions")
    public ResponseEntity<SocialReaction> addReaction(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long postId,
            @RequestBody ReactionRequest request) {
        log.info("Customer {} adding {} reaction to post {}", 
                customer.getId(), request.type(), postId);
        SocialReaction reaction = socialPostService.reactToPost(
                postId, customer.getId(), request.type());
        return ResponseEntity.ok(reaction);
    }

    /**
     * Remove a reaction from a post
     */
    @DeleteMapping("/posts/{postId}/reactions")
    public ResponseEntity<Void> removeReaction(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long postId) {
        log.info("Customer {} removing reaction from post {}", customer.getId(), postId);
        socialPostService.removeReaction(postId, customer.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if user has reacted to a post
     */
    @GetMapping("/posts/{postId}/reactions/me")
    public ResponseEntity<Boolean> hasReacted(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long postId) {
        boolean hasReacted = socialPostService.hasUserReacted(postId, customer.getId());
        return ResponseEntity.ok(hasReacted);
    }

    // ==================== COMMENTS ====================

    /**
     * Add a comment to a post
     */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<SocialComment> addComment(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long postId,
            @RequestBody CommentRequest request) {
        log.info("Customer {} commenting on post {}", customer.getId(), postId);
        SocialComment comment = socialPostService.addComment(
                postId, customer.getId(), request.content(), request.parentCommentId());
        return ResponseEntity.ok(comment);
    }

    /**
     * Edit own comment
     */
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<SocialComment> editComment(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long commentId,
            @RequestBody CommentRequest request) {
        // Note: Would need to add getCommentById to service and verify ownership
        log.info("Customer {} editing comment {}", customer.getId(), commentId);
        SocialComment updated = socialPostService.updateComment(commentId, request.content());
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete own comment
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long commentId) {
        log.info("Customer {} deleting comment {}", customer.getId(), commentId);
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

    /**
     * Get replies to a comment
     */
    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<List<SocialComment>> getCommentReplies(@PathVariable Long commentId) {
        List<SocialComment> replies = socialPostService.getCommentReplies(commentId);
        return ResponseEntity.ok(replies);
    }

    // ==================== FOLLOWING ====================

    /**
     * Follow a restaurant
     */
    @PostMapping("/restaurants/{restaurantId}/follow")
    public ResponseEntity<SocialFollow> followRestaurant(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long restaurantId) {
        log.info("Customer {} following restaurant {}", customer.getId(), restaurantId);
        SocialFollow follow = socialPostService.follow(
                customer.getId(), USER_TYPE_CUSTOMER,
                restaurantId, "RESTAURANT",
                false  // no approval required for following restaurants
        );
        return ResponseEntity.ok(follow);
    }

    /**
     * Unfollow a restaurant
     */
    @DeleteMapping("/restaurants/{restaurantId}/follow")
    public ResponseEntity<Void> unfollowRestaurant(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long restaurantId) {
        log.info("Customer {} unfollowing restaurant {}", customer.getId(), restaurantId);
        socialPostService.unfollow(customer.getId(), USER_TYPE_CUSTOMER, restaurantId, "RESTAURANT");
        return ResponseEntity.noContent().build();
    }

    /**
     * Get restaurants the customer is following
     */
    @GetMapping("/following")
    public ResponseEntity<Page<SocialFollow>> getFollowing(
            @AuthenticationPrincipal Customer customer,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SocialFollow> following = socialPostService.getFollowing(
                customer.getId(), USER_TYPE_CUSTOMER, page, size);
        return ResponseEntity.ok(following);
    }

    /**
     * Check if following a restaurant
     */
    @GetMapping("/restaurants/{restaurantId}/following")
    public ResponseEntity<Boolean> isFollowing(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long restaurantId) {
        boolean following = socialPostService.isFollowing(
                customer.getId(), USER_TYPE_CUSTOMER, restaurantId, "RESTAURANT");
        return ResponseEntity.ok(following);
    }

    /**
     * Get follow counts
     */
    @GetMapping("/follow-counts")
    public ResponseEntity<Map<String, Long>> getFollowCounts(
            @AuthenticationPrincipal Customer customer) {
        Map<String, Long> counts = socialPostService.getFollowCounts(customer.getId(), USER_TYPE_CUSTOMER);
        return ResponseEntity.ok(counts);
    }

    // ==================== SHARING ====================

    /**
     * Share a post
     */
    @PostMapping("/posts/{postId}/share")
    public ResponseEntity<SocialPost> sharePost(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long postId,
            @RequestBody(required = false) ShareRequest request) {
        log.info("Customer {} sharing post {}", customer.getId(), postId);
        SocialPost shared = socialPostService.sharePost(
                postId, 
                customer.getId(), 
                USER_TYPE_CUSTOMER, 
                request != null ? request.comment() : null
        );
        return ResponseEntity.ok(shared);
    }

    // ==================== VIEW TRACKING ====================

    /**
     * Record a post view
     */
    @PostMapping("/posts/{postId}/view")
    public ResponseEntity<Void> recordView(@PathVariable Long postId) {
        socialPostService.recordView(postId);
        return ResponseEntity.ok().build();
    }

    // ==================== REQUEST DTOs ====================

    public record ReactionRequest(ReactionType type) {}

    public record CommentRequest(
            String content,
            Long parentCommentId
    ) {}

    public record CheckinRequest(
            Long restaurantId,
            String content,
            Long reservationId
    ) {}

    public record CreatePostRequest(
            String content,
            PostType postType,
            PostVisibility visibility,
            Long restaurantId,
            Long eventId
    ) {}

    public record ShareRequest(
            String comment
    ) {}
}
