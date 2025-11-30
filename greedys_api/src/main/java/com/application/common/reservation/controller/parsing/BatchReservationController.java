package com.application.common.reservation.controller.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.reservation.web.dto.parsing.BatchReservationCreateInput;
import com.application.common.reservation.web.dto.parsing.BatchReservationCreateResponse;
import com.application.common.reservation.web.dto.parsing.ParsedReservationDTO;
import com.application.common.reservation.web.dto.parsing.ReservationCreationResult;
import com.application.customer.service.CustomerMatchService;
import com.application.customer.web.dto.matching.CustomerMatchInput;
import com.application.customer.web.dto.matching.CustomerMatchResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for batch reservation creation from parsed data
 */
@RestController
@RequestMapping("/restaurant/reservations")
@Tag(name = "Batch Reservation Creation", description = "Endpoints for creating multiple reservations from parsed text data")
@Slf4j
public class BatchReservationController {

    @Autowired
    private CustomerMatchService customerMatchService;

    // Note: ReservationService would be injected here for actual reservation creation
    // @Autowired
    // private ReservationService reservationService;

    /**
     * Create multiple reservations from parsed text data
     * 
     * @param input Batch reservation creation input
     * @return Response with creation results for each reservation
     */
    @PostMapping("/batch-create")
    @Operation(summary = "Create reservations from parsed data", 
               description = "Create multiple reservations from parsed text data with customer matching")
    @ApiResponse(responseCode = "200", description = "Batch creation completed")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<BatchReservationCreateResponse> createReservationsFromParsedData(
            @Valid @RequestBody BatchReservationCreateInput input) {
        
        log.info("Batch reservation creation request: {} reservations for restaurant {}", 
                input.getParsedReservations().size(), input.getRestaurantId());

        try {
            List<ReservationCreationResult> results = new ArrayList<>();
            
            for (ParsedReservationDTO parsedReservation : input.getParsedReservations()) {
                ReservationCreationResult result = processSingleReservation(parsedReservation, input);
                results.add(result);
            }

            // Calculate success statistics
            long successCount = results.stream()
                    .filter(ReservationCreationResult::isSuccessful)
                    .count();
            long errorCount = results.size() - successCount;

            BatchReservationCreateResponse response = BatchReservationCreateResponse.builder()
                    .restaurantId(input.getRestaurantId())
                    .results(results)
                    .totalProcessed(results.size())
                    .successCount((int) successCount)
                    .errorCount((int) errorCount)
                    .processingTime(System.currentTimeMillis())
                    .build();

            log.info("Batch creation completed: {}/{} successful, {} errors", 
                    successCount, results.size(), errorCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing batch reservation creation", e);
            return ResponseEntity.status(500)
                .body(BatchReservationCreateResponse.builder()
                    .restaurantId(input.getRestaurantId())
                    .results(new ArrayList<>())
                    .totalProcessed(0)
                    .successCount(0)
                    .errorCount(input.getParsedReservations().size())
                    .error("Internal error during batch creation: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Process a single reservation from parsed data
     */
    private ReservationCreationResult processSingleReservation(ParsedReservationDTO parsedReservation, 
                                                               BatchReservationCreateInput input) {
        
        ReservationCreationResult.ReservationCreationResultBuilder resultBuilder = 
            ReservationCreationResult.builder()
                .parsedReservation(parsedReservation)
                .processed(true);

        try {
            // Validate parsed reservation
            if (!parsedReservation.isValid()) {
                return resultBuilder
                        .successful(false)
                        .error("Invalid parsed reservation data: " + String.join(", ", parsedReservation.getMissingFields()))
                        .build();
            }

            // Step 1: Customer matching
            CustomerMatchResponse matchResponse = performCustomerMatching(parsedReservation);
            resultBuilder.customerMatchResponse(matchResponse);

            // Step 2: Handle customer based on match decision
            Long customerId = handleCustomerMatching(matchResponse, parsedReservation, input);
            if (customerId == null) {
                return resultBuilder
                        .successful(false)
                        .error("Failed to resolve customer: " + matchResponse.getDecision().getReason())
                        .build();
            }

            // Step 3: Create reservation
            // TODO: Implement actual reservation creation using ReservationService
            Long reservationId = createReservation(parsedReservation, customerId, input);
            
            if (reservationId != null) {
                return resultBuilder
                        .successful(true)
                        .reservationId(reservationId)
                        .customerId(customerId)
                        .message("Reservation created successfully")
                        .build();
            } else {
                return resultBuilder
                        .successful(false)
                        .error("Failed to create reservation")
                        .build();
            }

        } catch (Exception e) {
            log.error("Error processing single reservation", e);
            return resultBuilder
                    .successful(false)
                    .error("Processing error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Perform customer matching for the parsed reservation
     */
    private CustomerMatchResponse performCustomerMatching(ParsedReservationDTO parsedReservation) {
        // Build customer match input from parsed data
        CustomerMatchInput matchInput = CustomerMatchInput.builder()
                .firstName(extractFirstName(parsedReservation.getCustomerName()))
                .lastName(extractLastName(parsedReservation.getCustomerName()))
                .phone(parsedReservation.getPhoneNumber())
                .email(parsedReservation.getEmail())
                .restaurantId(parsedReservation.getRestaurantId())
                .build();

        return customerMatchService.findMatches(matchInput);
    }

    /**
     * Handle customer matching decision
     */
    private Long handleCustomerMatching(CustomerMatchResponse matchResponse, 
                                       ParsedReservationDTO parsedReservation, 
                                       BatchReservationCreateInput input) {
        
        switch (matchResponse.getDecision().getType()) {
            case AUTO_ATTACH:
                // Use existing customer
                if (matchResponse.getBestCandidate() != null) {
                    return convertUUIDToLong(matchResponse.getBestCandidate().getId());
                }
                break;
                
            case CONFIRM:
                // In batch mode, use auto-decision based on input configuration
                if (input.isAutoConfirmMatches() && matchResponse.getBestCandidate() != null) {
                    return convertUUIDToLong(matchResponse.getBestCandidate().getId());
                } else {
                    // Create new customer if auto-confirm is disabled
                    return createNewCustomer(parsedReservation);
                }
                
            case CREATE_NEW:
                // Create new customer
                return createNewCustomer(parsedReservation);
        }
        
        return null;
    }

    /**
     * Create a new customer from parsed reservation data
     */
    private Long createNewCustomer(ParsedReservationDTO parsedReservation) {
        // TODO: Implement actual customer creation using CustomerService
        // For now, return a mock ID
        log.info("Would create new customer: {}", parsedReservation.getCustomerName());
        return System.currentTimeMillis() % 10000L; // Mock customer ID
    }

    /**
     * Create reservation
     */
    private Long createReservation(ParsedReservationDTO parsedReservation, Long customerId, 
                                  BatchReservationCreateInput input) {
        // TODO: Implement actual reservation creation using ReservationService
        log.info("Would create reservation for customer {} at {}", 
                customerId, parsedReservation.getReservationDateTime());
        
        // Mock reservation creation
        return System.currentTimeMillis() % 100000L; // Mock reservation ID
    }

    /**
     * Extract first name from full name
     */
    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return null;
        
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : null;
    }

    /**
     * Extract last name from full name
     */
    private String extractLastName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return null;
        
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length)) : null;
    }

    /**
     * Convert UUID to Long (temporary utility)
     */
    private Long convertUUIDToLong(UUID uuid) {
        if (uuid == null) return null;
        return uuid.getMostSignificantBits() & Long.MAX_VALUE;
    }
}