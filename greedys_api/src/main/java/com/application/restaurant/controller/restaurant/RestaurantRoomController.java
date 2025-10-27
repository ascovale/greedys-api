package com.application.restaurant.controller.restaurant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.web.dto.restaurant.RoomDTO;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.service.RoomService;
import com.application.restaurant.web.dto.restaurant.NewRoomDTO;

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
	
    public ResponseEntity<Page<RoomDTO>> getRooms(
    		@AuthenticationPrincipal RUser rUser,
    		@PageableDefault(size = 10, sort = "id") Pageable pageable) {
		return executePaginated("get restaurant rooms", () -> {
			log.info("Getting rooms for restaurant: {}", rUser.getRestaurant().getId());
			return roomService.findByRestaurant(rUser.getRestaurant().getId(), pageable);
		});
	}

	@PostMapping
	@Operation(summary = "Add a room to a restaurant", description = "Add a new room to a restaurant")
	@CreateApiResponses
    public ResponseEntity<RoomDTO> addRoom(@RequestBody NewRoomDTO roomDto,
			@AuthenticationPrincipal RUser rUser) {
		return executeCreate("add room", "Room added successfully", () -> {
			log.info("Adding new room for restaurant: {}", rUser.getRestaurant().getId());
			return roomService.createRoom(roomDto, rUser.getRestaurant().getId());
		});
	}

	@DeleteMapping(value = "/remove/{roomId}")
	@Operation(summary = "Remove a room", description = "Remove a specific room by its ID")
	@ReadApiResponses
	public ResponseEntity<String> removeRoom(@PathVariable Long roomId) {
		return execute("remove room", () -> {
			log.info("Removing room with ID: {}", roomId);
			roomService.deleteRoom(roomId);
			return "Room removed successfully";
		});
	}
	@GetMapping(value = "/{roomId}")
	@Operation(summary = "Get a specific room", description = "Retrieve details of a specific room by its ID")
	@ReadApiResponses
	public ResponseEntity<RoomDTO> getRoom(@PathVariable Long roomId) {
		return execute("get room", () -> {
			log.info("Getting room with ID: {}", roomId);
			return roomService.findRoomById(roomId);
		});
	}
}

