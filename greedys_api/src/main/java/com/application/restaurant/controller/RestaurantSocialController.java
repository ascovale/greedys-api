package com.application.restaurant.controller;

import com.application.common.persistence.model.social.PostType;
import com.application.common.persistence.model.social.PostVisibility;
import com.application.common.persistence.model.social.SocialComment;
import com.application.common.persistence.model.social.SocialFollow;
import com.application.common.persistence.model.social.SocialPost;
import com.application.common.service.social.SocialPostService;
import com.application.restaurant.persistence.model.user.RUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Restaurant Social Controller
 * Handles social feed operations for restaurants (can create posts, manage followers)
 */
@RestController
@RequestMapping("/restaurant/social")
@RequiredArgsConstructor
@Slf4j
public class RestaurantSocialController {

    private final SocialPostService socialPostService;
    private static final String USER_TYPE_RESTAURANT = "RESTAURANT";

    // ==================== POST MANAGEMENT ====================

    /**
     * Create a new social post
     */
    @PostMapping("/posts")
    public ResponseEntity<SocialPost> createPost(
            @AuthenticationPrincipal RUser restaurantUser,
            @RequestBody CreatePostRequest request) {
        log.info("Restaurant {} creating post", restaurantUser.getRestaurant().getId());
        SocialPost post = socialPostService.createPost(
                restaurantUser.getRestaurant().getId(),  // Use restaurant ID as author
                USER_TYPE_RESTAURANT,
                request.content(),
                request.postType() != null ? request.postType() : PostType.REGULAR,
                request.visibility() != null ? request.visibility() : PostVisibility.PUBLIC,
                restaurantUser.getRestaurant().getId(),  // Also set restaurantId
                request.eventId()
        );
        return ResponseEntity.ok(post);
    }

    /**
     * Update a post
     */
    @PutMapping("/posts/{postId}")
    public ResponseEntity<SocialPost> updatePost(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long postId,
            @RequestBody UpdatePostRequest request) {
        return socialPostService.getPost(postId)
                .filter(post -> post.getAuthorId().equals(restaurantUser.getRestaurant().getId()) 
                        && USER_TYPE_RESTAURANT.equals(post.getAuthorType()))
                .map(post -> {
                    log.info("Restaurant {} updating post {}", restaurantUser.getRestaurant().getId(), postId);
                    SocialPost updated = socialPostService.updatePost(postId, request.content());
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Delete a post
     */
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long postId) {
        return socialPostService.getPost(postId)
                .filter(post -> post.getAuthorId().equals(restaurantUser.getRestaurant().getId()) 
                        && USER_TYPE_RESTAURANT.equals(post.getAuthorType()))
                .map(post -> {
                    log.info("Restaurant {} deleting post {}", restaurantUser.getRestaurant().getId(), postId);
                    socialPostService.deletePost(postId);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Pin/Unpin a post
     */
    @PostMapping("/posts/{postId}/pin")
    public ResponseEntity<SocialPost> togglePinPost(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long postId) {
        return socialPostService.getPost(postId)
                .filter(post -> post.getAuthorId().equals(restaurantUser.getRestaurant().getId()) 
                        && USER_TYPE_RESTAURANT.equals(post.getAuthorType()))
                .map(post -> {
                    log.info("Restaurant {} toggling pin on post {}", restaurantUser.getRestaurant().getId(), postId);
                    SocialPost updated = socialPostService.togglePin(postId);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.status(403).build());
    }

    // ==================== VIEW MY POSTS ====================

    /**
     * Get restaurant's own posts
     */
    @GetMapping("/posts")
    public ResponseEntity<Page<SocialPost>> getMyPosts(
            @AuthenticationPrincipal RUser restaurantUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SocialPost> posts = socialPostService.getPostsByAuthor(
                restaurantUser.getRestaurant().getId(), USER_TYPE_RESTAURANT, page, size);
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

    // ==================== FOLLOWERS ====================

    /**
     * Get restaurant's followers
     */
    @GetMapping("/followers")
    public ResponseEntity<Page<SocialFollow>> getFollowers(
            @AuthenticationPrincipal RUser restaurantUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SocialFollow> followers = socialPostService.getFollowers(
                restaurantUser.getRestaurant().getId(), USER_TYPE_RESTAURANT, page, size);
        return ResponseEntity.ok(followers);
    }

    /**
     * Get follower/following counts
     */
    @GetMapping("/follow-counts")
    public ResponseEntity<Map<String, Long>> getFollowCounts(
            @AuthenticationPrincipal RUser restaurantUser) {
        Map<String, Long> counts = socialPostService.getFollowCounts(
                restaurantUser.getRestaurant().getId(), USER_TYPE_RESTAURANT);
        return ResponseEntity.ok(counts);
    }

    /**
     * Accept a follow request (if restaurant requires approval)
     */
    @PostMapping("/followers/{followId}/accept")
    public ResponseEntity<Void> acceptFollowRequest(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long followId) {
        log.info("Restaurant {} accepting follow request {}", restaurantUser.getRestaurant().getId(), followId);
        socialPostService.acceptFollowRequest(followId);
        return ResponseEntity.ok().build();
    }

    /**
     * Reject a follow request
     */
    @PostMapping("/followers/{followId}/reject")
    public ResponseEntity<Void> rejectFollowRequest(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long followId) {
        log.info("Restaurant {} rejecting follow request {}", restaurantUser.getRestaurant().getId(), followId);
        socialPostService.rejectFollowRequest(followId);
        return ResponseEntity.ok().build();
    }

    /**
     * Block a user
     */
    @PostMapping("/block/{userId}")
    public ResponseEntity<Void> blockUser(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long userId,
            @RequestParam String userType) {
        log.info("Restaurant {} blocking user {} ({})", restaurantUser.getRestaurant().getId(), userId, userType);
        socialPostService.blockUser(
                restaurantUser.getRestaurant().getId(), USER_TYPE_RESTAURANT,
                userId, userType
        );
        return ResponseEntity.ok().build();
    }

    // ==================== COMMENTS MODERATION ====================

    /**
     * Get comments on restaurant's posts
     */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<Page<SocialComment>> getPostComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SocialComment> comments = socialPostService.getPostComments(postId, page, size);
        return ResponseEntity.ok(comments);
    }

    /**
     * Delete a comment on restaurant's own post (moderation)
     */
    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        return socialPostService.getPost(postId)
                .filter(post -> post.getAuthorId().equals(restaurantUser.getRestaurant().getId()) 
                        && USER_TYPE_RESTAURANT.equals(post.getAuthorType()))
                .map(post -> {
                    log.info("Restaurant {} deleting comment {} on post {}", 
                            restaurantUser.getRestaurant().getId(), commentId, postId);
                    socialPostService.deleteComment(commentId);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Reply to a comment on restaurant's post
     */
    @PostMapping("/posts/{postId}/comments/{commentId}/reply")
    public ResponseEntity<SocialComment> replyToComment(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody CommentRequest request) {
        return socialPostService.getPost(postId)
                .filter(post -> post.getAuthorId().equals(restaurantUser.getRestaurant().getId()) 
                        && USER_TYPE_RESTAURANT.equals(post.getAuthorType()))
                .map(post -> {
                    SocialComment reply = socialPostService.addComment(
                            postId, restaurantUser.getRestaurant().getId(), request.content(), commentId);
                    return ResponseEntity.ok(reply);
                })
                .orElse(ResponseEntity.status(403).build());
    }

    // ==================== FEED & DISCOVERY ====================

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

    // ==================== REQUEST DTOs ====================

    public record CreatePostRequest(
            String content,
            PostType postType,
            PostVisibility visibility,
            Long eventId
    ) {}

    public record UpdatePostRequest(String content) {}

    public record CommentRequest(String content) {}
}
