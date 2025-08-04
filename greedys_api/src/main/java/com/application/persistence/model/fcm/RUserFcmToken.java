package com.application.persistence.model.fcm;

import com.application.persistence.model.restaurant.user.RUser;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
public class RUserFcmToken extends AFcmToken {

    @ManyToOne
    @JoinColumn(nullable = false)
    private RUser RUser;

    public RUserFcmToken(RUser RUser, String fcmToken, String deviceId) {
        super(fcmToken, deviceId);
        this.RUser = RUser;
    }
  
    public RUser getRUser() { return RUser; }

}