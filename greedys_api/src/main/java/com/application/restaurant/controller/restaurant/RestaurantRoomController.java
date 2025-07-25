package com.application.restaurant.controller.restaurant;

import java.util.Collection;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.web.dto.ApiResponse;
import com.application.common.web.dto.get.RoomDTO;
import com.application.restaurant.controller.utils.RestaurantControllerUtils;
import com.application.restaurant.persistence.model.Room;
import com.application.restaurant.service.RoomService;
import com.application.restaurant.web.dto.post.NewRoomDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Restaurant Room Management", description = "Controller for managing restaurant room operations")
@RestController
@RequestMapping("/restaurant/room")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class RestaurantRoomController extends BaseController {
	
	private final RoomService roomService;

	@GetMapping(value = "/all")
	@Operation(summary = "Get rooms of a restaurant", description = "Retrieve the rooms of a restaurant")
	@ReadApiResponses
	public ResponseEntity<ApiResponse<Collection<RoomDTO>>> getRooms() {
		return execute("get restaurant rooms", () -> {
			log.info("Getting rooms for restaurant: {}", RestaurantControllerUtils.getCurrentRestaurant().getId());
			return roomService.findByRestaurant(RestaurantControllerUtils.getCurrentRestaurant().getId());
		});
	}

	@PostMapping
	@Operation(summary = "Add a room to a restaurant", description = "Add a new room to a restaurant")
	public ResponseEntity<ApiResponse<Room>> addRoom(@RequestBody NewRoomDTO roomDto) {
		return executeCreate("add room", "Room added successfully", () -> {
			log.info("Adding new room for restaurant: {}", RestaurantControllerUtils.getCurrentRestaurant().getId());
			return roomService.createRoom(roomDto);
		});
	}

	@DeleteMapping(value = "/remove/{roomId}")
	@Operation(summary = "Remove a room", description = "Remove a specific room by its ID")
	public ResponseEntity<ApiResponse<String>> removeRoom(@PathVariable Long roomId) {
		return executeVoid("remove room", "Room removed successfully", () -> {
			log.info("Removing room with ID: {}", roomId);
			roomService.deleteRoom(roomId);
		});
	}
}
