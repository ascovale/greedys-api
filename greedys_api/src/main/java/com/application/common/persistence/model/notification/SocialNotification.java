package com.application.common.persistence.model.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ⭐ SOCIAL NOTIFICATION ENTITY
 * 
 * Notifica per eventi social (post, like, commenti, follower, eventi).
 * 
 * Estende ANotification con campi specifici per social:
 * - postId, commentId, storyId
 * - actorId, actorName (chi ha generato l'azione)
 * - eventEntityId (per eventi ristorante)
 * - webSocketDestination
 * 
 * FLOW:
 * SocialPostService/RestaurantEventService → EventOutbox → RabbitMQ → SocialOrchestrator → SocialNotification
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "social_notification", indexes = {
    @Index(name = "idx_social_notif_user", columnList = "user_id"),
    @Index(name = "idx_social_notif_post", columnList = "post_id"),
    @Index(name = "idx_social_notif_event_type", columnList = "event_type"),
    @Index(name = "idx_social_notif_created", columnList = "creation_time DESC")
})
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SocialNotification extends ANotification {

    /**
     * Tipo di evento social
     */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /**
     * ID del post (se applicabile)
     */
    @Column(name = "post_id")
    private Long postId;

    /**
     * ID del commento (se applicabile)
     */
    @Column(name = "comment_id")
    private Long commentId;

    /**
     * ID della story (se applicabile)
     */
    @Column(name = "story_id")
    private Long storyId;

    /**
     * ID dell'evento ristorante (se applicabile)
     */
    @Column(name = "event_entity_id")
    private Long eventEntityId;

    /**
     * ID dell'attore che ha generato l'evento (chi ha messo like, commentato, etc)
     */
    @Column(name = "actor_id")
    private Long actorId;

    /**
     * Nome dell'attore (per display)
     */
    @Column(name = "actor_name", length = 100)
    private String actorName;

    /**
     * Canale di notifica (WEBSOCKET, PUSH, EMAIL)
     */
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    /**
     * Event ID disaggregato
     */
    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    /**
     * Destinazione WebSocket per questo evento
     */
    @Column(name = "websocket_destination", length = 200)
    private String webSocketDestination;

    @Override
    public Long getRecipientId() {
        return getUserId();
    }

    @Override
    public String getRecipientType() {
        return "SOCIAL_USER";
    }
}
