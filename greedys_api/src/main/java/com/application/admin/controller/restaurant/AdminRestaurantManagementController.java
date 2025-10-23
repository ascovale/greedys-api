package com.application.admin.controller.restaurant;

import java.util.Collection;
import java.util.List;

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

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.service.RestaurantService;
import com.application.common.web.dto.restaurant.RestaurantDTO;
import com.application.common.web.dto.restaurant.ServiceDTO;

import io.swagger.v3.oas.annotations.Operation;
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
public class AdminRestaurantManagementController extends BaseController {

	private final RestaurantService restaurantService;

	@GetMapping(value = "/{restaurantId}/services")
	@Operation(summary = "Get services of a restaurant", description = "Retrieve the services of a restaurant")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_READ')")
	
    public ResponseEntity<List<ServiceDTO>> getServices(@PathVariable Long restaurantId) {
		return executeList("get services", () -> {
			Collection<ServiceDTO> services = restaurantService.getServices(restaurantId);
			return services instanceof java.util.List ? (java.util.List<ServiceDTO>) services : new java.util.ArrayList<>(services);
		});
	}

	@Operation(summary = "Set no show time limit", description = "Set the no-show time limit for reservations")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@PostMapping(value = "{restaurantId}/no_show_time_limit")
	
	public ResponseEntity<String> setNoShowTimeLimit(@PathVariable Long restaurantId, @RequestParam int minutes) {
		return execute("set no show time limit", () -> {
			restaurantService.setNoShowTimeLimit(restaurantId, minutes);
			return "No-show time limit updated successfully";
		});
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Enable restaurant", description = "Enable a restaurant by its primary email")
	@PutMapping("/{restaurantId}/enable_restaurant")
	
	public ResponseEntity<String> enableRestaurant(@PathVariable Long restaurantId) {
		return execute("enable restaurant", () -> {
			restaurantService.enableRestaurant(restaurantId);
			return "Restaurant enabled successfully";
		});
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Create restaurant", description = "Create a new restaurant")
	@CreateApiResponses
	@PostMapping("/new")
	public ResponseEntity<RestaurantDTO> createRestaurant(@RequestBody RestaurantDTO restaurantDto) {
		return executeCreate("create restaurant", () -> 
			restaurantService.createRestaurantAndReturnDTO(restaurantDto));
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Change restaurant email", description = "Change the email of a restaurant by its ID")
	@PutMapping("/{restaurantId}/change_email")
	public ResponseEntity<String> changeRestaurantEmail(@PathVariable Long restaurantId, @RequestParam String newEmail) {
		return execute("change restaurant email", () -> {
			restaurantService.changeRestaurantEmail(restaurantId, newEmail);
			return "Restaurant email changed successfully";
		});
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Mark restaurant as deleted", description = "Mark a restaurant as deleted or disabled by its ID")
	@DeleteMapping("/{restaurantId}/delete")
	public ResponseEntity<String> markRestaurantAsDeleted(@PathVariable Long restaurantId, @RequestParam boolean deleted) {
		return execute("mark restaurant as deleted", () -> {
			restaurantService.setRestaurantDeleted(restaurantId, deleted);
			return "Restaurant marked as deleted successfully";
		});
	}
}

