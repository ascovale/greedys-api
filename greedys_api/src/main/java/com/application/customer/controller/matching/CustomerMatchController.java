package com.application.customer.controller.matching;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.customer.service.CustomerMatchService;
import com.application.customer.web.dto.matching.CustomerFormSchemaDTO;
import com.application.customer.web.dto.matching.CustomerMatchInput;
import com.application.customer.web.dto.matching.CustomerMatchResponse;
import com.application.restaurant.service.RestaurantSettingsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for customer matching operations
 */
@RestController
@RequestMapping("/api/customer/match")
@Tag(name = "Customer Matching", description = "Endpoints for intelligent customer matching and form configuration")
@Slf4j
public class CustomerMatchController {

    @Autowired
    private CustomerMatchService customerMatchService;

    @Autowired
    private RestaurantSettingsService restaurantSettingsService;

    /**
     * Find matching customers for given input data
     * 
     * @param input Customer matching input data
     * @return Response with matching candidates and decision
     */
    @PostMapping("/find")
    @Operation(summary = "Find matching customers", 
               description = "Intelligently find matching customers based on phone, email, and name data")
    @ApiResponse(responseCode = "200", description = "Matching completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<CustomerMatchResponse> findMatches(
            @Valid @RequestBody CustomerMatchInput input) {
        
        log.info("Customer matching request for restaurant: {} with input: phone={}, email={}, name='{}'", 
                input.getRestaurantId(), input.getPhone(), input.getEmail(), input.getComputedFullName());

        try {
            // Validate input
            if (!input.isValid()) {
                return ResponseEntity.badRequest()
                    .body(CustomerMatchResponse.builder()
                        .decision(com.application.customer.web.dto.matching.CustomerMatchDecisionDTO
                            .createNew("Invalid input: missing restaurant ID or all contact information"))
                        .build());
            }

            CustomerMatchResponse response = customerMatchService.findMatches(input);
            
            log.info("Customer matching completed for restaurant: {} - Decision: {}, Candidates: {}", 
                    input.getRestaurantId(), 
                    response.getDecision() != null ? response.getDecision().getTypeString() : "null",
                    response.getCandidateCount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing customer match request", e);
            return ResponseEntity.status(500)
                .body(CustomerMatchResponse.builder()
                    .decision(com.application.customer.web.dto.matching.CustomerMatchDecisionDTO
                        .createNew("Internal error during matching: " + e.getMessage()))
                    .build());
        }
    }

    /**
     * Get customer form schema configuration for a restaurant
     * 
     * @param restaurantId Restaurant UUID
     * @return Customer form schema configuration
     */
    @GetMapping("/form-schema/{restaurantId}")
    @Operation(summary = "Get customer form schema", 
               description = "Get the configurable customer form schema for a specific restaurant")
    @ApiResponse(responseCode = "200", description = "Schema retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Restaurant not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<CustomerFormSchemaDTO> getCustomerFormSchema(
            @PathVariable @Parameter(description = "Restaurant UUID") UUID restaurantId) {
        
        log.debug("Getting customer form schema for restaurant: {}", restaurantId);

        try {
            CustomerFormSchemaDTO schema = restaurantSettingsService.getCustomerFormSchema(restaurantId);
            
            if (schema == null) {
                log.warn("No form schema found for restaurant: {}", restaurantId);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(schema);

        } catch (Exception e) {
            log.error("Error getting customer form schema for restaurant: {}", restaurantId, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Update customer form schema configuration for a restaurant
     * 
     * @param restaurantId Restaurant UUID
     * @param schema New form schema configuration
     * @return Updated form schema configuration
     */
    @PostMapping("/form-schema/{restaurantId}")
    @Operation(summary = "Update customer form schema", 
               description = "Update the configurable customer form schema for a specific restaurant")
    @ApiResponse(responseCode = "200", description = "Schema updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid schema data")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<CustomerFormSchemaDTO> updateCustomerFormSchema(
            @PathVariable @Parameter(description = "Restaurant UUID") UUID restaurantId,
            @Valid @RequestBody CustomerFormSchemaDTO schema) {
        
        log.info("Updating customer form schema for restaurant: {}", restaurantId);

        try {
            restaurantSettingsService.updateCustomerFormSchema(restaurantId, schema);
            
            // Return the updated schema
            CustomerFormSchemaDTO updatedSchema = restaurantSettingsService.getCustomerFormSchema(restaurantId);
            
            return ResponseEntity.ok(updatedSchema);

        } catch (Exception e) {
            log.error("Error updating customer form schema for restaurant: {}", restaurantId, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Find customer by phone number for quick lookup
     * 
     * @param phoneNumber Phone number to search for
     * @param restaurantId Restaurant context for form schema
     * @return Customer match response with single candidate if found
     */
    @GetMapping("/by-phone")
    @Operation(summary = "Find customer by phone", 
               description = "Quick customer lookup by phone number")
    @ApiResponse(responseCode = "200", description = "Search completed")
    @ApiResponse(responseCode = "400", description = "Invalid phone number")
    public ResponseEntity<CustomerMatchResponse> findByPhone(
            @RequestParam @Parameter(description = "Phone number to search for") String phoneNumber,
            @RequestParam @Parameter(description = "Restaurant UUID for context") UUID restaurantId) {
        
        log.debug("Phone lookup request: {} for restaurant: {}", phoneNumber, restaurantId);

        try {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(CustomerMatchResponse.builder()
                        .decision(com.application.customer.web.dto.matching.CustomerMatchDecisionDTO
                            .createNew("Phone number is required"))
                        .build());
            }

            CustomerMatchInput input = CustomerMatchInput.builder()
                    .phone(phoneNumber.trim())
                    .restaurantId(restaurantId)
                    .build();

            CustomerMatchResponse response = customerMatchService.findMatches(input);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in phone lookup for: {}", phoneNumber, e);
            return ResponseEntity.status(500)
                .body(CustomerMatchResponse.builder()
                    .decision(com.application.customer.web.dto.matching.CustomerMatchDecisionDTO
                        .createNew("Internal error during phone lookup: " + e.getMessage()))
                    .build());
        }
    }
}