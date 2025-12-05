package com.application.restaurant.controller.restaurant;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.service.RestaurantService;
import com.application.common.web.dto.restaurant.ServiceDTO;
import com.application.restaurant.persistence.model.user.RUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Restaurant Information Management", description = "Controller for managing restaurant information and settings")
@RestController
@RequestMapping("/restaurant/info")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class RestaurantInfoController extends BaseController {
	
	private final RestaurantService restaurantService;

	@PostMapping(value = "/no-show-time-limit")
	@Operation(summary = "Set no-show time limit", description = "Set the time limit for no-show reservations")
	@ReadApiResponses
	public ResponseEntity<String> setNoShowTimeLimit(@RequestParam int minutes, @AuthenticationPrincipal RUser rUser) {
		return execute("set no-show time limit", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			log.info("Setting no-show time limit to {} minutes for restaurant ID: {}", minutes, restaurantId);
			restaurantService.setNoShowTimeLimit(restaurantId, minutes);
			return "No-show time limit updated successfully";
		});
	}

	@GetMapping(value = "/types")
	@Operation(summary = "Get types of a restaurant", description = "Retrieve the types of a restaurant")
	@ReadApiResponses
	public ResponseEntity<List<String>> getRestaurantTypesNames() {
		return executeList("get restaurant types", () -> {
			log.info("Getting restaurant types");
			return restaurantService.getRestaurantTypesNames();
		});
	}

	@GetMapping(value = "/open-days")
	@Operation(summary = "Get open days of the authenticated restaurant", description = "Retrieve the open days of the authenticated restaurant")
	@ReadApiResponses
	public ResponseEntity<List<String>> getOpenDays(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
			@AuthenticationPrincipal RUser rUser) {
		return executeList("get open days", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			LocalDate startDate = start != null ? start.toLocalDate() : null;
			LocalDate endDate = end != null ? end.toLocalDate() : null;
			log.info("Getting open days for restaurant ID: {} from {} to {}", restaurantId, startDate, endDate);
			Collection<String> openDays = restaurantService.getOpenDays(restaurantId, startDate, endDate);
			return List.copyOf(openDays);
		});
	}

	@GetMapping(value = "/closed-days")
	@Operation(summary = "Get closed days of the authenticated restaurant", description = "Retrieve the closed days of the authenticated restaurant")
	@ReadApiResponses
	
    public ResponseEntity<List<LocalDate>> getClosedDays(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
			@AuthenticationPrincipal RUser rUser) {
		return executeList("get closed days", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			LocalDate startDate = start != null ? start.toLocalDate() : null;
			LocalDate endDate = end != null ? end.toLocalDate() : null;
			log.info("Getting closed days for restaurant ID: {} from {} to {}", restaurantId, startDate, endDate);
			Collection<LocalDate> closedDays = restaurantService.getClosedDays(restaurantId, startDate, endDate);
			return List.copyOf(closedDays);
		});
	}

	@GetMapping(value = "/active-services-in-period")
	@Operation(summary = "Get active and enabled services of the authenticated restaurant for a specific period", description = "Retrieve the services of the authenticated restaurant that are active and enabled in a given date range")
	@ReadApiResponses
    public ResponseEntity<List<ServiceDTO>> getActiveEnabledServicesInPeriod(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
			@AuthenticationPrincipal RUser rUser) {
		return executeList("get active services in period", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			LocalDate startDate = start != null ? start.toLocalDate() : null;
			LocalDate endDate = end != null ? end.toLocalDate() : null;
			log.info("Getting active enabled services for restaurant ID: {} from {} to {}", restaurantId, startDate, endDate);
			Collection<ServiceDTO> services = restaurantService.findActiveEnabledServicesInPeriod(restaurantId, startDate, endDate);
			return List.copyOf(services);
		});
	}

	@GetMapping(value = "/active-services-in-date")
	@Operation(summary = "Get active and enabled services of the authenticated restaurant for a specific date", description = "Retrieve the services of the authenticated restaurant that are active and enabled on a given date")
	@ReadApiResponses
    public ResponseEntity<List<ServiceDTO>> getActiveEnabledServicesInDate(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date,
			@AuthenticationPrincipal RUser rUser) {
		return executeList("get active services in date", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			LocalDate dateAsLocalDate = date != null ? date.toLocalDate() : null;
			log.info("Getting active enabled services for restaurant ID: {} on date: {}", restaurantId, dateAsLocalDate);
			Collection<ServiceDTO> services = restaurantService.getActiveEnabledServices(restaurantId, dateAsLocalDate);
			return List.copyOf(services); // Converte Collection in List
		});
	}

	@PostMapping(value = "/add-category")
	@Operation(summary = "Add a category to the restaurant", description = "Add a new category to the authenticated restaurant")
	public ResponseEntity<String> addRestaurantCategory(@RequestParam Long categoryId,
			@AuthenticationPrincipal RUser rUser) {
		return execute("add restaurant category", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			log.info("Adding category ID '{}' to restaurant ID: {}", categoryId, restaurantId);
			restaurantService.addRestaurantCategory(restaurantId, categoryId);
			return "Category added successfully";
		});
	}

	@PostMapping(value = "/remove-category")
	@Operation(summary = "Remove a category from the restaurant", description = "Remove a category from the authenticated restaurant")
	public ResponseEntity<String> removeRestaurantCategory(@RequestParam Long categoryId,
			@AuthenticationPrincipal RUser rUser) {
		return execute("remove restaurant category", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			log.info("Removing category ID '{}' from restaurant ID: {}", categoryId, restaurantId);
			restaurantService.removeRestaurantCategory(restaurantId, categoryId);
			return "Category removed successfully";
		});
	}

	// ==================== DESCRIPTION MANAGEMENT ====================

	@PostMapping(value = "/description")
	@Operation(
		summary = "Update restaurant description",
		description = "Update the description of the authenticated restaurant (max 1000 characters)"
	)
	@ReadApiResponses
	public ResponseEntity<String> updateDescription(
			@RequestParam String description,
			@AuthenticationPrincipal RUser rUser) {
		return execute("update restaurant description", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			
			if (description != null && description.length() > 1000) {
				throw new IllegalArgumentException("La descrizione non può superare i 1000 caratteri");
			}
			
			log.info("Updating description for restaurant ID: {}", restaurantId);
			restaurantService.updateDescription(restaurantId, description);
			return "Description updated successfully";
		});
	}

	@GetMapping(value = "/description")
	@Operation(summary = "Get restaurant description", description = "Retrieve the description of the authenticated restaurant")
	@ReadApiResponses
	public ResponseEntity<String> getDescription(@AuthenticationPrincipal RUser rUser) {
		return execute("get restaurant description", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			log.info("Getting description for restaurant ID: {}", restaurantId);
			return restaurantService.getDescription(restaurantId);
		});
	}

	
}

