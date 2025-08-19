package com.application.admin.persistence.model;

import com.application.common.persistence.model.fcm.AFcmToken;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@Entity
public class AdminFcmToken extends AFcmToken {

    @ManyToOne
    @JoinColumn(nullable = false)
    private Admin admin;

    public AdminFcmToken(Admin admin, String fcmToken, String deviceId) {
        super(fcmToken, deviceId);
        this.admin = admin;
    }
}
