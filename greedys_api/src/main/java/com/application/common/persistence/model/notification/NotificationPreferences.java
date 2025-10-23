package com.application.common.persistence.model.notification;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.time.Instant;

@Entity
@Table(name = "notification_preferences", indexes = {
    @Index(name = "idx_user", columnList = "user_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_user", columnNames = {"user_id", "user_type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_type", nullable = false, length = 50)
    private String userType;

    // Channel enable/disable
    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private Boolean emailEnabled = true;

    @Column(name = "push_enabled", nullable = false)
    @Builder.Default
    private Boolean pushEnabled = true;

    @Column(name = "websocket_enabled", nullable = false)
    @Builder.Default
    private Boolean websocketEnabled = true;

    @Column(name = "sms_enabled", nullable = false)
    @Builder.Default
    private Boolean smsEnabled = false;

    // Granular preferences
    @Column(name = "email_reservation", nullable = false)
    @Builder.Default
    private Boolean emailReservation = true;

    @Column(name = "email_chat", nullable = false)
    @Builder.Default
    private Boolean emailChat = true;

    @Column(name = "email_marketing", nullable = false)
    @Builder.Default
    private Boolean emailMarketing = false;

    @Column(name = "push_reservation", nullable = false)
    @Builder.Default
    private Boolean pushReservation = true;

    @Column(name = "push_chat", nullable = false)
    @Builder.Default
    private Boolean pushChat = true;

    // Restaurant-specific
    @Column(name = "notify_all_users")
    @Builder.Default
    private Boolean notifyAllUsers = true;

    @Column(name = "quiet_hours_enabled")
    @Builder.Default
    private Boolean quietHoursEnabled = false;

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
