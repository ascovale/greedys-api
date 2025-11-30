package com.application.customer.controller.restaurant;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.web.dto.restaurant.SlotDTO;
import com.application.restaurant.service.SlotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DEPRECATED: Use CustomerServiceVersionScheduleController instead.
 * 
 * This controller is based on the legacy Slot architecture.
 * Will be removed in v3.0 (planned for Q2 2025).
 * 
 * @deprecated Since v2.0, use {@link CustomerServiceVersionScheduleController}
 * @see CustomerServiceVersionScheduleController
 */
@Deprecated(since = "2.0", forRemoval = true)
@Tag(name = "Restaurant Slots (DEPRECATED)", description = "DEPRECATED - Use Customer Service Version Schedules API instead")
@RequestMapping("/customer/restaurant")
@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerSlotController extends BaseController {

	private final SlotService slotService;

	@GetMapping("/{restaurantId}/slots")
	@Operation(summary = "Get all slots by restaurant ID", description = "Retrieve all available slots for a specific restaurant")
	@ReadApiResponses
	@Deprecated(since = "2.0", forRemoval = true)
	
    public ResponseEntity<List<SlotDTO>> getAllSlotsByRestaurantId(@PathVariable Long restaurantId) {
		log.warn("DEPRECATED: getAllSlotsByRestaurantId() will be removed in v3.0. Use CustomerServiceVersionScheduleController.getActiveScheduleForRestaurant() instead.");
		return executeList("get all slots by restaurant", () -> slotService.findSlotsByRestaurantId(restaurantId));
	}

	@Operation(summary = "Get slot by id", description = "Retrieve a slot by its ID")
	@ReadApiResponses
	@GetMapping("/slot/{slotId}")
	@Deprecated(since = "2.0", forRemoval = true)
	
    public ResponseEntity<SlotDTO> getSlotById(@PathVariable Long slotId) {
		log.warn("DEPRECATED: getSlotById() will be removed in v3.0. Use CustomerServiceVersionScheduleController.getTimeSlotDetails() instead.");
		return execute("get slot by id", () -> slotService.findById(slotId));
	}
}

