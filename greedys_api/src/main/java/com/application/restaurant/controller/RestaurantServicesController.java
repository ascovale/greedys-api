package com.application.restaurant.controller;

import java.util.Collection;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.service.RestaurantService;
import com.application.common.web.ApiResponse;
import com.application.common.web.dto.restaurant.ServiceDTO;
import com.application.common.web.dto.restaurant.ServiceTypeDto;
import com.application.common.web.dto.restaurant.SlotDTO;
import com.application.restaurant.controller.utils.RestaurantControllerUtils;
import com.application.restaurant.service.ServiceService;
import com.application.restaurant.service.SlotService;
import com.application.restaurant.web.dto.services.RestaurantNewServiceDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Service Management", description = "Controller for managing services offered by restaurants")
@RestController
@RequestMapping("/restaurant/service")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class RestaurantServicesController extends BaseController {

    private final ServiceService serviceService;
    private final SlotService slotService;
    private final RestaurantService restaurantService;

    @Operation(summary = "Create a new service", description = "This method creates a new service in the system.")
    @PreAuthorize("authentication.principal.isEnabled() & hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_WRITE')")
    @PostMapping("/new")
    @CreateApiResponses
    public ResponseEntity<ApiResponse<String>> newService(@RequestBody RestaurantNewServiceDTO servicesDto) {
        return executeCreate("create new service", "Service created successfully", () -> {
            System.out.println("<<<   Controller Service   >>>");
            System.out.println("<<<   name: " + servicesDto.getName());
            serviceService.newService(servicesDto);
            return "success";
        });
    }

    @Operation(summary = "Delete a service", description = "This method deletes a service by its ID.")
    @PreAuthorize("authentication.principal.isEnabled() & hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_WRITE')")
    @DeleteMapping("/{serviceId}/delete")
    public ResponseEntity<ApiResponse<String>> deleteService(@PathVariable Long serviceId) {
        return executeVoid("delete service", "Service deleted successfully", () -> {
            System.out.println("<<<   Controller Service   >>>");
            System.out.println("<<<   serviceId: " + serviceId);
            serviceService.deleteService(serviceId);
        });
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_READ')")
    @Operation(summary = "Get service by ID", description = "Retrieve a service by its ID.")
    @GetMapping("/{serviceId}")
    @ReadApiResponses
    public ResponseEntity<ApiResponse<ServiceDTO>> getServiceById(@PathVariable Long serviceId) {
        return execute("get service by id", () -> serviceService.findById(serviceId));
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_READ')")
    @Operation(summary = "Get all slots of a service", description = "Retrieve all slots associated with a specific service by its ID.")
    @GetMapping("/{serviceId}/slots")
    @ReadApiResponses
    public ResponseEntity<ApiResponse<Collection<SlotDTO>>> getSlots(@PathVariable long serviceId) {
        return execute("get service slots", () -> slotService.findByService_Id(serviceId));
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_READ')")
    @GetMapping("/types")
    @ReadApiResponses
    @Operation(summary = "Get all service types", description = "Retrieve all service types.")
    public ResponseEntity<ApiResponse<Collection<ServiceTypeDto>>> getServiceTypes() {
        return execute("get service types", () -> serviceService.getServiceTypesFromRUser());
    }

    @GetMapping(value = "/services")
    @Operation(summary = "Get services of a restaurant", description = "Retrieve the services of a restaurant")
    @ReadApiResponses
    public ResponseEntity<ApiResponse<Collection<ServiceDTO>>> getServices() {
        return execute("get restaurant services", () -> 
            restaurantService.getServices(RestaurantControllerUtils.getCurrentRestaurant().getId()));
    }
}
