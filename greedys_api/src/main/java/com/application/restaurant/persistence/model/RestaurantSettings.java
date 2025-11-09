package com.application.restaurant.persistence.model;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RestaurantSettings stores configuration data for each restaurant,
 * including customer form schema and matching policies.
 */
@Entity
@Table(name = "restaurant_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @ManyToOne
    @JoinColumn(name = "restaurant_entity_id")
    private Restaurant restaurant;

    @Column(name = "customer_form_schema", columnDefinition = "JSON")
    private JsonNode customerFormSchema;

    @Column(name = "matching_policy", columnDefinition = "JSON")
    private JsonNode matchingPolicy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    /**
     * Update the updated_at timestamp
     */
    public void touch() {
        this.updatedAt = Instant.now();
    }

    /**
     * Check if restaurant has custom form schema
     */
    public boolean hasCustomSchema() {
        return customerFormSchema != null && !customerFormSchema.isNull();
    }

    /**
     * Check if restaurant has custom matching policy
     */
    public boolean hasCustomMatchingPolicy() {
        return matchingPolicy != null && !matchingPolicy.isNull();
    }
}