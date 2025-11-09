package com.application.restaurant.persistence.model.user;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "restaurant_user_hub_verification_token")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RUserHubVerificationToken {

    private static final int EXPIRATION = 60 * 24; // 24 hours

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    private String token;
    
    @OneToOne(targetEntity = RUserHub.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_hub_id", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "FK_VERIFY_RUserHub"))
    private RUserHub rUserHub;
    
    @Builder.Default
    private LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(EXPIRATION);

    // Costruttori personalizzati per mantenere la logica esistente
    public RUserHubVerificationToken(final String token) {
        this.token = token;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

    public RUserHubVerificationToken(final String token, final RUserHub rUserHub) {
        this.token = token;
        this.rUserHub = rUserHub;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

    private LocalDateTime calculateExpiryDate(final int expiryTimeInMinutes) {
        LocalDateTime now = LocalDateTime.now();
        return now.plusMinutes(expiryTimeInMinutes);
    }

    public void updateToken(final String token) {
        this.token = token;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

    public static int getExpiration() {
        return EXPIRATION;
    }
}