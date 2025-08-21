package com.application.customer.controller.restaurant;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.WrapperDataType;
import com.application.common.controller.annotation.WrapperType;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.restaurant.SlotDTO;
import com.application.restaurant.service.SlotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Restaurant Slots", description = "Controller for handling requests related to restaurant slots")
@RequestMapping("/customer/restaurant")
@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerSlotController extends BaseController {

	private final SlotService slotService;

	@GetMapping("/{restaurantId}/slots")
	@Operation(summary = "Get all slots by restaurant ID", description = "Retrieve all available slots for a specific restaurant")
	
	@WrapperType(dataClass = SlotDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ResponseWrapper<List<SlotDTO>>> getAllSlotsByRestaurantId(@PathVariable Long restaurantId) {
		return executeList("get all slots by restaurant", () -> slotService.findSlotsByRestaurantId(restaurantId));
	}

	@Operation(summary = "Get slot by id", description = "Retrieve a slot by its ID")
	@GetMapping("/slot/{slotId}")
	
	@WrapperType(dataClass = SlotDTO.class, type = WrapperDataType.DTO)
    public ResponseEntity<ResponseWrapper<SlotDTO>> getSlotById(@PathVariable Long slotId) {
		return execute("get slot by id", () -> slotService.findById(slotId));
	}
}
