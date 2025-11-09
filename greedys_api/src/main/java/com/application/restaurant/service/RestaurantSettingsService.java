package com.application.restaurant.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.application.restaurant.persistence.dao.RestaurantSettingsDAO;
import com.application.restaurant.persistence.model.RestaurantSettings;
import com.application.customer.web.dto.matching.CustomerFormSchemaDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing restaurant-specific settings including customer form schemas
 */
@Service
@Slf4j
public class RestaurantSettingsService {

    @Autowired
    private RestaurantSettingsDAO restaurantSettingsDAO;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Get customer form schema for a restaurant
     * 
     * @param restaurantId Restaurant UUID
     * @return CustomerFormSchemaDTO or null if no schema configured
     */
    public CustomerFormSchemaDTO getCustomerFormSchema(UUID restaurantId) {
        log.debug("Getting customer form schema for restaurant: {}", restaurantId);

        try {
            Optional<RestaurantSettings> settings = restaurantSettingsDAO.findByRestaurantId(restaurantId);
            
            if (settings.isEmpty()) {
                log.debug("No settings found for restaurant {}, returning default schema", restaurantId);
                return getDefaultCustomerFormSchema();
            }

            RestaurantSettings restaurantSettings = settings.get();
            JsonNode schemaNode = restaurantSettings.getCustomerFormSchema();
            
            if (schemaNode == null) {
                log.debug("No customer form schema configured for restaurant {}, returning default", restaurantId);
                return getDefaultCustomerFormSchema();
            }

            // Convert JsonNode to DTO
            return objectMapper.treeToValue(schemaNode, CustomerFormSchemaDTO.class);

        } catch (Exception e) {
            log.error("Error getting customer form schema for restaurant {}", restaurantId, e);
            return getDefaultCustomerFormSchema();
        }
    }

    /**
     * Update customer form schema for a restaurant
     * 
     * @param restaurantId Restaurant UUID
     * @param schema New schema configuration
     * @return Updated RestaurantSettings entity
     */
    public RestaurantSettings updateCustomerFormSchema(UUID restaurantId, CustomerFormSchemaDTO schema) {
        log.info("Updating customer form schema for restaurant: {}", restaurantId);

        try {
            // Convert DTO to JsonNode
            JsonNode schemaNode = objectMapper.valueToTree(schema);

            // Get or create settings
            RestaurantSettings settings = restaurantSettingsDAO.findByRestaurantId(restaurantId)
                    .orElse(RestaurantSettings.builder()
                            .restaurantId(restaurantId)
                            .createdAt(Instant.now())
                            .build());

            settings.setCustomerFormSchema(schemaNode);
            settings.setUpdatedAt(Instant.now());

            return restaurantSettingsDAO.save(settings);

        } catch (Exception e) {
            log.error("Error updating customer form schema for restaurant {}", restaurantId, e);
            throw new RuntimeException("Failed to update customer form schema", e);
        }
    }

    /**
     * Get matching policy for a restaurant
     * 
     * @param restaurantId Restaurant UUID
     * @return JsonNode with matching policy or null if not configured
     */
    public JsonNode getMatchingPolicy(UUID restaurantId) {
        log.debug("Getting matching policy for restaurant: {}", restaurantId);

        try {
            Optional<RestaurantSettings> settings = restaurantSettingsDAO.findByRestaurantId(restaurantId);
            
            if (settings.isEmpty()) {
                log.debug("No settings found for restaurant {}, returning default policy", restaurantId);
                return getDefaultMatchingPolicy();
            }

            RestaurantSettings restaurantSettings = settings.get();
            JsonNode policyNode = restaurantSettings.getMatchingPolicy();
            
            return policyNode != null ? policyNode : getDefaultMatchingPolicy();

        } catch (Exception e) {
            log.error("Error getting matching policy for restaurant {}", restaurantId, e);
            return getDefaultMatchingPolicy();
        }
    }

    /**
     * Update matching policy for a restaurant
     * 
     * @param restaurantId Restaurant UUID
     * @param policy New matching policy configuration
     * @return Updated RestaurantSettings entity
     */
    public RestaurantSettings updateMatchingPolicy(UUID restaurantId, JsonNode policy) {
        log.info("Updating matching policy for restaurant: {}", restaurantId);

        try {
            // Get or create settings
            RestaurantSettings settings = restaurantSettingsDAO.findByRestaurantId(restaurantId)
                    .orElse(RestaurantSettings.builder()
                            .restaurantId(restaurantId)
                            .createdAt(Instant.now())
                            .build());

            settings.setMatchingPolicy(policy);
            settings.setUpdatedAt(Instant.now());

            return restaurantSettingsDAO.save(settings);

        } catch (Exception e) {
            log.error("Error updating matching policy for restaurant {}", restaurantId, e);
            throw new RuntimeException("Failed to update matching policy", e);
        }
    }

    /**
     * Get complete restaurant settings
     * 
     * @param restaurantId Restaurant UUID
     * @return RestaurantSettings entity or null if not found
     */
    public RestaurantSettings getRestaurantSettings(UUID restaurantId) {
        log.debug("Getting restaurant settings for: {}", restaurantId);
        
        return restaurantSettingsDAO.findByRestaurantId(restaurantId)
                .orElse(null);
    }

    /**
     * Create or update complete restaurant settings
     * 
     * @param restaurantId Restaurant UUID
     * @param settings Settings to save
     * @return Updated RestaurantSettings entity
     */
    public RestaurantSettings saveRestaurantSettings(UUID restaurantId, RestaurantSettings settings) {
        log.info("Saving restaurant settings for: {}", restaurantId);

        try {
            settings.setRestaurantId(restaurantId);
            settings.setUpdatedAt(Instant.now());
            
            if (settings.getCreatedAt() == null) {
                settings.setCreatedAt(Instant.now());
            }

            return restaurantSettingsDAO.save(settings);

        } catch (Exception e) {
            log.error("Error saving restaurant settings for {}", restaurantId, e);
            throw new RuntimeException("Failed to save restaurant settings", e);
        }
    }

    /**
     * Delete restaurant settings
     * 
     * @param restaurantId Restaurant UUID
     */
    public void deleteRestaurantSettings(UUID restaurantId) {
        log.info("Deleting restaurant settings for: {}", restaurantId);

        try {
            restaurantSettingsDAO.deleteByRestaurantId(restaurantId);
        } catch (Exception e) {
            log.error("Error deleting restaurant settings for {}", restaurantId, e);
            throw new RuntimeException("Failed to delete restaurant settings", e);
        }
    }

    /**
     * Get default customer form schema
     */
    private CustomerFormSchemaDTO getDefaultCustomerFormSchema() {
        return CustomerFormSchemaDTO.builder()
                .requireFirstName(true)
                .requireLastName(true)
                .requirePhone(true)
                .requireEmail(false)
                .allowNickname(false)
                .phoneFormat("italian")
                .emailValidation(true)
                .build();
    }

    /**
     * Get default matching policy as JsonNode
     */
    private JsonNode getDefaultMatchingPolicy() {
        try {
            String defaultPolicy = """
                {
                    "phoneExactConfidence": 1.0,
                    "emailExactConfidence": 0.95,
                    "namePhonePartialConfidence": 0.85,
                    "fullNameFuzzyConfidence": 0.75,
                    "nameSimilarityThreshold": 0.8,
                    "autoAttachThreshold": 0.95,
                    "confirmThreshold": 0.75,
                    "createNewThreshold": 0.50
                }
                """;
            
            return objectMapper.readTree(defaultPolicy);
        } catch (Exception e) {
            log.error("Error creating default matching policy", e);
            return objectMapper.createObjectNode();
        }
    }
}