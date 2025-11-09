package com.application.agency.web.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class AgencyDTO {
    private Long id;
    private String name;
    private String description;
    private String email;
    private String phoneNumber;
    private String website;
    private String address;
    private String city;
    private String country;
    private String postalCode;
    private String taxCode;
    private String vatNumber;
    private String status;
    private String agencyType;
    private LocalDate createdDate;
    private LocalDate verifiedDate;
    private String licenseNumber;
    private String notes;
    private boolean active;
    private boolean verified;
    private String fullAddress;
}