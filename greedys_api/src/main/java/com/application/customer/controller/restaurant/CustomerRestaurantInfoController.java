package com.application.customer.controller.restaurant;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.web.dto.get.RestaurantDTO;
import com.application.common.web.dto.get.ServiceDTO;
import com.application.common.web.dto.get.SlotDTO;
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

@Tag(name = "Restaurant Info", description = "Controller for handling requests related to restaurant information")
@RequestMapping("/customer/restaurant")
@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerRestaurantInfoController {

	private final RestaurantService restaurantService;

	@Operation(summary = "Get all restaurants", description = "Retrieve all restaurants")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = RestaurantDTO.class)))),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@GetMapping("")
	public ResponseEntity<Collection<RestaurantDTO>> getRestaurants() {
		Collection<RestaurantDTO> restaurants = restaurantService.findAll().stream().map(r -> new RestaurantDTO(r))
				.toList();
		return new ResponseEntity<>(restaurants, HttpStatus.OK);
	}

	@GetMapping("/{restaurantId}/open-days")
	@Operation(summary = "Get open days of a restaurant", description = "Retrieve the open days of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = LocalDate.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<String>> getOpenDays(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		Collection<String> openDays = restaurantService.getOpenDays(restaurantId, start, end);
		return new ResponseEntity<>(openDays, HttpStatus.OK);
	}

	@GetMapping("/{restaurantId}/closed-days")
	@Operation(summary = "Get closed days of a restaurant", description = "Retrieve the closed days of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = LocalDate.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<LocalDate>> getClosedDays(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		Collection<LocalDate> openDays = restaurantService.getClosedDays(restaurantId, start, end);
		return new ResponseEntity<>(openDays, HttpStatus.OK);
	}

	@GetMapping("/{restaurantId}/day-slots")
	@Operation(summary = "Get day slots of a restaurant", description = "Retrieve the daily slots of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SlotDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<SlotDTO>> getDaySlots(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate date) {
		Collection<SlotDTO> slots = restaurantService.getDaySlots(restaurantId, date);
		return new ResponseEntity<>(slots, HttpStatus.OK);
	}

	@Operation(summary = "Search restaurants by name", description = "Search for restaurants by name")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = RestaurantDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@GetMapping("/search")
	public ResponseEntity<Collection<RestaurantDTO>> searchRestaurants(@RequestParam String name) {
		Collection<RestaurantDTO> restaurants = restaurantService.findBySearchTerm(name);
		return new ResponseEntity<>(restaurants, HttpStatus.OK);
	}

	@GetMapping("/{restaurantId}/types")
	@Operation(summary = "Get types of a restaurant", description = "Retrieve the types of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = String.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<String>> getRestaurantTypesNames(@PathVariable Long restaurantId) {
		List<String> types = restaurantService.getRestaurantTypesNames(restaurantId);
		return new ResponseEntity<>(types, HttpStatus.OK);
	}

	/**
     * @deprecated Usa {@link CustomerRestaurantServiceController#getActiveEnabledServicesInDate(Long, LocalDate)} al posto di questo metodo.
     */
    @Deprecated
    @GetMapping("/{restaurantId}/services")
    @Operation(summary = "Get services of a restaurant", description = "Retrieve the services of a restaurant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ServiceDTO.class)))),
        @ApiResponse(responseCode = "404", description = "Restaurant not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<Collection<ServiceDTO>> getServices(@PathVariable Long restaurantId) {
        Collection<ServiceDTO> services = restaurantService.getServices(restaurantId);
        return new ResponseEntity<>(services, HttpStatus.OK);
    }
}
