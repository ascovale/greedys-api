package com.application.customer.persistence.model;

import com.application.common.persistence.model.fcm.AFcmToken;

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
public class CustomerFcmToken extends AFcmToken {

    @ManyToOne
    @JoinColumn(nullable = false)
    private Customer customer;

    public CustomerFcmToken(Customer customer, String fcmToken, String deviceId) {
        super(fcmToken, deviceId);
        this.customer = customer;
    }
}
