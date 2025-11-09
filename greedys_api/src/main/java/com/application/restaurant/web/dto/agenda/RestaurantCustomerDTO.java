package com.application.restaurant.web.dto.agenda;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.application.customer.persistence.model.Customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for restaurant customer agenda entry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantCustomerDTO {
    
    private Long customerId;
    private UUID restaurantId;
    private String firstName;
    private String lastName;
    private String displayName; // Nome completo per display
    private String phoneNumber;
    private String email;
    private String notes; // Note specifiche del ristorante per questo cliente
    private boolean hasAccount; // Se il cliente ha un account registrato
    private LocalDate dateOfBirth;
    private String address;
    private String allergies;
    private String preferences; // Preferenze culinarie
    private int totalReservations; // Numero totale prenotazioni
    private LocalDateTime lastReservation; // Data ultima prenotazione
    private LocalDateTime firstAdded; // Data prima aggiunta in agenda
    private LocalDateTime lastModified; // Data ultima modifica
    private String addedBy; // Chi ha aggiunto il cliente in agenda
    private Customer.Status status; // Status del cliente (REGISTERED, UNREGISTERED, etc.)

    /**
     * Create DTO from Customer entity
     */
    public static RestaurantCustomerDTO fromCustomer(Customer customer, UUID restaurantId) {
        return RestaurantCustomerDTO.builder()
                .customerId(customer.getId())
                .restaurantId(restaurantId)
                .firstName(customer.getName())
                .lastName(customer.getSurname())
                .displayName(buildDisplayName(customer.getName(), customer.getSurname()))
                .phoneNumber(customer.getPhoneNumber())
                .email(customer.getEmail())
                .hasAccount(customer.hasPassword())
                .status(customer.getStatus())
                .build();
    }

    /**
     * Build display name from first and last name
     */
    private static String buildDisplayName(String firstName, String lastName) {
        StringBuilder name = new StringBuilder();
        if (firstName != null && !firstName.trim().isEmpty()) {
            name.append(firstName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(lastName.trim());
        }
        return name.length() > 0 ? name.toString() : null;
    }

    /**
     * Get full display name with fallback
     */
    public String getFullDisplayName() {
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        }
        
        return buildDisplayName(firstName, lastName);
    }

    /**
     * Check if customer has contact information
     */
    public boolean hasContactInfo() {
        return (phoneNumber != null && !phoneNumber.trim().isEmpty()) ||
               (email != null && !email.trim().isEmpty());
    }

    /**
     * Check if customer is a regular (multiple reservations)
     */
    public boolean isRegularCustomer() {
        return totalReservations >= 3;
    }

    /**
     * Get customer type description
     */
    public String getCustomerType() {
        if (hasAccount) {
            return "Registered";
        } else if (isRegularCustomer()) {
            return "Regular Contact";
        } else {
            return "Contact";
        }
    }

    /**
     * Get short contact info for display
     */
    public String getContactSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            summary.append("Tel: ").append(phoneNumber);
        }
        
        if (email != null && !email.trim().isEmpty()) {
            if (summary.length() > 0) {
                summary.append(" | ");
            }
            summary.append("Email: ").append(email);
        }
        
        return summary.length() > 0 ? summary.toString() : "No contact info";
    }

    /**
     * Check if customer has complete profile
     */
    public boolean hasCompleteProfile() {
        return firstName != null && !firstName.trim().isEmpty() &&
               lastName != null && !lastName.trim().isEmpty() &&
               hasContactInfo();
    }

    /**
     * Get completeness score (0-100)
     */
    public int getCompletenessScore() {
        int score = 0;
        
        if (firstName != null && !firstName.trim().isEmpty()) score += 20;
        if (lastName != null && !lastName.trim().isEmpty()) score += 20;
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) score += 25;
        if (email != null && !email.trim().isEmpty()) score += 20;
        if (notes != null && !notes.trim().isEmpty()) score += 5;
        if (preferences != null && !preferences.trim().isEmpty()) score += 5;
        if (allergies != null && !allergies.trim().isEmpty()) score += 5;
        
        return Math.min(score, 100);
    }
}