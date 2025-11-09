package com.application.restaurant.controller.agenda;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.restaurant.service.agenda.RestaurantAgendaService;
import com.application.restaurant.web.dto.agenda.CustomerContactCreateDTO;
import com.application.restaurant.web.dto.agenda.RestaurantAgendaResponse;
import com.application.restaurant.web.dto.agenda.RestaurantCustomerDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for restaurant customer agenda management
 */
@Tag(name = "Restaurant Customer Agenda", description = "Endpoints for managing restaurant customer contacts and agenda")
@RestController
@RequestMapping("/restaurant/agenda")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class RestaurantCustomerAgendaController extends BaseController {

    @Autowired
    private RestaurantAgendaService restaurantAgendaService;

    /**
     * Get restaurant customer agenda with all contacts
     */
    @Operation(summary = "Get restaurant customer agenda", 
               description = "Retrieve all customer contacts in restaurant agenda including registered and unregistered customers")
    @ReadApiResponses
    @GetMapping("/{restaurantId}/customers")
    public ResponseEntity<RestaurantAgendaResponse> getRestaurantAgenda(
            @Parameter(description = "Restaurant ID", required = true)
            @PathVariable Long restaurantId) {
        
        return execute("get restaurant agenda", () -> 
            restaurantAgendaService.getRestaurantAgenda(restaurantId)
        );
    }

    /**
     * Search customers in restaurant agenda
     */
    @Operation(summary = "Search customers in agenda", 
               description = "Search customers by name, email, or phone number in restaurant agenda")
    @ReadApiResponses
    @GetMapping("/{restaurantId}/customers/search")
    public ResponseEntity<RestaurantAgendaResponse> searchCustomers(
            @Parameter(description = "Restaurant ID", required = true)
            @PathVariable Long restaurantId,
            
            @Parameter(description = "Search query (name, email, phone)")
            @RequestParam String query,
            
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        return execute("search customers in agenda", () ->
            restaurantAgendaService.searchCustomers(restaurantId, query, pageable)
        );
    }

    /**
     * Force create new customer contact for restaurant
     */
    @Operation(summary = "Force create customer contact", 
               description = "Create new customer contact in restaurant agenda, optionally ignoring potential duplicates")
    @PostMapping("/{restaurantId}/force-create-customer")
    public ResponseEntity<RestaurantCustomerDTO> forceCreateCustomerContact(
            @Parameter(description = "Restaurant ID", required = true)
            @PathVariable Long restaurantId,
            
            @Parameter(description = "Customer contact creation data", required = true)
            @Valid @RequestBody CustomerContactCreateDTO createDTO) {
        
        // Ensure restaurant ID matches the path parameter
        createDTO.setRestaurantId(java.util.UUID.fromString(restaurantId.toString()));
        
        return execute("force create customer contact", () ->
            restaurantAgendaService.forceCreateCustomerContact(restaurantId, createDTO)
        );
    }

    /**
     * Create or update customer contact
     */
    @Operation(summary = "Create customer contact", 
               description = "Create new customer contact with duplicate checking")
    @PostMapping("/{restaurantId}/customer")
    public ResponseEntity<RestaurantCustomerDTO> createCustomerContact(
            @Parameter(description = "Restaurant ID", required = true)
            @PathVariable Long restaurantId,
            
            @Parameter(description = "Customer contact creation data", required = true)
            @Valid @RequestBody CustomerContactCreateDTO createDTO) {
        
        // Ensure restaurant ID matches and force create is false for normal creation
        createDTO.setRestaurantId(java.util.UUID.fromString(restaurantId.toString()));
        createDTO.setForceCreate(false);
        
        return execute("create customer contact", () ->
            restaurantAgendaService.forceCreateCustomerContact(restaurantId, createDTO)
        );
    }

    /**
     * Update customer contact information
     */
    @Operation(summary = "Update customer contact", 
               description = "Update existing customer contact information")
    @PutMapping("/customer/{customerId}")
    public ResponseEntity<RestaurantCustomerDTO> updateCustomerContact(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable Long customerId,
            
            @Parameter(description = "Customer contact update data", required = true)
            @Valid @RequestBody CustomerContactCreateDTO updateDTO) {
        
        return execute("update customer contact", () ->
            restaurantAgendaService.updateCustomerContact(customerId, updateDTO)
        );
    }

    /**
     * Delete customer contact
     */
    @Operation(summary = "Delete customer contact", 
               description = "Remove customer from restaurant agenda (only if no active reservations)")
    @DeleteMapping("/customer/{customerId}")
    public ResponseEntity<Void> deleteCustomerContact(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable Long customerId) {
        
        return execute("delete customer contact", () -> {
            restaurantAgendaService.deleteCustomerContact(customerId);
            return null;
        });
    }

    /**
     * Get specific customer contact details
     */
    @Operation(summary = "Get customer contact details", 
               description = "Retrieve detailed information about a specific customer contact")
    @ReadApiResponses
    @GetMapping("/{restaurantId}/customer/{customerId}")
    public ResponseEntity<RestaurantCustomerDTO> getCustomerContact(
            @Parameter(description = "Restaurant ID", required = true)
            @PathVariable Long restaurantId,
            
            @Parameter(description = "Customer ID", required = true)
            @PathVariable Long customerId) {
        
        return execute("get customer contact", () -> {
            Optional<RestaurantCustomerDTO> customer = 
                restaurantAgendaService.getCustomerContact(customerId, restaurantId);
            
            if (customer.isPresent()) {
                return customer.get();
            } else {
                throw new IllegalArgumentException("Customer not found in restaurant agenda");
            }
        });
    }
}