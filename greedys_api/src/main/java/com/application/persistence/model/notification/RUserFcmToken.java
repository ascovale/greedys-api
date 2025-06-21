package com.application.persistence.model.notification;

import com.application.persistence.model.restaurant.user.RUser;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class RUserFcmToken extends FcmToken {

    @ManyToOne
    @JoinColumn(nullable = false)
    private RUser RUser;

    public RUserFcmToken(RUser RUser, String fcmToken, String deviceId) {
        super(fcmToken, deviceId);
        this.RUser = RUser;
    }
  
    public RUser getRUser() { return RUser; }

}