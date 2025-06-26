package com.application.controller.restaurant;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

import com.application.controller.utils.ControllerUtils;
import com.application.service.ReservationService;
import com.application.service.RestaurantService;
import com.application.service.ServiceService;
import com.application.service.SlotService;
import com.application.web.dto.ServiceTypeDto;
import com.application.web.dto.get.ServiceDTO;
import com.application.web.dto.get.SlotDTO;
import com.application.web.dto.post.restaurant.RestaurantNewServiceDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Service Management", description = "Controller for managing services offered by restaurants")
@RestController
@RequestMapping("/restaurant/service")
@SecurityRequirement(name = "bearerAuth")
public class RestaurantServicesController {

    @Autowired
    private ServiceService serviceService;
    @Autowired
    private SlotService slotService;
    @Autowired
    RestaurantService restaurantService;
    @Autowired
    ReservationService reservationService;

    @Operation(summary = "Create a new service", description = "This method creates a new service in the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("authentication.principal.isEnabled() & hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_WRITE')")
    @PostMapping("/new")
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
    @DeleteMapping("/{serviceId}/delete")
    public GenericResponse deleteService(@PathVariable Long serviceId) {
        System.out.println("<<<   Controller Service   >>>");
        System.out.println("<<<   serviceId: " + serviceId);
        serviceService.deleteService(serviceId);
        return new GenericResponse("success");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_READ')")
    @Operation(summary = "Get service by ID", description = "Retrieve a service by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Service not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{serviceId}")
    public ResponseEntity<ServiceDTO> getServiceById(@PathVariable Long serviceId) {
        ServiceDTO service = serviceService.findById(serviceId);
        return ResponseEntity.ok(service);
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_READ')")
    @Operation(summary = "Get all slots of a service", description = "Retrieve all slots associated with a specific service by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Slots retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Service not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{serviceId}/slots")
    public Collection<SlotDTO> getSlots(@PathVariable long serviceId) {
        return slotService.findByService_Id(serviceId);
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SERVICE_READ')")
    @GetMapping("/types")
    @Operation(summary = "Get all service types", description = "Retrieve all service types.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service types retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Collection<ServiceTypeDto> getServiceTypes() {
        return serviceService.getServiceTypesFromRUser();
    }

    @GetMapping(value = "/services")
    @Operation(summary = "Get services of a restaurant", description = "Retrieve the services of a restaurant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ServiceDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Restaurant not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<Collection<ServiceDTO>> getServices() {
        Collection<ServiceDTO> services = restaurantService.getServices(ControllerUtils.getCurrentRestaurant().getId());
        return new ResponseEntity<>(services, HttpStatus.OK);
    }

}
