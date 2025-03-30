package com.application.controller.restaurantUser;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.ServiceService;
import com.application.service.SlotService;
import com.application.web.dto.ServiceTypeDto;
import com.application.web.dto.get.ServiceDTO;
import com.application.web.dto.get.SlotDTO;
import com.application.web.dto.post.restaurant.RestaurantNewServiceDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;


@Tag(name = "Service", description = "Controller per la gestione dei servizi offerti dai ristoranti")
@RestController
@RequestMapping("/restaurant/service")
@SecurityRequirement(name = "restaurantBearerAuth")
public class RestaurantServicesController {

    @Autowired
    private ServiceService serviceService;
    @Autowired
    private SlotService slotService;

    @Operation(summary = "Create a new service", description = "This method creates a new service in the system.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("authentication.principal.isEnabled() & hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_WRITE')")
    @PostMapping("/new_service")
    public ResponseEntity<GenericResponse> newService(@RequestBody RestaurantNewServiceDTO servicesDto) {
        System.out.println("<<<   Controller Service   >>>");
        System.out.println("<<<   name: " + servicesDto.getName());
        serviceService.newService(servicesDto);
        return ResponseEntity.ok(new GenericResponse("success"));
    }

    @Operation(summary = "Delete a service", description = "This method deletes a service by its ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid service ID"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("authentication.principal.isEnabled() & hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_WRITE')")
    @DeleteMapping("/delete_service")
    public GenericResponse deleteService(@RequestParam Long serviceId) {
        System.out.println("<<<   Controller Service   >>>");
        System.out.println("<<<   serviceId: " + serviceId);
        serviceService.deleteService(serviceId);
        return new GenericResponse("success");
    }

    @PreAuthorize("authentication.principal.isEnabled() & hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_READ')")
    @Operation(summary = "Get service by ID", description = "Retrieve a service by its ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/get_service/{serviceId}")
    public ResponseEntity<ServiceDTO> getServiceById(@PathVariable Long serviceId) {
        ServiceDTO service = serviceService.findById(serviceId);
        return ResponseEntity.ok(service);
    }

    @PreAuthorize("authentication.principal.isEnabled() & hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_READ')")
    @Operation(summary = "Get all slots of a service", description = "Retrieve all slots associated with a specific service by its ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Slots retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/get_service_slot/{serviceId}")
    public Collection<SlotDTO> getSlots(@PathVariable(value = "serviceId") long serviceId) {
        return slotService.findByService_Id(serviceId);
    }

    @PreAuthorize("authentication.principal.isEnabled() & hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_READ')")
    @GetMapping("/service_types")
    @Operation(summary = "Get all service types", description = "Retrieve all service types.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service types retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Collection<ServiceTypeDto> getServiceTypes() {
        return serviceService.getServiceTypesFromRestaurantUser();
    }

}
