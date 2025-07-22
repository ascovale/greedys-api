package com.application.admin.controller.restaurant;

import java.util.Collection;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.web.dto.get.RoomDTO;
import com.application.common.web.dto.get.TableDTO;
import com.application.common.web.util.GenericResponse;
import com.application.restaurant.service.RoomService;
import com.application.restaurant.service.TableService;
import com.application.restaurant.web.post.NewRoomDTO;
import com.application.restaurant.web.post.NewTableDTO;

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

@RequestMapping("/admin/restaurant")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Room & Table", description = "Admin Room and Table Management")
@RequiredArgsConstructor
@Slf4j
public class AdminRoomTableController {

	private final RoomService roomService;
	private final TableService tableService;

	/* -- === *** ROOMS AND TABLES *** === --- */

	@GetMapping(value = "/{restaurantId}/rooms")
	@Operation(summary = "Get rooms of a restaurant", description = "Retrieve the rooms of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = RoomDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "405", description = "Method not allowed"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_READ')")
	public ResponseEntity<Collection<RoomDTO>> getRooms(@PathVariable Long restaurantId) {
		Collection<RoomDTO> rooms = roomService.findByRestaurant(restaurantId);
		return new ResponseEntity<>(rooms, HttpStatus.OK);
	}

	@GetMapping(value = "/room/{roomId}/tables")
	@Operation(summary = "Get tables of a room", description = "Retrieve the tables of a room")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TableDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant or room not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "405", description = "Method not allowed"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_READ')")
	public ResponseEntity<Collection<TableDTO>> getTables(@PathVariable Long roomId) {
		Collection<TableDTO> tables = tableService.findByRoom(roomId);
		return new ResponseEntity<>(tables, HttpStatus.OK);
	}

	@PostMapping(value = "/{restaurantId}/room")
	@Operation(summary = "Add a room to a restaurant", description = "Add a new room to a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "405", description = "Method not allowed"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	public GenericResponse addRoom(@PathVariable Long restaurantId, @RequestBody NewRoomDTO roomDto) {
		roomService.createRoom(roomDto);
		return new GenericResponse("success");
	}

	@PostMapping(value = "/{restaurantId}/table")
	@Operation(summary = "Add a table to a room", description = "Add a new table to a room")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Restaurant or room not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "405", description = "Method not allowed"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	public GenericResponse addTable(@PathVariable Long restaurantId, @RequestParam NewTableDTO tableDto) {
		tableService.createTable(tableDto);
		return new GenericResponse("success");
	}
}
