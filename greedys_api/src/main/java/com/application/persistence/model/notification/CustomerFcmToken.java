package com.application.persistence.model.notification;

import com.application.persistence.model.customer.Customer;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class CustomerFcmToken extends FcmToken {

    @ManyToOne
    @JoinColumn(nullable = false)
    private Customer customer;

    public CustomerFcmToken(Customer customer, String fcmToken, String deviceId) {
        super(fcmToken, deviceId);
        this.customer = customer;
    }
    public Customer getCustomer() { return customer; }
}