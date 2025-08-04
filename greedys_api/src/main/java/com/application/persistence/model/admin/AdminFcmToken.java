package com.application.persistence.model.admin;

import java.time.LocalDateTime;

import com.application.persistence.model.fcm.AFcmToken;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@Entity
public class AdminFcmToken extends AFcmToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Admin admin;

    @Column(nullable = false)
    private String fcmToken;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private String deviceId;
}