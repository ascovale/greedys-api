package com.application.common.web.dto.notification;

import java.time.Instant;
import java.util.Map;

import com.application.customer.persistence.model.CustomerNotification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CustomerNotificationDTO", description = "DTO for customer notification details")
public class CustomerNotificationDTO {

    private Long id;
    private String title;
    private String body;
    private Map<String, String> properties;
    private Boolean read;
    private Instant creationTime;
    private Long customerId;

    public CustomerNotificationDTO(CustomerNotification notification) {
        this.id = notification.getId();
        this.title = notification.getTitle();
        this.body = notification.getBody();
        this.properties = notification.getProperties();
        this.read = notification.getRead();
        this.creationTime = notification.getCreationTime();
        this.customerId = notification.getCustomer() != null ? notification.getCustomer().getId() : null;
    }

    public static CustomerNotificationDTO toDTO(CustomerNotification notification) {
        return new CustomerNotificationDTO(notification);
    }
}
