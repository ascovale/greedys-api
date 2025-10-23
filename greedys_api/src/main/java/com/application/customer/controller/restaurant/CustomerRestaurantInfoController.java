package com.application.customer.controller.restaurant;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.service.RestaurantService;
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
	
    public ResponseEntity<Page<RestaurantDTO>> getRestaurants(
    		@PageableDefault(size = 10, sort = "id") Pageable pageable) {
		return executePaginated("get all restaurants", () -> restaurantService.findAllPaginated(pageable));
	}

	@GetMapping("/{restaurantId}/open-days")
	@Operation(summary = "Get open days of a restaurant", description = "Retrieve the open days of a restaurant")
	public ResponseEntity<List<String>> getOpenDays(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		return executeList("get open days", () -> {
			Collection<String> openDays = restaurantService.getOpenDays(restaurantId, start, end);
			return openDays instanceof List ? (List<String>) openDays : List.copyOf(openDays);
		});
	}

	@GetMapping("/{restaurantId}/closed-days")
	@Operation(summary = "Get closed days of a restaurant", description = "Retrieve the closed days of a restaurant")
	
    public ResponseEntity<List<LocalDate>> getClosedDays(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		return executeList("get closed days", () -> {
			Collection<LocalDate> closedDays = restaurantService.getClosedDays(restaurantId, start, end);
			return closedDays instanceof List ? (List<LocalDate>) closedDays : List.copyOf(closedDays);
		});
	}

	@GetMapping("/{restaurantId}/day-slots")
	@Operation(summary = "Get day slots of a restaurant", description = "Retrieve the daily slots of a restaurant")
	
    public ResponseEntity<List<SlotDTO>> getDaySlots(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate date) {
		return executeList("get day slots", () -> {
			Collection<SlotDTO> slots = restaurantService.getDaySlots(restaurantId, date);
			return slots instanceof List ? (List<SlotDTO>) slots : List.copyOf(slots);
		});
	}

	@Operation(summary = "Search restaurants by name", description = "Search for restaurants by name")
	@GetMapping("/search")
	
    public ResponseEntity<List<RestaurantDTO>> searchRestaurants(@RequestParam String name) {
		return executeList("search restaurants", () -> {
			Collection<RestaurantDTO> restaurants = restaurantService.findBySearchTerm(name);
			return restaurants instanceof List ? (List<RestaurantDTO>) restaurants : List.copyOf(restaurants);
		});
	}

	@GetMapping("/{restaurantId}/types")
	@Operation(summary = "Get types of a restaurant", description = "Retrieve the types of a restaurant")
	public ResponseEntity<List<String>> getRestaurantTypesNames(@PathVariable Long restaurantId) {
		return executeList("get restaurant types", () -> {
			Collection<String> types = restaurantService.getRestaurantTypesNames(restaurantId);
			return types instanceof List ? (List<String>) types : List.copyOf(types);
		});
	}


}

