package com.application.restaurant.controller.restaurant;

import java.util.Collection;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.service.RestaurantService;
import com.application.common.web.dto.restaurant.SlotDTO;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.service.SlotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DEPRECATED: Use ServiceVersionScheduleController instead.
 * 
 * This controller is based on the legacy Slot architecture.
 * Will be removed in v3.0 (planned for Q2 2025).
 * 
 * <strong>Method Mapping:</strong>
 * <ul>
 *   <li>getDaySlots() → ServiceVersionScheduleController.getActiveTimeSlots()</li>
 *   <li>getAllSlots() → ServiceVersionScheduleController.getWeeklySchedule()</li>
 *   <li>getSlotById() → REMOVED (slots are computed dynamically, use date+time)</li>
 * </ul>
 * 
 * @deprecated Since v2.0, use {@link ServiceVersionScheduleController}
 * @see ServiceVersionScheduleController
 */
@Deprecated(since = "2.0", forRemoval = true)
@Tag(name = "Restaurant Slot Management (DEPRECATED)", description = "DEPRECATED - Use Service Version Schedules API instead")
@RestController
@RequestMapping("/restaurant/slot")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class RestaurantSlotManagementController extends BaseController {
	
	private final SlotService slotService;
	private final RestaurantService restaurantService;

	@GetMapping(value = "/day-slots")
	@Operation(summary = "Get day slots of the authenticated restaurant", description = "Retrieve the daily slots of the authenticated restaurant")
	@ReadApiResponses
	@Deprecated(since = "2.0", forRemoval = true)
    public ResponseEntity<List<SlotDTO>> getDaySlots(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate date,
			@AuthenticationPrincipal RUser rUser) {
		log.warn("DEPRECATED: getDaySlots() will be removed in v3.0. Use ServiceVersionScheduleController.getActiveTimeSlots() instead.");
		return executeList("get day slots", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			log.info("Getting day slots for restaurant ID: {} on date: {}", restaurantId, date);
			Collection<SlotDTO> slots = restaurantService.getDaySlots(restaurantId, date);
			return List.copyOf(slots);
		});
	}

	@GetMapping(value = "/all")
	@Operation(summary = "Get all slots of the authenticated restaurant", description = "Retrieve all available slots for the authenticated restaurant")
	@ReadApiResponses
	@Deprecated(since = "2.0", forRemoval = true)
    public ResponseEntity<List<SlotDTO>> getAllSlots(@AuthenticationPrincipal RUser rUser) {
		log.warn("DEPRECATED: getAllSlots() will be removed in v3.0. Use ServiceVersionScheduleController.getWeeklySchedule() instead.");
		return executeList("get all slots", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			log.info("Getting all slots for restaurant ID: {}", restaurantId);
			return slotService.findSlotsByRestaurantId(restaurantId);
		});
	}

	@GetMapping("/{slotId}")
	@Operation(summary = "Get slot by id", description = "Retrieve a slot by its ID")
	@ReadApiResponses
	@Deprecated(since = "2.0", forRemoval = true)
    public ResponseEntity<SlotDTO> getSlotById(@PathVariable Long slotId) {
		log.warn("DEPRECATED: getSlotById() will be removed in v3.0. Slots are computed dynamically, use date+time instead.");
		return execute("get slot by id", () -> {
			log.info("Getting slot by ID: {}", slotId);
			return slotService.findById(slotId);
		});
	}
}

