package com.application.agency.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgencyCreateDTO {
    
    @NotBlank(message = "Agency name is required")
    @Size(max = 100, message = "Agency name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Admin email must be valid")
    private String adminEmail;

    @NotBlank(message = "Admin first name is required")
    @Size(max = 50, message = "Admin first name must not exceed 50 characters")
    private String adminFirstName;

    @NotBlank(message = "Admin last name is required")
    @Size(max = 50, message = "Admin last name must not exceed 50 characters")
    private String adminLastName;

    @Size(max = 20, message = "Admin phone number must not exceed 20 characters")
    private String adminPhoneNumber;

    // Optional fields for agency details
    @Email(message = "Agency email must be valid")
    private String email;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;

    @Size(max = 255, message = "Website must not exceed 255 characters")
    private String website;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    @Size(max = 50, message = "Tax code must not exceed 50 characters")
    private String taxCode;

    @Size(max = 50, message = "VAT number must not exceed 50 characters")
    private String vatNumber;

    @Size(max = 100, message = "License number must not exceed 100 characters")
    private String licenseNumber;

    private String agencyType; // TRAVEL, TOURISM, EVENT, etc.

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}