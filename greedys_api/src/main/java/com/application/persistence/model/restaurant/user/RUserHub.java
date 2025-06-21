package com.application.persistence.model.restaurant.user;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "restaurant_user_hub")
public class RUserHub {
    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY)
    private RUserOptions options;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.ENABLED;
    private String phoneNumber;
    @Column(length = 60)
    private String password;
    @Column(unique = true, nullable = false)
    private String email;
    private boolean accepted;
    private String firstName;
    private String lastName;

    @Column(name = "credentials_expiration_date")
    private LocalDate credentialsExpirationDate;

    public enum Status {
        BLOCKED,
        DELETED,
        ENABLED,
        DISABLED
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    private Integer toReadNotification = 0;

    public Integer getToReadNotification() {
        return toReadNotification;
    }

    public void setToReadNotification(Integer toReadNotification) {
        this.toReadNotification = toReadNotification;
    }

    public Long getId() {
        return id;
    }

    public RUserOptions getUserOptions() {
        return options;
    }

    public void setUserOptions(RUserOptions options) {
        this.options = options;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return email;
    }

    
    public boolean isAccountNonExpired() {
        return status == Status.ENABLED;
    }


}
