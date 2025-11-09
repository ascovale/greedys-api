package com.application.agency.persistence.model.user;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AgencyUserOptions for storing user preferences and settings.
 * Similar to RUserOptions but for agency users.
 */
@Entity
@Table(name = "agency_user_options")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyUserOptions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Notification preferences
    @Builder.Default
    private Boolean emailNotifications = true;

    @Builder.Default
    private Boolean pushNotifications = true;

    @Builder.Default
    private Boolean smsNotifications = false;

    // UI preferences
    private String language;
    
    private String timezone;
    
    @Builder.Default
    private String theme = "light";

    // Business preferences
    @Builder.Default
    private Boolean autoConfirmReservations = false;

    @Builder.Default
    private Integer defaultGroupSize = 10;

    private String preferredContactMethod; // EMAIL, PHONE, SMS
}