package com.application.restaurant.web.dto.customer;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RestaurantCustomerContactDTO", description = "DTO for displaying customer contacts in restaurant address book")
public class RestaurantCustomerContactDTO {

    @Schema(description = "Customer ID", example = "123")
    private Long id;

    @Schema(description = "Customer full name", example = "Mario Rossi")
    private String fullName;

    @Schema(description = "Customer first name", example = "Mario")
    private String firstName;

    @Schema(description = "Customer last name", example = "Rossi")
    private String lastName;

    @Schema(description = "Customer email", example = "mario.rossi@email.com")
    private String email;

    @Schema(description = "Customer phone number", example = "+39 333 1234567")
    private String phoneNumber;

    @Schema(description = "Customer registration status")
    private CustomerRegistrationStatus status;

    @Schema(description = "Total number of reservations made by this customer")
    private Long reservationCount;

    @Schema(description = "Date of last reservation")
    private LocalDate lastReservationDate;

    @Schema(description = "Customer date of birth")
    private LocalDate dateOfBirth;

    public enum CustomerRegistrationStatus {
        @Schema(description = "Customer has made reservations but hasn't registered online")
        UNREGISTERED,
        
        @Schema(description = "Customer is registered and can access online features")
        REGISTERED,
        
        @Schema(description = "Customer account is verified and active")
        VERIFIED,
        
        @Schema(description = "Customer account is blocked")
        BLOCKED
    }

    /**
     * Get customer's full name for display
     */
    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName;
        }
        if (firstName != null && lastName != null) {
            return (firstName + " " + lastName).trim();
        }
        if (firstName != null) {
            return firstName;
        }
        return "Unknown";
    }
}