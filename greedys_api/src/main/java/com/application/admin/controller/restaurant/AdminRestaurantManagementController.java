package com.application.admin.controller.restaurant;

import java.util.Collection;

import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

import com.application.common.web.dto.get.RestaurantDTO;
import com.application.common.web.dto.get.ServiceDTO;
import com.application.common.web.util.GenericResponse;
import com.application.restaurant.service.RestaurantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/admin/restaurant")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Restaurant Management", description = "Admin Restaurant Management Operations")
@RequiredArgsConstructor
@Slf4j
public class AdminRestaurantManagementController {

	private final RestaurantService restaurantService;

	@GetMapping(value = "/{restaurantId}/services")
	@Operation(summary = "Get services of a restaurant", description = "Retrieve the services of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ServiceDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_READ')")
	public ResponseEntity<Collection<ServiceDTO>> getServices(@PathVariable Long restaurantId) {
		Collection<ServiceDTO> services = restaurantService.getServices(restaurantId);
		return new ResponseEntity<>(services, HttpStatus.OK);
	}

	@Operation(summary = "Set no show time limit", description = "Set the no-show time limit for reservations")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "405", description = "Method not allowed"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@PostMapping(value = "{restaurantId}/no_show_time_limit")
	public GenericResponse setNoShowTimeLimit(@PathVariable Long restaurantId, @RequestParam int minutes) {
		restaurantService.setNoShowTimeLimit(restaurantId, minutes);
		return new GenericResponse("success");
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Enable restaurant", description = "Enable a restaurant by its primary email")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Restaurant enabled successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "400", description = "Invalid request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "405", description = "Method not allowed"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PutMapping("/{restaurantId}/enable_restaurant")
	public GenericResponse enableRestaurant(@PathVariable Long restaurantId) {
		restaurantService.enableRestaurant(restaurantId);
		return new GenericResponse("Restaurant enabled successfully");
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Create restaurant", description = "Create a new restaurant")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Restaurant created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "400", description = "Invalid request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "405", description = "Method not allowed"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PostMapping("/new")
	public GenericResponse createRestaurant(@RequestBody RestaurantDTO restaurantDto) {
		restaurantService.createRestaurant(restaurantDto);
		return new GenericResponse("Restaurant created successfully");
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Change restaurant email", description = "Change the email of a restaurant by its ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Restaurant email changed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "400", description = "Invalid request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "405", description = "Method not allowed"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PutMapping("/{restaurantId}/change_email")
	public GenericResponse changeRestaurantEmail(@PathVariable Long restaurantId, @RequestBody String newEmail) {
		restaurantService.changeRestaurantEmail(restaurantId, newEmail);
		return new GenericResponse("Restaurant email changed successfully");
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Mark restaurant as deleted", description = "Mark a restaurant as deleted or disabled by its ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Restaurant marked as deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "400", description = "Invalid request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "405", description = "Method not allowed"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@DeleteMapping("/{restaurantId}/delete")
	public GenericResponse markRestaurantAsDeleted(@PathVariable Long restaurantId, @RequestParam boolean deleted) {
		restaurantService.setRestaurantDeleted(restaurantId, deleted);
		return new GenericResponse("Restaurant marked as deleted successfully");
	}
}
