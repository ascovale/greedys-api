package com.application.customer.controller.restaurant;

import java.util.Collection;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.web.ApiResponse;
import com.application.common.web.dto.get.RoomDTO;
import com.application.restaurant.service.RoomService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Restaurant Rooms", description = "Controller for handling requests related to restaurant rooms")
@RequestMapping("/customer/restaurant")
@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerRoomController extends BaseController {

	private final RoomService roomService;

	@GetMapping("/{restaurantId}/rooms")
	@ReadApiResponses
	@Operation(summary = "Get rooms of a restaurant", description = "Retrieve the rooms of a restaurant")
	public ResponseEntity<ApiResponse<Collection<RoomDTO>>> getRooms(@PathVariable Long restaurantId) {
		return execute("getRooms", () -> roomService.findByRestaurant(restaurantId));
	}
}
