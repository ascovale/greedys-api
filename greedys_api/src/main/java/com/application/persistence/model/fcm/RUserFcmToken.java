package com.application.persistence.model.fcm;

import com.application.persistence.model.restaurant.user.RUser;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class RUserFcmToken extends AFcmToken {

    @ManyToOne
    @JoinColumn(nullable = false)
    private RUser rUser;

    public RUserFcmToken(RUser rUser, String fcmToken, String deviceId) {
        super(fcmToken, deviceId);
        this.rUser = rUser;
    }

}