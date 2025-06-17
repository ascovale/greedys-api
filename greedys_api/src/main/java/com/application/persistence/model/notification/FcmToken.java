package com.application.persistence.model.notification;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.CreationTimestamp;

@MappedSuperclass
public abstract class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "fcm_token", nullable = false, length = 512)
    private String fcmToken;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected FcmToken(String fcmToken, String deviceId) {
        this.fcmToken = fcmToken;
        this.deviceId = deviceId;
    }

    public Long getId() {
        return id;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
