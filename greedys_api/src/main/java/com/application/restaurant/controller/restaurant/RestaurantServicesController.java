package com.application.restaurant.controller.restaurant;

import java.util.Collection;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.WrapperDataType;
import com.application.common.controller.annotation.WrapperType;
import com.application.common.service.RestaurantService;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.restaurant.ServiceDTO;
import com.application.common.web.dto.restaurant.ServiceTypeDto;
import com.application.common.web.dto.restaurant.SlotDTO;
import com.application.restaurant.persistence.model.user.RUser;
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
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_WRITE')")
    @PostMapping("/new")
    @WrapperType(dataClass = ServiceDTO.class, responseCode = "201")
    public ResponseEntity<ResponseWrapper<ServiceDTO>> newService(@RequestBody RestaurantNewServiceDTO servicesDto) {
        return executeCreate("create new service", "Service created successfully", () -> {
            System.out.println("<<<   Controller Service   >>>");
            System.out.println("<<<   name: " + servicesDto.getName());
            return serviceService.newService(servicesDto);
        });
    }

    @Operation(summary = "Delete a service", description = "This method deletes a service by its ID.")
    
    @PreAuthorize("authentication.principal.isEnabled() & hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_WRITE')")
    @DeleteMapping("/{serviceId}/delete")
        public ResponseEntity<ResponseWrapper<String>> deleteService(@PathVariable Long serviceId) {
        return executeVoid("delete service", "Service deleted successfully", () -> {
            System.out.println("<<<   Controller Service   >>>");
            System.out.println("<<<   serviceId: " + serviceId);
            serviceService.deleteService(serviceId);
        });
    }

    @Operation(summary = "Get service by ID", description = "Retrieve a service by its ID.")
    
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_READ')")
    @GetMapping("/{serviceId}")
        @WrapperType(dataClass = ServiceDTO.class, type = WrapperDataType.DTO)
    public ResponseEntity<ResponseWrapper<ServiceDTO>> getServiceById(@PathVariable Long serviceId) {
        return execute("get service by id", () -> serviceService.findById(serviceId));
    }

    @Operation(summary = "Get all slots of a service", description = "Retrieve all slots associated with a specific service by its ID.")
    
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_READ')")
    @GetMapping("/{serviceId}/slots")
    @WrapperType(dataClass = SlotDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ResponseWrapper<List<SlotDTO>>> getSlots(@PathVariable long serviceId) {
        return executeList("get service slots", () -> {
            Collection<SlotDTO> slots = slotService.findByService_Id(serviceId);
            return slots instanceof java.util.List ? (java.util.List<SlotDTO>) slots : new java.util.ArrayList<>(slots);
        });
    }

    @Operation(summary = "Get all service types", description = "Retrieve all service types.")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_READ')")
    @GetMapping("/types")
    @WrapperType(dataClass = ServiceTypeDto.class, type = WrapperDataType.LIST)
    public ResponseEntity<ResponseWrapper<List<ServiceTypeDto>>> getServiceTypes() {
        return executeList("get service types", () -> {
            Collection<ServiceTypeDto> types = serviceService.getServiceTypesFromRUser();
            return types instanceof java.util.List ? (java.util.List<ServiceTypeDto>) types : new java.util.ArrayList<>(types);
        });
    }

    @Operation(summary = "Get services of a restaurant", description = "Retrieve the services of a restaurant")
    @GetMapping(value = "/services")
    @WrapperType(dataClass = ServiceDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ResponseWrapper<List<ServiceDTO>>> getServices(@AuthenticationPrincipal RUser rUser) {
        return executeList("get restaurant services", () -> {
            Collection<ServiceDTO> services = restaurantService.getServices(rUser.getRestaurant().getId());
            return services instanceof java.util.List ? (java.util.List<ServiceDTO>) services : new java.util.ArrayList<>(services);
        });
    }
}
