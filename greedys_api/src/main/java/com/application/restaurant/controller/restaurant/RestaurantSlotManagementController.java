package com.application.restaurant.controller.restaurant;

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

import com.application.restaurant.controller.utils.RestaurantControllerUtils;
import com.application.restaurant.service.RestaurantService;
import com.application.restaurant.service.SlotService;

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

@Tag(name = "Restaurant Slot Management", description = "Controller for managing restaurant slot operations")
@RestController
@RequestMapping("/restaurant/slot")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class RestaurantSlotManagementController {
	
	private final SlotService slotService;
	private final RestaurantService restaurantService;

	@GetMapping(value = "/day-slots")
	@Operation(summary = "Get day slots of the authenticated restaurant", description = "Retrieve the daily slots of the authenticated restaurant")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.application.common.web.dto.get.SlotDTO.class)))),
		@ApiResponse(responseCode = "404", description = "Restaurant not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<com.application.common.web.dto.get.SlotDTO>> getDaySlots(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") java.time.LocalDate date) {
		Long restaurantId = RestaurantControllerUtils.getCurrentRestaurant().getId();
		log.info("Getting day slots for restaurant ID: {} on date: {}", restaurantId, date);
		Collection<com.application.common.web.dto.get.SlotDTO> slots = restaurantService.getDaySlots(restaurantId, date);
		return new ResponseEntity<>(slots, HttpStatus.OK);
	}

	@GetMapping(value = "/all")
	@Operation(summary = "Get all slots of the authenticated restaurant", description = "Retrieve all available slots for the authenticated restaurant")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.application.common.web.dto.get.SlotDTO.class)))),
		@ApiResponse(responseCode = "404", description = "Restaurant not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<?> getAllSlots() {
		Long restaurantId = RestaurantControllerUtils.getCurrentRestaurant().getId();
		log.info("Getting all slots for restaurant ID: {}", restaurantId);
		List<com.application.common.web.dto.get.SlotDTO> slots = slotService.findSlotsByRestaurantId(restaurantId);
		return new ResponseEntity<>(slots, HttpStatus.OK);
	}

	@GetMapping("/{slotId}")
	@Operation(summary = "Get slot by id", description = "Retrieve a slot by its ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Slot found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.application.common.web.dto.get.SlotDTO.class))),
		@ApiResponse(responseCode = "404", description = "Slot not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public com.application.common.web.dto.get.SlotDTO getSlotById(@PathVariable Long slotId) {
		log.info("Getting slot by ID: {}", slotId);
		return slotService.findById(slotId);
	}
}
