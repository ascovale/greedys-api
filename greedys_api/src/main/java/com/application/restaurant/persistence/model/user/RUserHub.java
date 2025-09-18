package com.application.restaurant.persistence.model.user;

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

@Entity
@Table(name = "restaurant_user_hub")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RUserHub {
    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY)
    private RUserOptions options;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
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

    public String getUsername() {
        return email;
    }

    
    public boolean isAccountNonExpired() {
        return status == Status.ENABLED;
    }


}
