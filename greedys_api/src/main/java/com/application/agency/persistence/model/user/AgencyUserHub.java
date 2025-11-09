package com.application.agency.persistence.model.user;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * AgencyUserHub represents the central hub for agency users who can work for multiple agencies.
 * Similar to RestaurantUserHub but for agency operations.
 */
@Entity
@Table(name = "agency_user_hub")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AgencyUserHub {
    
    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    private AgencyUserOptions options;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private Status status = Status.VERIFY_TOKEN;
    
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
        VERIFY_TOKEN,
        BLOCKED,
        DELETED,
        ENABLED,
        DISABLED
    }

    public String getUsername() {
        return email;
    }

    public boolean isAccountNonExpired() {
        return status == Status.ENABLED || status == Status.VERIFY_TOKEN;
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public boolean isEnabled() {
        return status == Status.ENABLED;
    }

    public boolean isAccountNonLocked() {
        return status != Status.BLOCKED && status != Status.DELETED;
    }

    public boolean isCredentialsNonExpired() {
        return credentialsExpirationDate == null || credentialsExpirationDate.isAfter(LocalDate.now());
    }
}