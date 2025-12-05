package com.application.common.service.notification.orchestrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.application.common.persistence.dao.social.SocialFollowDAO;
import com.application.common.persistence.model.notification.SocialNotification;
import com.application.common.persistence.model.social.SocialFollow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ SOCIAL ORCHESTRATOR
 * 
 * Orchestratore per la disaggregazione degli eventi social.
 * Riceve eventi dalla coda notification.social.* e li distribuisce ai follower.
 * 
 * FLOW:
 * 1. Riceve evento social da RabbitMQ
 * 2. Carica followers dell'autore
 * 3. Filtra per preferenze notifica
 * 4. Crea notifiche per ogni follower
 * 
 * QUEUES HANDLED:
 * - notification.social.feed: Nuovi post nel feed
 * - notification.social.events: Eventi restaurant (RSVP, reminder)
 * 
 * EVENT TYPES:
 * - SOCIAL_NEW_POST: Nuovo post da seguito
 * - SOCIAL_POST_LIKED: Like al tuo post
 * - SOCIAL_POST_COMMENTED: Commento al tuo post
 * - SOCIAL_POST_SHARED: Condivisione del tuo post
 * - SOCIAL_NEW_FOLLOWER: Nuovo follower
 * - SOCIAL_FOLLOW_ACCEPTED: Follow request accettata
 * - SOCIAL_NEW_STORY: Nuova story da seguito
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SocialOrchestrator extends NotificationOrchestrator<SocialNotification> {

    private final SocialFollowDAO followDAO;

    @Override
    public List<SocialNotification> disaggregateAndProcess(Map<String, Object> message) {
        // ⭐ FIX: Support both camelCase and snake_case keys for compatibility
        String eventType = extractEventType(message);
        
        List<SocialNotification> notifications = new ArrayList<>();
        
        // Carica destinatari in base al tipo di evento
        List<Long> recipients = loadRecipients(message);
        
        if (recipients.isEmpty()) {
            log.warn("⚠️ SocialOrchestrator: no recipients found for eventType={}", eventType);
            return notifications;
        }
        
        for (Long recipientId : recipients) {
            List<String> channels = loadUserPreferences(recipientId);
            
            if (channels.isEmpty()) {
                channels.add("WEBSOCKET");
            }
            
            for (String channel : channels) {
                String disaggregatedEventId = generateDisaggregatedEventId(
                    extractEventId(message), recipientId, channel
                );
                
                SocialNotification notification = createNotificationRecord(
                    disaggregatedEventId, 
                    recipientId, 
                    channel, 
                    message
                );
                
                notifications.add(notification);
            }
        }
        
        log.info("✅ Social disaggregation: {} → {} notifications for {} recipients", 
            eventType, notifications.size(), recipients.size());
        
        return notifications;
    }

    /**
     * Helper: Extract eventType supporting both camelCase and snake_case
     */
    private String extractEventType(Map<String, Object> message) {
        if (message.containsKey("event_type")) {
            return (String) message.get("event_type");
        }
        if (message.containsKey("eventType")) {
            return (String) message.get("eventType");
        }
        throw new IllegalArgumentException("Missing event_type/eventType in message");
    }

    /**
     * Helper: Extract eventId supporting both camelCase and snake_case
     */
    private String extractEventId(Map<String, Object> message) {
        if (message.containsKey("event_id")) {
            return (String) message.get("event_id");
        }
        if (message.containsKey("eventId")) {
            return (String) message.get("eventId");
        }
        throw new IllegalArgumentException("Missing event_id/eventId in message");
    }

    @Override
    protected List<Long> loadRecipients(Map<String, Object> message) {
        Map<String, Object> payload = extractPayloadSafe(message);
        String eventType = extractEventType(message);
        
        List<Long> recipients = new ArrayList<>();
        
        if (payload == null) {
            log.warn("⚠️ SocialOrchestrator: payload is null for eventType={}", eventType);
            return recipients;
        }
        
        switch (eventType) {
            case "SOCIAL_NEW_POST":
            case "SOCIAL_NEW_STORY":
                // Notifica tutti i followers dell'autore
                Long authorId = extractLongSafe(payload, "authorId");
                String authorType = (String) payload.get("authorType");
                
                if (authorId != null) {
                    List<SocialFollow> followers = followDAO.findFollowersWithNotifications(authorId, authorType);
                    for (SocialFollow follow : followers) {
                        recipients.add(follow.getFollowerId());
                    }
                    log.debug("📢 SOCIAL_NEW_POST/STORY: found {} followers for author {}", followers.size(), authorId);
                }
                break;
            
            case "SOCIAL_POST_LIKED":
            case "SOCIAL_POST_COMMENTED":
            case "SOCIAL_POST_SHARED":
            case "SOCIAL_USER_MENTIONED":
                // Notifica l'autore del post
                Long postAuthorId = extractLongSafe(payload, "postAuthorId");
                if (postAuthorId != null) {
                    recipients.add(postAuthorId);
                }
                break;
            
            case "SOCIAL_NEW_FOLLOWER":
            case "SOCIAL_FOLLOW_REQUEST":
                // Notifica la persona seguita
                Long followingId = extractLongSafe(payload, "followingId");
                if (followingId != null) {
                    recipients.add(followingId);
                }
                break;
            
            case "SOCIAL_FOLLOW_ACCEPTED":
                // Notifica chi ha fatto richiesta
                Long followerId = extractLongSafe(payload, "followerId");
                if (followerId != null) {
                    recipients.add(followerId);
                }
                break;
            
            case "SOCIAL_STORY_REPLY":
                // Notifica l'autore della story
                Long storyAuthorId = extractLongSafe(payload, "storyAuthorId");
                if (storyAuthorId != null) {
                    recipients.add(storyAuthorId);
                }
                break;
            
            // Event events (handled by SocialOrchestrator for EVENT_CREATED feed)
            case "EVENT_CREATED":
                // Notifica followers del ristorante
                Long restaurantId = extractLongSafe(payload, "restaurantId");
                if (restaurantId != null) {
                    List<SocialFollow> restaurantFollowers = followDAO.findFollowersWithNotifications(
                        restaurantId, "RESTAURANT"
                    );
                    for (SocialFollow follow : restaurantFollowers) {
                        recipients.add(follow.getFollowerId());
                    }
                    log.debug("📢 EVENT_CREATED: found {} followers for restaurant {}", restaurantFollowers.size(), restaurantId);
                }
                break;
            
            case "EVENT_REMINDER":
            case "EVENT_UPDATED":
            case "EVENT_CANCELLED":
            case "EVENT_RSVP_STATUS_CHANGED":
                // Notifica utente specifico
                Long userId = extractLongSafe(payload, "userId");
                if (userId != null) {
                    recipients.add(userId);
                }
                break;
            
            default:
                // Recipiente specifico se presente
                Long recipientId = extractLongSafe(payload, "recipientId");
                if (recipientId != null) {
                    recipients.add(recipientId);
                } else {
                    log.warn("⚠️ SocialOrchestrator: no recipient found for eventType={}", eventType);
                }
                break;
        }
        
        return recipients;
    }

    /**
     * Helper: Extract payload safely (returns null if not present)
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPayloadSafe(Map<String, Object> message) {
        Object payload = message.get("payload");
        if (payload instanceof Map) {
            return (Map<String, Object>) payload;
        }
        // Also check "data" key (alternative name used by some callers)
        Object data = message.get("data");
        if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        return null;
    }

    /**
     * Helper: Extract Long safely (returns null if not present or not a number)
     */
    private Long extractLongSafe(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    @Override
    protected List<String> loadUserPreferences(Long userId) {
        List<String> channels = new ArrayList<>();
        channels.add("WEBSOCKET");
        channels.add("PUSH");
        
        // TODO: Caricare preferenze utente reali
        
        return channels;
    }

    @Override
    protected Map<String, Object> loadGroupSettings(Map<String, Object> message) {
        return Map.of(
            "notificationsEnabled", true
        );
    }

    @Override
    protected Map<String, Object> loadEventTypeRules(String eventType) {
        switch (eventType) {
            case "SOCIAL_NEW_POST":
            case "SOCIAL_NEW_STORY":
                return Map.of(
                    "mandatory", List.of("WEBSOCKET"),
                    "optional", List.of("PUSH")
                );
            
            case "SOCIAL_POST_LIKED":
            case "SOCIAL_POST_COMMENTED":
            case "SOCIAL_POST_SHARED":
                return Map.of(
                    "mandatory", List.of("WEBSOCKET"),
                    "optional", List.of("PUSH")
                );
            
            case "SOCIAL_NEW_FOLLOWER":
            case "SOCIAL_FOLLOW_REQUEST":
            case "SOCIAL_FOLLOW_ACCEPTED":
                return Map.of(
                    "mandatory", List.of("WEBSOCKET", "PUSH"),
                    "optional", List.of()
                );
            
            case "EVENT_CREATED":
                return Map.of(
                    "mandatory", List.of("WEBSOCKET"),
                    "optional", List.of("PUSH", "EMAIL")
                );
            
            case "EVENT_REMINDER":
                return Map.of(
                    "mandatory", List.of("WEBSOCKET", "PUSH"),
                    "optional", List.of("EMAIL")
                );
            
            case "EVENT_CANCELLED":
                return Map.of(
                    "mandatory", List.of("WEBSOCKET", "PUSH", "EMAIL"),
                    "optional", List.of()
                );
            
            default:
                return Map.of(
                    "mandatory", List.of("WEBSOCKET"),
                    "optional", List.of()
                );
        }
    }

    @Override
    protected SocialNotification createNotificationRecord(
            String eventId, 
            Long userId, 
            String channel, 
            Map<String, Object> message
    ) {
        Map<String, Object> payload = extractPayloadSafe(message);
        String eventType = extractEventType(message);
        
        SocialNotification notification = new SocialNotification();
        notification.setEventId(eventId);
        notification.setUserId(userId);
        notification.setChannel(channel);
        notification.setEventType(eventType);
        
        if (payload != null) {
            // Estrai dettagli specifici
            Long postId = extractLongSafe(payload, "postId");
            if (postId != null) {
                notification.setPostId(postId);
            }
            Long commentId = extractLongSafe(payload, "commentId");
            if (commentId != null) {
                notification.setCommentId(commentId);
            }
            Long storyId = extractLongSafe(payload, "storyId");
            if (storyId != null) {
                notification.setStoryId(storyId);
            }
            // eventId in payload refers to restaurant event, not notification event
            Long eventEntityId = extractLongSafe(payload, "eventId");
            if (eventEntityId != null) {
                notification.setEventEntityId(eventEntityId);
            }
            Long actorId = extractLongSafe(payload, "actorId");
            if (actorId != null) {
                notification.setActorId(actorId);
            }
            if (payload.containsKey("actorName")) {
                notification.setActorName((String) payload.get("actorName"));
            }
        }
        
        // Imposta titolo e body
        notification.setTitle(generateTitle(eventType, payload));
        notification.setBody(generateBody(eventType, payload));
        
        // WebSocket destination
        notification.setWebSocketDestination(generateWebSocketDestination(eventType, payload, userId));
        
        return notification;
    }

    /**
     * Genera titolo della notifica
     */
    private String generateTitle(String eventType, Map<String, Object> payload) {
        if (payload == null) {
            return "Nuova notifica";
        }
        String actorName = payload.containsKey("actorName") 
            ? (String) payload.get("actorName") 
            : payload.containsKey("sharerName") 
                ? (String) payload.get("sharerName") 
                : payload.containsKey("followerName")
                    ? (String) payload.get("followerName")
                    : "Qualcuno";
        
        switch (eventType) {
            case "SOCIAL_NEW_POST":
                String authorName = payload.containsKey("authorName") 
                    ? (String) payload.get("authorName") : "Qualcuno";
                return authorName + " ha pubblicato un nuovo post";
            
            case "SOCIAL_POST_LIKED":
                return actorName + " ha messo like al tuo post";
            
            case "SOCIAL_POST_COMMENTED":
                return actorName + " ha commentato il tuo post";
            
            case "SOCIAL_POST_SHARED":
                return actorName + " ha condiviso il tuo post";
            
            case "SOCIAL_USER_MENTIONED":
                return actorName + " ti ha menzionato";
            
            case "SOCIAL_NEW_FOLLOWER":
                return actorName + " ha iniziato a seguirti";
            
            case "SOCIAL_FOLLOW_REQUEST":
                return actorName + " vuole seguirti";
            
            case "SOCIAL_FOLLOW_ACCEPTED":
                String followingName = payload.containsKey("followingName") 
                    ? (String) payload.get("followingName") : "Qualcuno";
                return followingName + " ha accettato la tua richiesta";
            
            case "SOCIAL_NEW_STORY":
                String storyAuthor = payload.containsKey("authorName") 
                    ? (String) payload.get("authorName") : "Qualcuno";
                return storyAuthor + " ha aggiunto una story";
            
            case "EVENT_CREATED":
                String restaurantName = payload.containsKey("restaurantName") 
                    ? (String) payload.get("restaurantName") : "Un ristorante";
                return "Nuovo evento da " + restaurantName;
            
            case "EVENT_REMINDER":
                String eventTitle = payload.containsKey("title") 
                    ? (String) payload.get("title") : "L'evento";
                return "Promemoria: " + eventTitle;
            
            case "EVENT_UPDATED":
                return "Evento aggiornato";
            
            case "EVENT_CANCELLED":
                return "Evento annullato";
            
            case "EVENT_RSVP_STATUS_CHANGED":
                return "Aggiornamento partecipazione";
            
            default:
                return "Notifica social";
        }
    }

    /**
     * Genera body della notifica
     */
    private String generateBody(String eventType, Map<String, Object> payload) {
        if (payload == null) {
            return "";
        }
        switch (eventType) {
            case "SOCIAL_NEW_POST":
            case "SOCIAL_POST_COMMENTED":
                if (payload.containsKey("content")) {
                    return truncateContent((String) payload.get("content"), 150);
                }
                break;
            
            case "EVENT_CREATED":
            case "EVENT_REMINDER":
                if (payload.containsKey("eventDate")) {
                    return "Data: " + payload.get("eventDate");
                }
                break;
            
            case "EVENT_CANCELLED":
                if (payload.containsKey("message")) {
                    return (String) payload.get("message");
                }
                break;
        }
        return "";
    }

    /**
     * Genera destinazione WebSocket
     */
    private String generateWebSocketDestination(String eventType, Map<String, Object> payload, Long userId) {
        switch (eventType) {
            case "SOCIAL_NEW_POST":
            case "SOCIAL_POST_LIKED":
            case "SOCIAL_POST_COMMENTED":
            case "SOCIAL_POST_SHARED":
                return "/topic/social/feed/" + userId;
            
            case "SOCIAL_NEW_FOLLOWER":
            case "SOCIAL_FOLLOW_REQUEST":
            case "SOCIAL_FOLLOW_ACCEPTED":
                return "/topic/social/followers/" + userId;
            
            case "SOCIAL_NEW_STORY":
            case "SOCIAL_STORY_REPLY":
                return "/topic/social/stories/" + userId;
            
            case "SOCIAL_USER_MENTIONED":
                return "/topic/social/mentions/" + userId;
            
            case "EVENT_CREATED":
            case "EVENT_REMINDER":
            case "EVENT_UPDATED":
            case "EVENT_CANCELLED":
            case "EVENT_RSVP_STATUS_CHANGED":
                return "/topic/event/" + userId;
            
            default:
                return "/topic/social/" + userId;
        }
    }

    /**
     * Tronca contenuto per preview
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength - 3) + "...";
    }
}
