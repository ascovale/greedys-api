package com.application.restaurant.controller.restaurant;

import java.util.Collection;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.service.RestaurantService;
import com.application.common.web.ListResponseWrapper;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.restaurant.ServiceDTO;
import com.application.common.web.dto.restaurant.ServiceTypeDto;
import com.application.common.web.dto.restaurant.SlotDTO;
import com.application.restaurant.controller.utils.RestaurantControllerUtils;
import com.application.restaurant.service.ServiceService;
import com.application.restaurant.service.SlotService;
import com.application.restaurant.web.dto.services.RestaurantNewServiceDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    @ApiResponse(responseCode = "201", description = "Service created successfully", 
                content = @Content(schema = @Schema(implementation = String.class)))
    @CreateApiResponses
    @PreAuthorize("authentication.principal.isEnabled() & hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_WRITE')")
    @PostMapping("/new")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ResponseWrapper<String>> newService(@RequestBody RestaurantNewServiceDTO servicesDto) {
        return executeCreate("create new service", "Service created successfully", () -> {
            System.out.println("<<<   Controller Service   >>>");
            System.out.println("<<<   name: " + servicesDto.getName());
            serviceService.newService(servicesDto);
            return "success";
        });
    }

    @Operation(summary = "Delete a service", description = "This method deletes a service by its ID.")
    @ApiResponse(responseCode = "200", description = "Service deleted successfully", 
                content = @Content(schema = @Schema(implementation = String.class)))
    @ReadApiResponses
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
    @ApiResponse(responseCode = "200", description = "Service retrieved successfully", 
                content = @Content(schema = @Schema(implementation = ServiceDTO.class)))
    @ReadApiResponses
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_READ')")
    @GetMapping("/{serviceId}")
        public ResponseEntity<ResponseWrapper<ServiceDTO>> getServiceById(@PathVariable Long serviceId) {
        return execute("get service by id", () -> serviceService.findById(serviceId));
    }

    @Operation(summary = "Get all slots of a service", description = "Retrieve all slots associated with a specific service by its ID.")
    @ApiResponse(responseCode = "200", description = "Slots retrieved successfully", 
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = SlotDTO.class))))
    @ReadApiResponses
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_READ')")
    @GetMapping("/{serviceId}/slots")
        public ResponseEntity<ListResponseWrapper<SlotDTO>> getSlots(@PathVariable long serviceId) {
        return executeList("get service slots", () -> {
            Collection<SlotDTO> slots = slotService.findByService_Id(serviceId);
            return slots instanceof java.util.List ? (java.util.List<SlotDTO>) slots : new java.util.ArrayList<>(slots);
        });
    }

    @Operation(summary = "Get all service types", description = "Retrieve all service types.")
    @ApiResponse(responseCode = "200", description = "Service types retrieved successfully", 
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = ServiceTypeDto.class))))
    @ReadApiResponses
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_READ')")
    @GetMapping("/types")
        public ResponseEntity<ListResponseWrapper<ServiceTypeDto>> getServiceTypes() {
        return executeList("get service types", () -> {
            Collection<ServiceTypeDto> types = serviceService.getServiceTypesFromRUser();
            return types instanceof java.util.List ? (java.util.List<ServiceTypeDto>) types : new java.util.ArrayList<>(types);
        });
    }

    @Operation(summary = "Get services of a restaurant", description = "Retrieve the services of a restaurant")
    @ApiResponse(responseCode = "200", description = "Restaurant services retrieved successfully", 
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = ServiceDTO.class))))
    @ReadApiResponses
    @GetMapping(value = "/services")
        public ResponseEntity<ListResponseWrapper<ServiceDTO>> getServices() {
        return executeList("get restaurant services", () -> {
            Collection<ServiceDTO> services = restaurantService.getServices(RestaurantControllerUtils.getCurrentRestaurant().getId());
            return services instanceof java.util.List ? (java.util.List<ServiceDTO>) services : new java.util.ArrayList<>(services);
        });
    }
}
