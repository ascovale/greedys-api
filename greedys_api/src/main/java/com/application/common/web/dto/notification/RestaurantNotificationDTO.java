package com.application.common.web.dto.notification;

import java.time.Instant;
import java.util.Map;

import com.application.common.persistence.model.notification.RestaurantNotification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RestaurantNotificationDTO", description = "DTO for restaurant notification details")
public class RestaurantNotificationDTO {

    private Long id;
    private String title;
    private String body;
    private Map<String, String> properties;
    private Boolean read;
    private Instant creationTime;
    private Long rUserId;
    private Long restaurantId;

    public RestaurantNotificationDTO(RestaurantNotification notification) {
        this.id = notification.getId();
        this.title = notification.getTitle();
        this.body = notification.getBody();
        this.properties = notification.getProperties();
        this.read = notification.getRead();
        this.creationTime = notification.getCreationTime();
        this.rUserId = notification.getUserId();
        this.restaurantId = notification.getRestaurantId();
    }

    public static RestaurantNotificationDTO toDTO(RestaurantNotification notification) {
        return new RestaurantNotificationDTO(notification);
    }
}
