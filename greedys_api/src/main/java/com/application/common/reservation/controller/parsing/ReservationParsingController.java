package com.application.common.reservation.controller.parsing;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.reservation.service.ReservationTextParserService;
import com.application.common.reservation.web.dto.parsing.ParsedReservationDTO;
import com.application.common.reservation.web.dto.parsing.ReservationParseInput;
import com.application.common.reservation.web.dto.parsing.ReservationParseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for reservation text parsing operations
 */
@RestController
@RequestMapping("/restaurant/reservations")
@Tag(name = "Reservation Text Parsing", description = "Endpoints for intelligent parsing of reservation text into structured data")
@Slf4j
public class ReservationParsingController {

    @Autowired
    private ReservationTextParserService parsingService;

    /**
     * Parse free text into structured reservation data
     * 
     * @param input Text parsing input with restaurant context
     * @return Response with parsed reservation candidates
     */
    @PostMapping("/parse-text")
    @Operation(summary = "Parse reservation text", 
               description = "Intelligently parse free text (phone calls, emails, WhatsApp) into structured reservation data")
    @ApiResponse(responseCode = "200", description = "Text parsed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<ReservationParseResponse> parseReservationText(
            @Valid @RequestBody ReservationParseInput input) {
        
        log.info("Reservation text parsing request for restaurant: {} with text length: {}", 
                input.getRestaurantId(), input.getTextLength());

        try {
            // Validate input
            if (!input.isValid()) {
                return ResponseEntity.badRequest()
                    .body(ReservationParseResponse.builder()
                        .originalText(input.getText())
                        .totalReservations(0)
                        .overallConfidence(0.0)
                        .error("Invalid input: text and restaurant ID are required")
                        .build());
            }

            // Set default date if not provided
            if (input.getDefaultDate() == null) {
                input.setDefaultDate(LocalDate.now());
            }

            ReservationParseResponse response = parsingService.parseText(input);
            
            log.info("Text parsing completed for restaurant: {} - Found {} reservations with confidence {}", 
                    input.getRestaurantId(), response.getTotalReservations(), response.getOverallConfidence());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing reservation text parsing request", e);
            return ResponseEntity.status(500)
                .body(ReservationParseResponse.builder()
                    .originalText(input.getText())
                    .totalReservations(0)
                    .overallConfidence(0.0)
                    .error("Internal error during parsing: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Quick parse for simple reservation text (e.g., from phone calls)
     * 
     * @param text Free text to parse
     * @param restaurantId Restaurant UUID for context
     * @param defaultDate Default date if not specified in text
     * @param context Additional context (phone, email, whatsapp)
     * @return Response with parsed reservation candidates
     */
    @PostMapping("/quick-parse")
    @Operation(summary = "Quick parse reservation text", 
               description = "Quick parsing for simple reservation text with URL parameters")
    @ApiResponse(responseCode = "200", description = "Text parsed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid parameters")
    public ResponseEntity<ReservationParseResponse> quickParseReservationText(
            @RequestParam @Parameter(description = "Free text to parse") String text,
            @RequestParam @Parameter(description = "Restaurant UUID") UUID restaurantId,
            @RequestParam(required = false) @Parameter(description = "Default date (YYYY-MM-DD)") LocalDate defaultDate,
            @RequestParam(required = false) @Parameter(description = "Context (phone, email, whatsapp)") String context) {
        
        log.debug("Quick parse request: restaurant={}, text length={}", restaurantId, text.length());

        try {
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ReservationParseResponse.builder()
                        .originalText(text)
                        .totalReservations(0)
                        .overallConfidence(0.0)
                        .error("Text is required")
                        .build());
            }

            if (restaurantId == null) {
                return ResponseEntity.badRequest()
                    .body(ReservationParseResponse.builder()
                        .originalText(text)
                        .totalReservations(0)
                        .overallConfidence(0.0)
                        .error("Restaurant ID is required")
                        .build());
            }

            ReservationParseInput input = ReservationParseInput.builder()
                    .text(text)
                    .restaurantId(restaurantId)
                    .defaultDate(defaultDate != null ? defaultDate : LocalDate.now())
                    .context(context)
                    .language("it")
                    .build();

            ReservationParseResponse response = parsingService.parseText(input);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in quick parse request", e);
            return ResponseEntity.status(500)
                .body(ReservationParseResponse.builder()
                    .originalText(text)
                    .totalReservations(0)
                    .overallConfidence(0.0)
                    .error("Internal error during quick parse: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Validate parsed reservations before creation
     * 
     * @param reservations List of parsed reservations to validate
     * @return Validation results with corrected data
     */
    @PostMapping("/validate-parsed")
    @Operation(summary = "Validate parsed reservations", 
               description = "Validate and enhance parsed reservation data before creation")
    @ApiResponse(responseCode = "200", description = "Validation completed")
    @ApiResponse(responseCode = "400", description = "Invalid reservation data")
    public ResponseEntity<ReservationParseResponse> validateParsedReservations(
            @RequestBody List<ParsedReservationDTO> reservations) {
        
        log.debug("Validating {} parsed reservations", reservations.size());

        try {
            // Create validation response
            ReservationParseResponse.ReservationParseResponseBuilder responseBuilder = 
                ReservationParseResponse.builder()
                    .parsedReservations(reservations)
                    .totalReservations(reservations.size());

            // Validate each reservation
            long validCount = reservations.stream()
                    .filter(ParsedReservationDTO::isValid)
                    .count();

            // Calculate overall confidence
            double avgConfidence = reservations.stream()
                    .mapToDouble(ParsedReservationDTO::getConfidence)
                    .average()
                    .orElse(0.0);

            responseBuilder
                    .overallConfidence(avgConfidence)
                    .processingTime(System.currentTimeMillis());

            // Add validation warnings if needed
            if (validCount < reservations.size()) {
                long invalidCount = reservations.size() - validCount;
                responseBuilder.error(String.format(
                    "Warning: %d of %d reservations have validation issues", 
                    invalidCount, reservations.size()));
            }

            ReservationParseResponse response = responseBuilder.build();
            
            log.info("Validation completed: {}/{} valid reservations, avg confidence: {}", 
                    validCount, reservations.size(), avgConfidence);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error validating parsed reservations", e);
            return ResponseEntity.status(500)
                .body(ReservationParseResponse.builder()
                    .totalReservations(0)
                    .overallConfidence(0.0)
                    .error("Internal error during validation: " + e.getMessage())
                    .build());
        }
    }
}