package com.application.common.service.social;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.domain.event.EventType;
import com.application.common.persistence.dao.EventOutboxDAO;
import com.application.common.persistence.dao.social.SocialCommentDAO;
import com.application.common.persistence.dao.social.SocialFollowDAO;
import com.application.common.persistence.dao.social.SocialPostDAO;
import com.application.common.persistence.dao.social.SocialReactionDAO;
import com.application.common.persistence.model.notification.EventOutbox;
import com.application.common.persistence.model.social.FollowStatus;
import com.application.common.persistence.model.social.PostType;
import com.application.common.persistence.model.social.PostVisibility;
import com.application.common.persistence.model.social.ReactionType;
import com.application.common.persistence.model.social.SocialComment;
import com.application.common.persistence.model.social.SocialFollow;
import com.application.common.persistence.model.social.SocialPost;
import com.application.common.persistence.model.social.SocialReaction;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ SOCIAL POST SERVICE
 * 
 * Service per la gestione del feed social.
 * 
 * FEATURES:
 * - Creazione/modifica/eliminazione post
 * - Feed personalizzato
 * - Interazioni (like, commenti, condivisioni)
 * - Follow/Unfollow
 * - Notifiche social
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SocialPostService {

    private final SocialPostDAO postDAO;
    private final SocialCommentDAO commentDAO;
    private final SocialReactionDAO reactionDAO;
    private final SocialFollowDAO followDAO;
    private final EventOutboxDAO eventOutboxDAO;
    private final ObjectMapper objectMapper;

    // ==================== POST MANAGEMENT ====================

    /**
     * Crea un nuovo post
     */
    public SocialPost createPost(
            Long authorId,
            String authorType,
            String content,
            PostType postType,
            PostVisibility visibility,
            Long restaurantId,
            Long eventId
    ) {
        SocialPost post = new SocialPost();
        post.setAuthorId(authorId);
        post.setAuthorType(authorType);
        post.setContent(content);
        post.setPostType(postType != null ? postType : PostType.REGULAR);
        post.setVisibility(visibility != null ? visibility : PostVisibility.PUBLIC);
        post.setRestaurantId(restaurantId);
        post.setEventId(eventId);
        post.setPublishedAt(Instant.now());
        
        post = postDAO.save(post);
        
        // Notifica followers
        notifyFollowersNewPost(post);
        
        // Se è taggato un ristorante, notifica
        if (restaurantId != null) {
            triggerPostEvent(post, EventType.SOCIAL_NEW_POST, Map.of("restaurantTagged", restaurantId));
        }
        
        log.info("✅ Creato post {} da {} ({})", post.getId(), authorId, authorType);
        
        return post;
    }

    /**
     * Crea post check-in
     */
    public SocialPost createCheckinPost(
            Long customerId,
            Long restaurantId,
            String content,
            Long reservationId
    ) {
        SocialPost post = new SocialPost();
        post.setAuthorId(customerId);
        post.setAuthorType("CUSTOMER");
        post.setContent(content);
        post.setPostType(PostType.CHECKIN);
        post.setVisibility(PostVisibility.PUBLIC);
        post.setRestaurantId(restaurantId);
        post.setReservationId(reservationId);
        post.setPublishedAt(Instant.now());
        
        post = postDAO.save(post);
        
        notifyFollowersNewPost(post);
        
        log.info("📍 Check-in post {} - Customer {} @ Restaurant {}", 
            post.getId(), customerId, restaurantId);
        
        return post;
    }

    /**
     * Modifica contenuto post
     */
    public SocialPost updatePost(Long postId, String newContent) {
        SocialPost post = postDAO.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("Post non trovato: " + postId));
        
        post.setContent(newContent);
        return postDAO.save(post);
    }

    /**
     * Elimina post (soft delete)
     */
    public void deletePost(Long postId) {
        SocialPost post = postDAO.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("Post non trovato: " + postId));
        
        post.softDelete();
        postDAO.save(post);
        
        log.info("🗑️ Post {} eliminato", postId);
    }

    /**
     * Pin/Unpin post
     */
    public SocialPost togglePin(Long postId) {
        SocialPost post = postDAO.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("Post non trovato: " + postId));
        
        post.setIsPinned(!post.getIsPinned());
        return postDAO.save(post);
    }

    /**
     * Ottieni post per ID
     */
    @Transactional(readOnly = true)
    public Optional<SocialPost> getPost(Long postId) {
        return postDAO.findById(postId);
    }

    // ==================== FEED ====================

    /**
     * Feed personalizzato per un utente
     */
    @Transactional(readOnly = true)
    public Page<SocialPost> getFeed(Long userId, String userType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postDAO.findFeedForUser(userId, userType, pageable);
    }

    /**
     * Post di un autore specifico
     */
    @Transactional(readOnly = true)
    public Page<SocialPost> getPostsByAuthor(Long authorId, String authorType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postDAO.findByAuthor(authorId, authorType, pageable);
    }

    /**
     * Post pubblici di un ristorante
     */
    @Transactional(readOnly = true)
    public Page<SocialPost> getPostsByRestaurant(Long restaurantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postDAO.findByRestaurantId(restaurantId, pageable);
    }

    /**
     * Post trending
     */
    @Transactional(readOnly = true)
    public Page<SocialPost> getTrendingPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        return postDAO.findTrending(since, pageable);
    }

    /**
     * Cerca post
     */
    @Transactional(readOnly = true)
    public Page<SocialPost> searchPosts(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postDAO.searchByContent(query, pageable);
    }

    // ==================== REACTIONS ====================

    /**
     * Aggiungi/Cambia reazione a un post
     */
    public SocialReaction reactToPost(Long postId, Long userId, ReactionType reactionType) {
        SocialPost post = postDAO.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("Post non trovato: " + postId));
        
        // Verifica se esiste già una reazione
        Optional<SocialReaction> existing = reactionDAO.findByPostIdAndUserId(postId, userId);
        
        SocialReaction reaction;
        if (existing.isPresent()) {
            // Aggiorna reazione esistente
            reaction = existing.get();
            reaction.setReactionType(reactionType);
        } else {
            // Nuova reazione
            reaction = new SocialReaction();
            reaction.setPost(post);
            reaction.setUserId(userId);
            reaction.setReactionType(reactionType);
            
            post.incrementLikes();
            postDAO.save(post);
        }
        
        reaction = reactionDAO.save(reaction);
        
        // Notifica autore del post
        if (!post.getAuthorId().equals(userId)) {
            triggerReactionEvent(post, reaction);
        }
        
        return reaction;
    }

    /**
     * Rimuovi reazione da un post
     */
    public void removeReaction(Long postId, Long userId) {
        Optional<SocialReaction> existing = reactionDAO.findByPostIdAndUserId(postId, userId);
        
        if (existing.isPresent()) {
            reactionDAO.delete(existing.get());
            postDAO.decrementLikes(postId);
        }
    }

    /**
     * Verifica se utente ha reagito
     */
    @Transactional(readOnly = true)
    public boolean hasUserReacted(Long postId, Long userId) {
        return reactionDAO.hasUserReactedToPost(postId, userId);
    }

    // ==================== COMMENTS ====================

    /**
     * Aggiungi commento a un post
     */
    public SocialComment addComment(Long postId, Long authorId, String content, Long parentCommentId) {
        SocialPost post = postDAO.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("Post non trovato: " + postId));
        
        if (!post.getAllowsComments()) {
            throw new IllegalStateException("Commenti disabilitati per questo post");
        }
        
        SocialComment comment = new SocialComment();
        comment.setPost(post);
        comment.setAuthorId(authorId);
        comment.setContent(content);
        
        // Se è una risposta
        if (parentCommentId != null) {
            SocialComment parent = commentDAO.findById(parentCommentId)
                .orElseThrow(() -> new IllegalArgumentException("Commento padre non trovato"));
            comment.setParentComment(parent);
            commentDAO.incrementReplies(parentCommentId);
        }
        
        comment = commentDAO.save(comment);
        
        // Aggiorna contatore
        post.incrementComments();
        postDAO.save(post);
        
        // Notifica autore del post
        if (!post.getAuthorId().equals(authorId)) {
            triggerCommentEvent(post, comment);
        }
        
        return comment;
    }

    /**
     * Modifica commento
     */
    public SocialComment updateComment(Long commentId, String newContent) {
        SocialComment comment = commentDAO.findById(commentId)
            .orElseThrow(() -> new IllegalArgumentException("Commento non trovato: " + commentId));
        
        comment.setContent(newContent);
        comment.setIsEdited(true);
        return commentDAO.save(comment);
    }

    /**
     * Elimina commento (soft delete)
     */
    public void deleteComment(Long commentId) {
        commentDAO.softDelete(commentId, Instant.now());
        
        // Il contatore viene gestito come soft delete (manteniamo il placeholder)
    }

    /**
     * Commenti di un post
     */
    @Transactional(readOnly = true)
    public Page<SocialComment> getPostComments(Long postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return commentDAO.findTopLevelByPostId(postId, pageable);
    }

    /**
     * Risposte a un commento
     */
    @Transactional(readOnly = true)
    public List<SocialComment> getCommentReplies(Long commentId) {
        return commentDAO.findRepliesByParentId(commentId);
    }

    // ==================== FOLLOW ====================

    /**
     * Segui un utente/ristorante
     */
    public SocialFollow follow(
            Long followerId, 
            String followerType,
            Long followingId, 
            String followingType,
            boolean requiresApproval
    ) {
        // Verifica se già esiste
        Optional<SocialFollow> existing = followDAO.findRelation(
            followerId, followerType, followingId, followingType
        );
        
        if (existing.isPresent()) {
            SocialFollow follow = existing.get();
            if (follow.getStatus() == FollowStatus.BLOCKED) {
                throw new IllegalStateException("Non puoi seguire questo utente");
            }
            if (follow.getStatus() == FollowStatus.ACTIVE) {
                return follow; // Già following
            }
            // Riattiva
            follow.setStatus(requiresApproval ? FollowStatus.PENDING : FollowStatus.ACTIVE);
            return followDAO.save(follow);
        }
        
        // Nuovo follow
        SocialFollow follow = new SocialFollow();
        follow.setFollowerId(followerId);
        follow.setFollowerType(followerType);
        follow.setFollowingId(followingId);
        follow.setFollowingType(followingType);
        follow.setStatus(requiresApproval ? FollowStatus.PENDING : FollowStatus.ACTIVE);
        follow.setNotificationsEnabled(true);
        follow.setShowInFeed(true);
        
        follow = followDAO.save(follow);
        
        // Notifica
        triggerFollowEvent(follow);
        
        log.info("👥 {} {} ora segue {} {}", 
            followerType, followerId, followingType, followingId);
        
        return follow;
    }

    /**
     * Smetti di seguire
     */
    public void unfollow(Long followerId, String followerType, Long followingId, String followingType) {
        Optional<SocialFollow> existing = followDAO.findRelation(
            followerId, followerType, followingId, followingType
        );
        
        if (existing.isPresent()) {
            followDAO.delete(existing.get());
        }
    }

    /**
     * Accetta richiesta di follow
     */
    public void acceptFollowRequest(Long followId) {
        followDAO.updateStatus(followId, FollowStatus.ACTIVE, Instant.now());
    }

    /**
     * Rifiuta richiesta di follow
     */
    public void rejectFollowRequest(Long followId) {
        followDAO.deleteById(followId);
    }

    /**
     * Blocca utente
     */
    public void blockUser(Long blockerId, String blockerType, Long blockedId, String blockedType) {
        Optional<SocialFollow> existing = followDAO.findRelation(
            blockerId, blockerType, blockedId, blockedType
        );
        
        if (existing.isPresent()) {
            followDAO.updateStatus(existing.get().getId(), FollowStatus.BLOCKED, Instant.now());
        } else {
            SocialFollow block = new SocialFollow();
            block.setFollowerId(blockerId);
            block.setFollowerType(blockerType);
            block.setFollowingId(blockedId);
            block.setFollowingType(blockedType);
            block.setStatus(FollowStatus.BLOCKED);
            followDAO.save(block);
        }
    }

    /**
     * Verifica se segue
     */
    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, String followerType, Long followingId, String followingType) {
        return followDAO.isFollowing(followerId, followerType, followingId, followingType);
    }

    /**
     * Lista followers
     */
    @Transactional(readOnly = true)
    public Page<SocialFollow> getFollowers(Long userId, String userType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return followDAO.findFollowers(userId, userType, pageable);
    }

    /**
     * Lista following
     */
    @Transactional(readOnly = true)
    public Page<SocialFollow> getFollowing(Long userId, String userType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return followDAO.findFollowing(userId, userType, pageable);
    }

    /**
     * Conta followers/following
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getFollowCounts(Long userId, String userType) {
        return Map.of(
            "followers", followDAO.countFollowers(userId, userType),
            "following", followDAO.countFollowing(userId, userType)
        );
    }

    // ==================== SHARE ====================

    /**
     * Condividi un post
     */
    public SocialPost sharePost(Long originalPostId, Long sharerId, String sharerType, String comment) {
        SocialPost original = postDAO.findById(originalPostId)
            .orElseThrow(() -> new IllegalArgumentException("Post originale non trovato"));
        
        SocialPost shared = new SocialPost();
        shared.setAuthorId(sharerId);
        shared.setAuthorType(sharerType);
        shared.setContent(comment);
        shared.setPostType(PostType.SHARE);
        shared.setVisibility(PostVisibility.PUBLIC);
        shared.setSharedFromPost(original);
        shared.setPublishedAt(Instant.now());
        
        shared = postDAO.save(shared);
        
        // Incrementa contatore condivisioni
        original.incrementShares();
        postDAO.save(original);
        
        // Notifica autore originale
        triggerShareEvent(original, shared);
        
        return shared;
    }

    // ==================== VIEWS ====================

    /**
     * Registra visualizzazione
     */
    public void recordView(Long postId) {
        postDAO.incrementViews(postId);
    }

    // ==================== NOTIFICATIONS ====================

    private void notifyFollowersNewPost(SocialPost post) {
        try {
            // Ottieni followers con notifiche attive
            List<SocialFollow> followers = followDAO.findFollowersWithNotifications(
                post.getAuthorId(), 
                post.getAuthorType()
            );
            
            for (SocialFollow follower : followers) {
                String payload = objectMapper.writeValueAsString(Map.of(
                    "postId", post.getId(),
                    "authorId", post.getAuthorId(),
                    "authorType", post.getAuthorType(),
                    "postType", post.getPostType().name(),
                    "recipientId", follower.getFollowerId(),
                    "recipientType", follower.getFollowerType()
                ));
                
                EventOutbox outbox = EventOutbox.builder()
                    .eventId("social_post_" + post.getId() + "_follower_" + follower.getFollowerId())
                    .eventType(EventType.SOCIAL_NEW_POST.name())
                    .aggregateType("SocialPost")
                    .aggregateId(post.getId())
                    .payload(payload)
                    .build();
                
                eventOutboxDAO.save(outbox);
            }
            
            log.debug("Notificati {} followers per nuovo post {}", followers.size(), post.getId());
            
        } catch (Exception e) {
            log.error("Errore notifica followers: {}", e.getMessage());
        }
    }

    private void triggerPostEvent(SocialPost post, EventType eventType, Map<String, Object> extra) {
        try {
            Map<String, Object> data = new java.util.HashMap<>(Map.of(
                "postId", post.getId(),
                "authorId", post.getAuthorId(),
                "authorType", post.getAuthorType()
            ));
            data.putAll(extra);
            
            String payload = objectMapper.writeValueAsString(data);
            
            EventOutbox outbox = EventOutbox.builder()
                .eventId("social_" + post.getId() + "_" + System.currentTimeMillis())
                .eventType(eventType.name())
                .aggregateType("SocialPost")
                .aggregateId(post.getId())
                .payload(payload)
                .build();
            
            eventOutboxDAO.save(outbox);
        } catch (Exception e) {
            log.error("Errore creazione evento post: {}", e.getMessage());
        }
    }

    private void triggerReactionEvent(SocialPost post, SocialReaction reaction) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "postId", post.getId(),
                "postAuthorId", post.getAuthorId(),
                "postAuthorType", post.getAuthorType(),
                "reactionUserId", reaction.getUserId(),
                "reactionType", reaction.getReactionType().name()
            ));
            
            EventOutbox outbox = EventOutbox.builder()
                .eventId("social_reaction_" + reaction.getId())
                .eventType(EventType.SOCIAL_POST_LIKED.name())
                .aggregateType("SocialReaction")
                .aggregateId(reaction.getId())
                .payload(payload)
                .build();
            
            eventOutboxDAO.save(outbox);
        } catch (Exception e) {
            log.error("Errore creazione evento reaction: {}", e.getMessage());
        }
    }

    private void triggerCommentEvent(SocialPost post, SocialComment comment) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "postId", post.getId(),
                "postAuthorId", post.getAuthorId(),
                "postAuthorType", post.getAuthorType(),
                "commentId", comment.getId(),
                "commentAuthorId", comment.getAuthorId()
            ));
            
            EventOutbox outbox = EventOutbox.builder()
                .eventId("social_comment_" + comment.getId())
                .eventType(EventType.SOCIAL_POST_COMMENTED.name())
                .aggregateType("SocialComment")
                .aggregateId(comment.getId())
                .payload(payload)
                .build();
            
            eventOutboxDAO.save(outbox);
        } catch (Exception e) {
            log.error("Errore creazione evento comment: {}", e.getMessage());
        }
    }

    private void triggerFollowEvent(SocialFollow follow) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "followId", follow.getId(),
                "followerId", follow.getFollowerId(),
                "followerType", follow.getFollowerType(),
                "followingId", follow.getFollowingId(),
                "followingType", follow.getFollowingType(),
                "status", follow.getStatus().name()
            ));
            
            EventOutbox outbox = EventOutbox.builder()
                .eventId("social_follow_" + follow.getId())
                .eventType(EventType.SOCIAL_NEW_FOLLOWER.name())
                .aggregateType("SocialFollow")
                .aggregateId(follow.getId())
                .payload(payload)
                .build();
            
            eventOutboxDAO.save(outbox);
        } catch (Exception e) {
            log.error("Errore creazione evento follow: {}", e.getMessage());
        }
    }

    private void triggerShareEvent(SocialPost original, SocialPost shared) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "originalPostId", original.getId(),
                "originalAuthorId", original.getAuthorId(),
                "originalAuthorType", original.getAuthorType(),
                "sharedPostId", shared.getId(),
                "sharerId", shared.getAuthorId(),
                "sharerType", shared.getAuthorType()
            ));
            
            EventOutbox outbox = EventOutbox.builder()
                .eventId("social_share_" + shared.getId())
                .eventType(EventType.SOCIAL_POST_SHARED.name())
                .aggregateType("SocialPost")
                .aggregateId(shared.getId())
                .payload(payload)
                .build();
            
            eventOutboxDAO.save(outbox);
        } catch (Exception e) {
            log.error("Errore creazione evento share: {}", e.getMessage());
        }
    }
}
