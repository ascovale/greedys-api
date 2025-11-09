package com.application.agency.web.dto;

import com.application.agency.persistence.model.Agency;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgencyUpdateDTO {
    
    @Size(max = 100, message = "Agency name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Agency.Status status;

    @Size(max = 255, message = "Email must not exceed 255 characters")
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

    private Agency.AgencyType agencyType;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}