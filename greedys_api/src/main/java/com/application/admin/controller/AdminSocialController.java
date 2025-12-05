package com.application.admin.controller;

import com.application.admin.persistence.model.Admin;
import com.application.common.persistence.model.social.SocialComment;
import com.application.common.persistence.model.social.SocialFollow;
import com.application.common.persistence.model.social.SocialPost;
import com.application.common.service.social.SocialPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin Social Controller
 * Handles social moderation operations for admins
 */
@RestController
@RequestMapping("/admin/social")
@RequiredArgsConstructor
@Slf4j
public class AdminSocialController {

    private final SocialPostService socialPostService;

    // ==================== POST MODERATION ====================

    /**
     * Get all posts (paginated)
     */
    @GetMapping("/posts")
    public ResponseEntity<Page<SocialPost>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SocialPost> posts = socialPostService.getTrendingPosts(page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * Get posts by a specific author
     */
    @GetMapping("/posts/author/{authorId}")
    public ResponseEntity<Page<SocialPost>> getPostsByAuthor(
            @PathVariable Long authorId,
            @RequestParam String authorType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SocialPost> posts = socialPostService.getPostsByAuthor(authorId, authorType, page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * Get posts by restaurant
     */
    @GetMapping("/posts/restaurant/{restaurantId}")
    public ResponseEntity<Page<SocialPost>> getPostsByRestaurant(
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
     * Search posts
     */
    @GetMapping("/posts/search")
    public ResponseEntity<Page<SocialPost>> searchPosts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SocialPost> posts = socialPostService.searchPosts(query, page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * Delete any post (admin moderation)
     */
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long postId) {
        log.warn("Admin {} deleting post {} (moderation)", admin.getId(), postId);
        socialPostService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Feature/unfeature a post (pin)
     */
    @PostMapping("/posts/{postId}/pin")
    public ResponseEntity<SocialPost> togglePinPost(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long postId) {
        log.info("Admin {} toggling pin on post {}", admin.getId(), postId);
        SocialPost updated = socialPostService.togglePin(postId);
        return ResponseEntity.ok(updated);
    }

    // ==================== COMMENT MODERATION ====================

    /**
     * Get comments for a post
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
     * Get replies to a comment
     */
    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<List<SocialComment>> getCommentReplies(@PathVariable Long commentId) {
        List<SocialComment> replies = socialPostService.getCommentReplies(commentId);
        return ResponseEntity.ok(replies);
    }

    /**
     * Delete any comment (admin moderation)
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long commentId) {
        log.warn("Admin {} deleting comment {} (moderation)", admin.getId(), commentId);
        socialPostService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    // ==================== FOLLOW MANAGEMENT ====================

    /**
     * Get followers for a user
     */
    @GetMapping("/followers/{userId}")
    public ResponseEntity<Page<SocialFollow>> getFollowers(
            @PathVariable Long userId,
            @RequestParam String userType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SocialFollow> followers = socialPostService.getFollowers(userId, userType, page, size);
        return ResponseEntity.ok(followers);
    }

    /**
     * Get following for a user
     */
    @GetMapping("/following/{userId}")
    public ResponseEntity<Page<SocialFollow>> getFollowing(
            @PathVariable Long userId,
            @RequestParam String userType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SocialFollow> following = socialPostService.getFollowing(userId, userType, page, size);
        return ResponseEntity.ok(following);
    }

    /**
     * Block a user from another user's perspective (admin action)
     */
    @PostMapping("/block")
    public ResponseEntity<Void> blockUser(
            @AuthenticationPrincipal Admin admin,
            @RequestBody BlockUserRequest request) {
        log.warn("Admin {} blocking user {} ({}) from {} ({})", 
                admin.getId(), request.blockedId(), request.blockedType(),
                request.blockerId(), request.blockerType());
        socialPostService.blockUser(
                request.blockerId(), request.blockerType(),
                request.blockedId(), request.blockedType()
        );
        return ResponseEntity.ok().build();
    }

    // ==================== REQUEST DTOs ====================

    public record BlockUserRequest(
            Long blockerId,
            String blockerType,
            Long blockedId,
            String blockedType
    ) {}
}
