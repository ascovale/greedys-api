package com.application.restaurant.controller.restaurant;

import java.util.Collection;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.web.dto.get.RoomDTO;
import com.application.common.web.util.GenericResponse;
import com.application.restaurant.controller.utils.RestaurantControllerUtils;
import com.application.restaurant.service.RoomService;
import com.application.restaurant.web.post.NewRoomDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
public class RestaurantRoomController {
	
	private final RoomService roomService;

	@GetMapping(value = "/all")
	@Operation(summary = "Get rooms of a restaurant", description = "Retrieve the rooms of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = RoomDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<RoomDTO>> getRooms() {
		log.info("Getting rooms for restaurant: {}", RestaurantControllerUtils.getCurrentRestaurant().getId());
		Collection<RoomDTO> rooms = roomService.findByRestaurant(RestaurantControllerUtils.getCurrentRestaurant().getId());
		return new ResponseEntity<>(rooms, HttpStatus.OK);
	}

	@PostMapping
	@Operation(summary = "Add a room to a restaurant", description = "Add a new room to a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public GenericResponse addRoom(@RequestBody NewRoomDTO roomDto) {
		log.info("Adding new room for restaurant: {}", RestaurantControllerUtils.getCurrentRestaurant().getId());
		roomService.createRoom(roomDto);
		return new GenericResponse("success");
	}

	@DeleteMapping(value = "/remove/{roomId}")
	@Operation(summary = "Remove a room", description = "Remove a specific room by its ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "404", description = "Room not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public GenericResponse removeRoom(@PathVariable Long roomId) {
		log.info("Removing room with ID: {}", roomId);
		roomService.deleteRoom(roomId);
		return new GenericResponse("Room removed successfully");
	}
}
