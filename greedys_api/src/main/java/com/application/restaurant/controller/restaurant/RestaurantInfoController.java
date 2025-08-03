package com.application.restaurant.controller.restaurant;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.service.RestaurantService;
import com.application.common.web.ListResponseWrapper;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.restaurant.ServiceDTO;
import com.application.restaurant.controller.utils.RestaurantControllerUtils;

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
	public ResponseWrapper<String> setNoShowTimeLimit(@RequestParam int minutes) {
		return executeVoid("set no-show time limit", "No-show time limit updated successfully", () -> {
			Long restaurantId = RestaurantControllerUtils.getCurrentRestaurant().getId();
			log.info("Setting no-show time limit to {} minutes for restaurant ID: {}", minutes, restaurantId);
			restaurantService.setNoShowTimeLimit(restaurantId, minutes);
			
		});
	}

	@GetMapping(value = "/types")
	@Operation(summary = "Get types of a restaurant", description = "Retrieve the types of a restaurant")
	@ReadApiResponses
	public ListResponseWrapper<String> getRestaurantTypesNames() {
		return executeList("get restaurant types", () -> {
			log.info("Getting restaurant types");
			return restaurantService.getRestaurantTypesNames();
		});
	}

	@GetMapping(value = "/open-days")
	@Operation(summary = "Get open days of the authenticated restaurant", description = "Retrieve the open days of the authenticated restaurant")
	@ReadApiResponses
	public ListResponseWrapper<String> getOpenDays(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") java.time.LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") java.time.LocalDate end) {
		return executeList("get open days", () -> {
			Long restaurantId = RestaurantControllerUtils.getCurrentRestaurant().getId();
			log.info("Getting open days for restaurant ID: {} from {} to {}", restaurantId, start, end);
			Collection<String> openDays = restaurantService.getOpenDays(restaurantId, start, end);
			return List.copyOf(openDays);
		});
	}

	@GetMapping(value = "/closed-days")
	@Operation(summary = "Get closed days of the authenticated restaurant", description = "Retrieve the closed days of the authenticated restaurant")
	@ReadApiResponses
	public ListResponseWrapper<LocalDate> getClosedDays(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") java.time.LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") java.time.LocalDate end) {
		return executeList("get closed days", () -> {
			Long restaurantId = RestaurantControllerUtils.getCurrentRestaurant().getId();
			log.info("Getting closed days for restaurant ID: {} from {} to {}", restaurantId, start, end);
			Collection<LocalDate> closedDays = restaurantService.getClosedDays(restaurantId, start, end);
			return List.copyOf(closedDays);
		});
	}

	@GetMapping(value = "/active-services-in-period")
	@Operation(summary = "Get active and enabled services of the authenticated restaurant for a specific period", description = "Retrieve the services of the authenticated restaurant that are active and enabled in a given date range")
	@ReadApiResponses
	public ListResponseWrapper<ServiceDTO> getActiveEnabledServicesInPeriod(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") java.time.LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") java.time.LocalDate end) {
		return executeList("get active services in period", () -> {
			Long restaurantId = RestaurantControllerUtils.getCurrentRestaurant().getId();
			log.info("Getting active enabled services for restaurant ID: {} from {} to {}", restaurantId, start, end);
			Collection<ServiceDTO> services = restaurantService.findActiveEnabledServicesInPeriod(restaurantId, start, end);
			return List.copyOf(services);
		});
	}

	@GetMapping(value = "/active-services-in-date")
	@Operation(summary = "Get active and enabled services of the authenticated restaurant for a specific date", description = "Retrieve the services of the authenticated restaurant that are active and enabled on a given date")
	@ReadApiResponses
	public ListResponseWrapper<ServiceDTO> getActiveEnabledServicesInDate(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") java.time.LocalDate date) {
		return executeList("get active services in date", () -> {
			Long restaurantId = RestaurantControllerUtils.getCurrentRestaurant().getId();
			log.info("Getting active enabled services for restaurant ID: {} on date: {}", restaurantId, date);
			Collection<ServiceDTO> services = restaurantService.getActiveEnabledServices(restaurantId, date);
			return List.copyOf(services); // Converte Collection in List
		});
	}

	@PostMapping(value = "/add-category")
	@Operation(summary = "Add a category to the restaurant", description = "Add a new category to the authenticated restaurant")
	public ResponseWrapper<String> addRestaurantCategory(@RequestParam Long categoryId) {
		return executeVoid("add restaurant category", "Category added successfully", () -> {
			Long restaurantId = RestaurantControllerUtils.getCurrentRestaurant().getId();
			log.info("Adding category ID '{}' to restaurant ID: {}", categoryId, restaurantId);
			restaurantService.addRestaurantCategory(restaurantId, categoryId);
		});
	}

	@PostMapping(value = "/remove-category")
	@Operation(summary = "Remove a category from the restaurant", description = "Remove a category from the authenticated restaurant")
	public ResponseWrapper<String> removeRestaurantCategory(@RequestParam Long categoryId) {
		return executeVoid("remove restaurant category", "Category removed successfully", () -> {
			Long restaurantId = RestaurantControllerUtils.getCurrentRestaurant().getId();
			log.info("Removing category ID '{}' from restaurant ID: {}", categoryId, restaurantId);
			restaurantService.removeRestaurantCategory(restaurantId, categoryId);
		});
	}
}
