package com.application.persistence.model.admin;


import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPasswordResetToken {

    private static final int EXPIRATION = 60 * 24;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String token;
    @OneToOne(targetEntity = Admin.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private Admin admin;
    
    @Builder.Default
    private LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(EXPIRATION);

    // Costruttori personalizzati per mantenere la logica esistente
    public AdminPasswordResetToken(final String token) {
        this.token = token;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

    public AdminPasswordResetToken(final String token, final Admin admin) {
        this.token = token;
        this.admin = admin;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

    private LocalDateTime calculateExpiryDate(final int expiryTimeInMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDateTime = now.plusMinutes(expiryTimeInMinutes);
        return expiryDateTime;
    }


    public void updateToken(final String token) {
        this.token = token;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

    public static int getExpiration() {
        return EXPIRATION;
    }

}
