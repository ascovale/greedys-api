package com.application.restaurant.web.dto.agenda;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for restaurant customer agenda response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantAgendaResponse {
    
    private Long restaurantId;
    private List<RestaurantCustomerDTO> customers;
    private int totalCustomers;
    private int registeredCustomers; // Con account
    private int contactsOnly; // Solo contatti senza account
    private AgendaStats stats;

    /**
     * Get count of customers with accounts
     */
    public long getRegisteredCount() {
        if (customers == null) return 0;
        return customers.stream()
                .filter(RestaurantCustomerDTO::isHasAccount)
                .count();
    }

    /**
     * Get count of contact-only customers
     */
    public long getContactOnlyCount() {
        if (customers == null) return 0;
        return customers.stream()
                .filter(customer -> !customer.isHasAccount())
                .count();
    }

    /**
     * Get count of regular customers (3+ reservations)
     */
    public long getRegularCustomersCount() {
        if (customers == null) return 0;
        return customers.stream()
                .filter(RestaurantCustomerDTO::isRegularCustomer)
                .count();
    }

    /**
     * Get customers with complete profiles
     */
    public long getCompleteProfilesCount() {
        if (customers == null) return 0;
        return customers.stream()
                .filter(RestaurantCustomerDTO::hasCompleteProfile)
                .count();
    }

    /**
     * Get average completeness score
     */
    public double getAverageCompletenessScore() {
        if (customers == null || customers.isEmpty()) return 0.0;
        return customers.stream()
                .mapToInt(RestaurantCustomerDTO::getCompletenessScore)
                .average()
                .orElse(0.0);
    }

    /**
     * Statistics about the restaurant agenda
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgendaStats {
        private int totalContacts;
        private int withPhone;
        private int withEmail;
        private int withBothContacts;
        private int withNotes;
        private int withPreferences;
        private int withAllergies;
        private double avgReservationsPerCustomer;
        private String mostActiveCustomer;
        private String newestCustomer;
    }
}