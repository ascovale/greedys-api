package com.application.customer.controller.restaurant;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.web.dto.get.SlotDTO;
import com.application.restaurant.service.SlotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Restaurant Slots", description = "Controller for handling requests related to restaurant slots")
@RequestMapping("/customer/restaurant")
@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerSlotController {

	private final SlotService slotService;

	@GetMapping("/{restaurantId}/slots")
	@Operation(summary = "Get all slots by restaurant ID", description = "Retrieve all available slots for a specific restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SlotDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<?> getAllSlotsByRestaurantId(@PathVariable Long restaurantId) {
		List<SlotDTO> slots = slotService.findSlotsByRestaurantId(restaurantId);
		return new ResponseEntity<>(slots, HttpStatus.OK);
	}

	@Operation(summary = "Get slot by id", description = "Retrieve a slot by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Slot found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SlotDTO.class))),
			@ApiResponse(responseCode = "404", description = "Slot not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	@GetMapping("/slot/{slotId}")
	public SlotDTO getSlotById(@PathVariable Long slotId) {
		return slotService.findById(slotId);
	}
}
