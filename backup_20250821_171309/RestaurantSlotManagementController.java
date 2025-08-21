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
import com.application.common.controller.annotation.WrapperDataType;
import com.application.common.controller.annotation.WrapperType;
import com.application.common.service.RestaurantService;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.restaurant.SlotDTO;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.service.SlotService;

import io.swagger.v3.oas.annotations.Operation;
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
public class RestaurantSlotManagementController extends BaseController {
	
	private final SlotService slotService;
	private final RestaurantService restaurantService;

	@GetMapping(value = "/day-slots")
	@Operation(summary = "Get day slots of the authenticated restaurant", description = "Retrieve the daily slots of the authenticated restaurant")
	
	@WrapperType(dataClass = SlotDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ResponseWrapper<List<SlotDTO>>> getDaySlots(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") java.time.LocalDate date,
			@AuthenticationPrincipal RUser rUser) {
		return executeList("get day slots", () -> {
			Long restaurantId = rUser.getId();
			log.info("Getting day slots for restaurant ID: {} on date: {}", restaurantId, date);
			Collection<SlotDTO> slots = restaurantService.getDaySlots(restaurantId, date);
			return List.copyOf(slots);
		});
	}

	@GetMapping(value = "/all")
	@Operation(summary = "Get all slots of the authenticated restaurant", description = "Retrieve all available slots for the authenticated restaurant")
	
	@WrapperType(dataClass = SlotDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ResponseWrapper<List<SlotDTO>>> getAllSlots(@AuthenticationPrincipal RUser rUser) {
		return executeList("get all slots", () -> {
			Long restaurantId = rUser.getId();
			log.info("Getting all slots for restaurant ID: {}", restaurantId);
			return slotService.findSlotsByRestaurantId(restaurantId);
		});
	}

	@GetMapping("/{slotId}")
	@Operation(summary = "Get slot by id", description = "Retrieve a slot by its ID")
	
	@WrapperType(dataClass = SlotDTO.class, type = WrapperDataType.DTO)
    public ResponseEntity<ResponseWrapper<SlotDTO>> getSlotById(@PathVariable Long slotId) {
		return execute("get slot by id", () -> {
			log.info("Getting slot by ID: {}", slotId);
			return slotService.findById(slotId);
		});
	}
}
