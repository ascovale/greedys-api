package com.application.restaurant.controller.restaurant;

import java.util.Collection;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.web.util.GenericResponse;
import com.application.restaurant.controller.utils.RestaurantControllerUtils;
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

@Tag(name = "Restaurant Information Management", description = "Controller for managing restaurant information and settings")
@RestController
@RequestMapping("/restaurant/info")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class RestaurantInfoController {
	
	private final RestaurantService restaurantService;

	@PostMapping(value = "/no-show-time-limit")
	@Operation(summary = "Set no-show time limit", description = "Set the time limit for no-show reservations")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public GenericResponse setNoShowTimeLimit(@RequestParam int minutes) {
		Long restaurantId = RestaurantControllerUtils.getCurrentRestaurant().getId();
		log.info("Setting no-show time limit to {} minutes for restaurant ID: {}", minutes, restaurantId);
		restaurantService.setNoShowTimeLimit(restaurantId, minutes);
		return new GenericResponse("success");
	}

	@GetMapping(value = "/types")
	@Operation(summary = "Get types of a restaurant", description = "Retrieve the types of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = String.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<String>> getRestaurantTypesNames() {
		log.info("Getting restaurant types");
		List<String> types = restaurantService.getRestaurantTypesNames();
		return new ResponseEntity<>(types, HttpStatus.OK);
	}

	@GetMapping(value = "/open-days")
	@Operation(summary = "Get open days of the authenticated restaurant", description = "Retrieve the open days of the authenticated restaurant")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = String.class)))),
		@ApiResponse(responseCode = "404", description = "Restaurant not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<String>> getOpenDays(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") java.time.LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") java.time.LocalDate end) {
		Long restaurantId = RestaurantControllerUtils.getCurrentRestaurant().getId();
		log.info("Getting open days for restaurant ID: {} from {} to {}", restaurantId, start, end);
		Collection<String> openDays = restaurantService.getOpenDays(restaurantId, start, end);
		return new ResponseEntity<>(openDays, HttpStatus.OK);
	}

	@GetMapping(value = "/closed-days")
	@Operation(summary = "Get closed days of the authenticated restaurant", description = "Retrieve the closed days of the authenticated restaurant")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = java.time.LocalDate.class)))),
		@ApiResponse(responseCode = "404", description = "Restaurant not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<java.time.LocalDate>> getClosedDays(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") java.time.LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") java.time.LocalDate end) {
		Long restaurantId = RestaurantControllerUtils.getCurrentRestaurant().getId();
		log.info("Getting closed days for restaurant ID: {} from {} to {}", restaurantId, start, end);
		Collection<java.time.LocalDate> closedDays = restaurantService.getClosedDays(restaurantId, start, end);
		return new ResponseEntity<>(closedDays, HttpStatus.OK);
	}

	@GetMapping(value = "/active-services-in-period")
	@Operation(summary = "Get active and enabled services of the authenticated restaurant for a specific period", description = "Retrieve the services of the authenticated restaurant that are active and enabled in a given date range")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.application.common.web.dto.get.ServiceDTO.class)))),
		@ApiResponse(responseCode = "404", description = "Restaurant not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<com.application.common.web.dto.get.ServiceDTO>> getActiveEnabledServicesInPeriod(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") java.time.LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") java.time.LocalDate end) {
		Long restaurantId = RestaurantControllerUtils.getCurrentRestaurant().getId();
		log.info("Getting active enabled services for restaurant ID: {} from {} to {}", restaurantId, start, end);
		Collection<com.application.common.web.dto.get.ServiceDTO> services = restaurantService.findActiveEnabledServicesInPeriod(restaurantId, start, end);
		return new ResponseEntity<>(services, HttpStatus.OK);
	}

	@GetMapping(value = "/active-services-in-date")
	@Operation(summary = "Get active and enabled services of the authenticated restaurant for a specific date", description = "Retrieve the services of the authenticated restaurant that are active and enabled on a given date")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.application.common.web.dto.get.ServiceDTO.class)))),
		@ApiResponse(responseCode = "404", description = "Restaurant not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<com.application.common.web.dto.get.ServiceDTO>> getActiveEnabledServicesInDate(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") java.time.LocalDate date) {
		Long restaurantId = RestaurantControllerUtils.getCurrentRestaurant().getId();
		log.info("Getting active enabled services for restaurant ID: {} on date: {}", restaurantId, date);
		Collection<com.application.common.web.dto.get.ServiceDTO> services = restaurantService.getActiveEnabledServices(restaurantId, date);
		return new ResponseEntity<>(services, HttpStatus.OK);
	}
}
