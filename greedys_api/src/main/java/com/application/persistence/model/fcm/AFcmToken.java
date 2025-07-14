package com.application.persistence.model.fcm;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.hibernate.annotations.CreationTimestamp;

@SuperBuilder
@Getter
@Setter
@MappedSuperclass
public abstract class AFcmToken {

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

    protected AFcmToken(String fcmToken, String deviceId) {
        this.fcmToken = fcmToken;
        this.deviceId = deviceId;
    }

}
