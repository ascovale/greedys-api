package com.application.agency.persistence.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.application.agency.persistence.model.user.AgencyUser;
import com.application.common.persistence.model.Image;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agency entity representing travel/tourism agencies that can make group reservations.
 * Similar to Restaurant entity but for agency operations.
 */
@Entity
@Table(name = "agency")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "vat_number", length = 50)
    private String vatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "agency_type", nullable = false)
    @Builder.Default
    private AgencyType agencyType = AgencyType.TRAVEL;

    @Column(name = "created_date")
    private LocalDate createdDate;

    @Column(name = "verified_date")
    private LocalDate verifiedDate;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Column(name = "notes", length = 1000)
    private String notes;

    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AgencyUser> agencyUsers = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "agency_images",
        joinColumns = @JoinColumn(name = "agency_id"),
        inverseJoinColumns = @JoinColumn(name = "image_id")
    )
    @Builder.Default
    private List<Image> images = new ArrayList<>();

    public enum Status {
        PENDING,     // Waiting for verification
        ACTIVE,      // Active and can make reservations
        SUSPENDED,   // Temporarily suspended
        INACTIVE,    // Deactivated
        DELETED      // Soft deleted
    }

    public enum AgencyType {
        TRAVEL,      // Travel agency
        TOURISM,     // Tourism operator
        EVENT,       // Event organizer
        CORPORATE,   // Corporate travel
        EDUCATIONAL, // Educational trips
        OTHER        // Other types
    }

    /**
     * Check if agency is active and can make reservations
     */
    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    /**
     * Check if agency is verified
     */
    public boolean isVerified() {
        return verifiedDate != null;
    }

    /**
     * Get full address as string
     */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (address != null) sb.append(address);
        if (city != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        if (postalCode != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(postalCode);
        }
        if (country != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(country);
        }
        return sb.toString();
    }

    /**
     * Add agency user to this agency
     */
    public void addAgencyUser(AgencyUser agencyUser) {
        if (agencyUsers == null) {
            agencyUsers = new ArrayList<>();
        }
        agencyUsers.add(agencyUser);
        agencyUser.setAgency(this);
    }

    /**
     * Remove agency user from this agency
     */
    public void removeAgencyUser(AgencyUser agencyUser) {
        if (agencyUsers != null) {
            agencyUsers.remove(agencyUser);
            agencyUser.setAgency(null);
        }
    }
}