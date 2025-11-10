package com.application.common.reservation.web.dto.parsing;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for reservation text parsing input
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationParseInput {
    
    @NotNull(message = "Text to parse is required")
    private String text;
    
    @NotNull(message = "Restaurant ID is required")
    private UUID restaurantId;
    
    private LocalDate defaultDate; // Default date if not specified in text
    
    private String context; // Additional context (e.g., "phone call", "email", "whatsapp")
    
    private String language; // Language hint for parsing (default: "it")

    /**
     * Check if input is valid for parsing
     */
    public boolean isValid() {
        return text != null && !text.trim().isEmpty() && restaurantId != null;
    }

    /**
     * Get text length
     */
    public int getTextLength() {
        return text != null ? text.length() : 0;
    }

    /**
     * Get cleaned text for parsing
     */
    public String getCleanedText() {
        if (text == null) return "";
        return text.trim().replaceAll("\\s+", " ");
    }

    /**
     * Check if text is likely to contain multiple reservations
     */
    public boolean isMultipleReservations() {
        if (text == null) return false;
        
        String lower = text.toLowerCase();
        return lower.contains(";") || 
               lower.contains("e poi") || 
               lower.contains("altra prenotazione") ||
               lower.contains("secondo tavolo") ||
               lower.contains("\n");
    }

    /**
     * Get default language
     */
    public String getLanguageOrDefault() {
        return language != null ? language : "it";
    }
}