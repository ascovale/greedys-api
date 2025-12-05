package com.application.agency.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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

import com.application.agency.persistence.model.Agency;
import com.application.agency.service.AgencyService;
import com.application.agency.web.dto.AgencyCreateDTO;
import com.application.agency.web.dto.AgencyDTO;
import com.application.agency.web.dto.AgencyUpdateDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/agency")
@Tag(name = "Agency Management", description = "APIs for managing travel agencies")
public class AgencyController {

    @Autowired
    private AgencyService agencyService;

    @Operation(summary = "Create new agency", description = "Register a new travel agency with admin user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Agency created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Agency name or admin email already exists")
    })
    @PostMapping
    public ResponseEntity<AgencyDTO> createAgency(@Valid @RequestBody AgencyCreateDTO createDTO) {
        try {
            Agency agency = agencyService.createAgency(
                createDTO.getName(),
                createDTO.getDescription(),
                createDTO.getAdminEmail(),
                createDTO.getAdminFirstName(),
                createDTO.getAdminLastName(),
                createDTO.getAdminPhoneNumber()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(agency));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Operation(summary = "Get agency by ID", description = "Retrieve agency details by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Agency found"),
        @ApiResponse(responseCode = "404", description = "Agency not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AgencyDTO> getAgency(
            @Parameter(description = "Agency ID", required = true) @PathVariable Long id) {
        try {
            Agency agency = agencyService.findAgencyById(id);
            return ResponseEntity.ok(convertToDTO(agency));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get all agencies", description = "Retrieve list of all agencies with pagination")
    @GetMapping
    public ResponseEntity<Page<AgencyDTO>> getAllAgencies(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status) {
        
        if (status != null) {
            try {
                Agency.Status agencyStatus = Agency.Status.valueOf(status.toUpperCase());
                List<Agency> agencies = agencyService.getAgenciesByStatus(agencyStatus);
                Page<AgencyDTO> agencyDTOs = agencies.stream()
                    .map(this::convertToDTO)
                    .collect(java.util.stream.Collectors.toList())
                    .stream()
                    .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        list -> new org.springframework.data.domain.PageImpl<>(list)
                    ));
                return ResponseEntity.ok(agencyDTOs);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        
        Page<Agency> agenciesPage = agencyService.getAgenciesPage(page, size, sortBy);
        Page<AgencyDTO> agencyDTOs = agenciesPage.map(this::convertToDTO);
        return ResponseEntity.ok(agencyDTOs);
    }

    @Operation(summary = "Search agencies", description = "Search agencies by name or city")
    @GetMapping("/search")
    public ResponseEntity<List<AgencyDTO>> searchAgencies(
            @Parameter(description = "Search term", required = true) @RequestParam String query) {
        List<Agency> agencies = agencyService.searchAgencies(query);
        List<AgencyDTO> agencyDTOs = agencies.stream()
            .map(this::convertToDTO)
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(agencyDTOs);
    }

    @Operation(summary = "Update agency", description = "Update agency information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Agency updated successfully"),
        @ApiResponse(responseCode = "404", description = "Agency not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PutMapping("/{id}")
    public ResponseEntity<AgencyDTO> updateAgency(
            @Parameter(description = "Agency ID", required = true) @PathVariable Long id,
            @Valid @RequestBody AgencyUpdateDTO updateDTO) {
        try {
            Agency agency = agencyService.updateAgency(
                id,
                updateDTO.getName(),
                updateDTO.getDescription(),
                updateDTO.getStatus()
            );
            return ResponseEntity.ok(convertToDTO(agency));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Activate agency", description = "Activate an agency")
    @PutMapping("/{id}/activate")
    public ResponseEntity<AgencyDTO> activateAgency(@PathVariable Long id) {
        try {
            Agency agency = agencyService.activateAgency(id);
            return ResponseEntity.ok(convertToDTO(agency));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Suspend agency", description = "Suspend an agency")
    @PutMapping("/{id}/suspend")
    public ResponseEntity<AgencyDTO> suspendAgency(@PathVariable Long id) {
        try {
            Agency agency = agencyService.suspendAgency(id);
            return ResponseEntity.ok(convertToDTO(agency));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Delete agency", description = "Soft delete an agency")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgency(@PathVariable Long id) {
        try {
            agencyService.deleteAgency(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get agency statistics", description = "Get user statistics for an agency")
    @GetMapping("/{id}/statistics")
    public ResponseEntity<AgencyService.AgencyStats> getAgencyStatistics(@PathVariable Long id) {
        try {
            AgencyService.AgencyStats stats = agencyService.getAgencyStatistics(id);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Helper method to convert Agency to DTO
    private AgencyDTO convertToDTO(Agency agency) {
        AgencyDTO dto = new AgencyDTO();
        dto.setId(agency.getId());
        dto.setName(agency.getName());
        dto.setDescription(agency.getDescription());
        dto.setEmail(agency.getEmail());
        dto.setPhoneNumber(agency.getPhoneNumber());
        dto.setWebsite(agency.getWebsite());
        dto.setAddress(agency.getAddress());
        dto.setCity(agency.getCity());
        dto.setCountry(agency.getCountry());
        dto.setPostalCode(agency.getPostalCode());
        dto.setTaxCode(agency.getTaxCode());
        dto.setVatNumber(agency.getVatNumber());
        dto.setStatus(agency.getStatus().toString());
        dto.setAgencyType(agency.getAgencyType().toString());
        dto.setCreatedDate(agency.getCreatedDate());
        dto.setVerifiedDate(agency.getVerifiedDate());
        dto.setLicenseNumber(agency.getLicenseNumber());
        dto.setNotes(agency.getNotes());
        dto.setActive(agency.isActive());
        dto.setVerified(agency.isVerified());
        dto.setFullAddress(agency.getFullAddress());
        return dto;
    }
}