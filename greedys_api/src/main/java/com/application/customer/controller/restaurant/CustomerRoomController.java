package com.application.customer.controller.restaurant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.restaurant.RoomDTO;
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
	
	@Operation(summary = "Get rooms of a restaurant", description = "Retrieve the rooms of a restaurant")
    public ResponseEntity<ResponseWrapper<Page<RoomDTO>>> getRooms(
    		@PathVariable Long restaurantId,
    		@PageableDefault(size = 10, sort = "id") Pageable pageable) {
		return executePaginated("getRooms", () -> {
			return roomService.findByRestaurant(restaurantId, pageable);
		});
	}
}
