package com.application.web.dto.get.notification;

import java.time.Instant;
import java.util.Map;

import com.application.persistence.model.notification.RNotificationType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;


@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantNotificationDTO {
    /** id della notifica */
    private Long id;

    /** id dell'utente a cui è indirizzata */
    private Long rUserId;

    /** timestamp di creazione */
    private Instant creationTime;

    /** tipo di notifica (enum) */
    private RNotificationType type;

    /** titolo della notifica */
    private String title;

    /** corpo del messaggio */
    private String body;

    /** eventuali dati aggiuntivi */
    private Map<String, String> data;

    /** stato di lettura della notifica */
    private Boolean isRead; 
}
