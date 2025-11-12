package com.application.common.persistence.model.notification.messaging.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private NotificationType type;
    private Long aggregateId;         // Chi riceve (Long) - es: restaurantId
    private String aggregateType;     // Tipo di entità che riceve - es: RESTAURANT
    private String title;
    private String body;
    private Map<String, String> metadata;
    private Instant timestamp;

    public enum NotificationType {
        RESERVATION_CREATED,
        RESERVATION_ACCEPTED,
        RESERVATION_REJECTED,
        RESERVATION_MODIFIED,
        RESERVATION_CANCELLED,
        RESERVATION_NO_SHOW,
        CHAT_MESSAGE_RECEIVED,
        SYSTEM_ALERT
    }
}
