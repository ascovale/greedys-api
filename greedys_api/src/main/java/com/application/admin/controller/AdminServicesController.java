package com.application.admin.controller;

import java.util.Collection;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.web.dto.ServiceTypeDto;
import com.application.common.web.util.GenericResponse;
import com.application.restaurant.service.ServiceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Services", description = "Admin Services Management")
@RestController
@RequestMapping("/admin/service")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class AdminServicesController {

    private final ServiceService serviceService;

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_READ')")
    @GetMapping("/types")
    @Operation(summary = "Get all service types", description = "Retrieve all service types.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service types retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Collection<ServiceTypeDto> getServiceTypes() {
        return serviceService.getServiceTypes();
    }

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
    @Operation(summary = "Create a new service type", description = "This method creates a new service type in the system.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service type created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/type/new")
    public ResponseEntity<GenericResponse> newServiceType(@RequestBody String serviceTypeString) {
        serviceService.newServiceType(serviceTypeString);
        return ResponseEntity.ok(new GenericResponse("success"));
    }

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
    @Operation(summary = "Update a service type", description = "This method updates an existing service type.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service type updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Service type not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/type/{typeId}/update")
    public ResponseEntity<GenericResponse> updateServiceType(@PathVariable Long typeId, @RequestBody String serviceTypeString) {
        serviceService.updateServiceType(typeId, serviceTypeString);
        return ResponseEntity.ok(new GenericResponse("success"));
    }

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
    @Operation(summary = "Delete a service type", description = "This method deletes a service type by its ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service type deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid service type ID"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/type/{typeId}/delete")
    public ResponseEntity<GenericResponse> deleteServiceType(@PathVariable Long typeId) {
        serviceService.deleteServiceType(typeId);
        return ResponseEntity.ok(new GenericResponse("success"));
    }

}
