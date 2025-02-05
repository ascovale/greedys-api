package com.application.controller.admin;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.ServiceService;
import com.application.service.SlotService;
import com.application.web.dto.ServiceTypeDto;
import com.application.web.dto.get.ServiceDTO;
import com.application.web.dto.get.SlotDTO;
import com.application.web.dto.post.admin.AdminNewServiceDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin Service", description = "Controller per la gestione dei servizi offerti dai ristoranti")
@RestController
@RequestMapping("/admin/service")
@SecurityRequirement(name = "bearerAuth")
public class AdminServicesController {

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private SlotService slotService;

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new service", description = "This method creates a new service in the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/")
    public ResponseEntity<GenericResponse> newService(@RequestBody AdminNewServiceDTO servicesDto) {
        System.out.println("<<<   Controller Service   >>>");
        System.out.println("<<<   name: " + servicesDto.getName());
        serviceService.newService(servicesDto);
        return ResponseEntity.ok(new GenericResponse("success"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a service", description = "This method deletes a service by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid service ID"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/")
    public GenericResponse deleteService(@RequestParam Long serviceId) {
        System.out.println("<<<   Controller Service   >>>");
        System.out.println("<<<   serviceId: " + serviceId);
        serviceService.deleteService(serviceId);
        return new GenericResponse("success");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get service by ID", description = "Retrieve a service by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Service not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{serviceId}/")
    public ResponseEntity<ServiceDTO> getServiceById(@PathVariable Long id) {
        ServiceDTO service = serviceService.findById(id);
        return ResponseEntity.ok(service);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all slots of a service", description = "Retrieve all slots associated with a specific service by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Slots retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Service not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{serviceId}/slot")
    @ResponseBody
    public Collection<SlotDTO> getSlots(@PathVariable(value = "id") long serviceId) {
        return slotService.findByService_Id(serviceId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/types")
    @Operation(summary = "Get all service types", description = "Retrieve all service types.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service types retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Collection<ServiceTypeDto> getServiceTypes() {
        return serviceService.getServiceTypes();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new service type", description = "This method creates a new service type in the system.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service type created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/type")
    public ResponseEntity<GenericResponse> newServiceType(@RequestBody String serviceTypeString) {
        serviceService.newServiceType(serviceTypeString);
        return ResponseEntity.ok(new GenericResponse("success"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a service type", description = "This method updates an existing service type.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service type updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Service type not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/type/{typeId}")
    public ResponseEntity<GenericResponse> updateServiceType(@PathVariable Long typeId, @RequestBody String serviceTypeString) {
        serviceService.updateServiceType(typeId, serviceTypeString);
        return ResponseEntity.ok(new GenericResponse("success"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a service type", description = "This method deletes a service type by its ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service type deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid service type ID"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/type/{typeId}")
    public ResponseEntity<GenericResponse> deleteServiceType(@PathVariable Long typeId) {
        serviceService.deleteServiceType(typeId);
        return ResponseEntity.ok(new GenericResponse("success"));
    }

}
