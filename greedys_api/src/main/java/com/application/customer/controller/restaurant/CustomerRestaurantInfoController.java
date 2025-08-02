package com.application.customer.controller.restaurant;

import java.time.LocalDate;
import java.util.Collection;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.service.RestaurantService;
import com.application.common.web.ApiResponse;
import com.application.common.web.dto.restaurant.RestaurantDTO;
import com.application.common.web.dto.restaurant.SlotDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Restaurant Info", description = "Controller for handling requests related to restaurant information")
@RequestMapping("/customer/restaurant")
@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerRestaurantInfoController extends BaseController {

	private final RestaurantService restaurantService;

	@Operation(summary = "Get all restaurants", description = "Retrieve all restaurants")
	@GetMapping("")
	@ReadApiResponses
	public ResponseEntity<ApiResponse<Collection<RestaurantDTO>>> getRestaurants() {
		return execute("get all restaurants", () -> restaurantService.findAll().stream().map(r -> new RestaurantDTO(r)).toList());
	}

	@GetMapping("/{restaurantId}/open-days")
	@Operation(summary = "Get open days of a restaurant", description = "Retrieve the open days of a restaurant")
	@ReadApiResponses
	public ResponseEntity<ApiResponse<Collection<String>>> getOpenDays(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		return execute("get open days", () -> restaurantService.getOpenDays(restaurantId, start, end));
	}

	@GetMapping("/{restaurantId}/closed-days")
	@Operation(summary = "Get closed days of a restaurant", description = "Retrieve the closed days of a restaurant")
	@ReadApiResponses
	public ResponseEntity<ApiResponse<Collection<LocalDate>>> getClosedDays(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		return execute("get closed days", () -> restaurantService.getClosedDays(restaurantId, start, end));
	}

	@GetMapping("/{restaurantId}/day-slots")
	@Operation(summary = "Get day slots of a restaurant", description = "Retrieve the daily slots of a restaurant")
	@ReadApiResponses
	public ResponseEntity<ApiResponse<Collection<SlotDTO>>> getDaySlots(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate date) {
		return execute("get day slots", () -> restaurantService.getDaySlots(restaurantId, date));
	}

	@Operation(summary = "Search restaurants by name", description = "Search for restaurants by name")
	@GetMapping("/search")
	@ReadApiResponses
	public ResponseEntity<ApiResponse<Collection<RestaurantDTO>>> searchRestaurants(@RequestParam String name) {
		return execute("search restaurants", () -> restaurantService.findBySearchTerm(name));
	}

	@GetMapping("/{restaurantId}/types")
	@Operation(summary = "Get types of a restaurant", description = "Retrieve the types of a restaurant")
	@ReadApiResponses
	public ResponseEntity<ApiResponse<Collection<String>>> getRestaurantTypesNames(@PathVariable Long restaurantId) {
		return execute("get restaurant types", () -> restaurantService.getRestaurantTypesNames(restaurantId));
	}


}
