package com.application.customer.controller.restaurant;

import java.time.LocalDate;
import java.util.Collection;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.web.dto.get.ServiceDTO;
import com.application.restaurant.service.RestaurantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Restaurant Services", description = "Controller for handling requests related to restaurant services")
@RequestMapping("/customer/restaurant")
@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerRestaurantServiceController {

	private final RestaurantService restaurantService;

	@GetMapping("/{restaurantId}/active-services-in-period")
	@Operation(summary = "Get active and enabled services of a restaurant for a specific period", description = "Retrieve the services of a restaurant that are active and enabled in a given date range")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ServiceDTO.class)))),
		@ApiResponse(responseCode = "404", description = "Restaurant not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<ServiceDTO>> getActiveEnabledServicesInPeriod(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		Collection<ServiceDTO> services = restaurantService.findActiveEnabledServicesInPeriod(restaurantId, start, end);
		return new ResponseEntity<>(services, HttpStatus.OK);
	}

	@GetMapping("/{restaurantId}/active-services-in-date")
	@Operation(summary = "Get active and enabled services of a restaurant for a specific date", description = "Retrieve the services of a restaurant that are active and enabled on a given date")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ServiceDTO.class)))),
		@ApiResponse(responseCode = "404", description = "Restaurant not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<ServiceDTO>> getActiveEnabledServicesInDate(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate date) {
		Collection<ServiceDTO> services = restaurantService.getActiveEnabledServices(restaurantId, date);
		return new ResponseEntity<>(services, HttpStatus.OK);
	}
}
