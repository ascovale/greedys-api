package com.application.restaurant.controller.restaurant;

import java.util.Collection;
import java.util.List;

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
import com.application.common.controller.annotation.WrapperDataType;
import com.application.common.controller.annotation.WrapperType;
import com.application.common.web.ResponseWrapper;
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
	
	@WrapperType(dataClass = RoomDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ResponseWrapper<List<RoomDTO>>> getRooms(@AuthenticationPrincipal RUser rUser) {
		return executeList("get restaurant rooms", () -> {
			log.info("Getting rooms for restaurant: {}", rUser.getRestaurant().getId());
			Collection<RoomDTO> rooms = roomService.findByRestaurant(rUser.getRestaurant().getId());
			return List.copyOf(rooms);
		});
	}

	@PostMapping
	@Operation(summary = "Add a room to a restaurant", description = "Add a new room to a restaurant")
	@WrapperType(dataClass = RoomDTO.class, type = WrapperDataType.DTO, responseCode = "201")
    public ResponseEntity<ResponseWrapper<RoomDTO>> addRoom(@RequestBody NewRoomDTO roomDto,
			@AuthenticationPrincipal RUser rUser) {
		return executeCreate("add room", "Room added successfully", () -> {
			log.info("Adding new room for restaurant: {}", rUser.getRestaurant().getId());
			return roomService.createRoom(roomDto, rUser.getRestaurant().getId());
		});
	}

	@DeleteMapping(value = "/remove/{roomId}")
	@Operation(summary = "Remove a room", description = "Remove a specific room by its ID")
	public ResponseEntity<ResponseWrapper<String>> removeRoom(@PathVariable Long roomId) {
		return executeVoid("remove room", "Room removed successfully", () -> {
			log.info("Removing room with ID: {}", roomId);
			roomService.deleteRoom(roomId);
		});
	}
}
