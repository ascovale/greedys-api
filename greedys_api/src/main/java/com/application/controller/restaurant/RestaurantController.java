package com.application.controller.restaurant;

import java.util.Collection;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.controller.utils.ControllerUtils;
import com.application.service.RestaurantService;
import com.application.service.RoomService;
import com.application.service.TableService;
import com.application.web.dto.get.RoomDTO;
import com.application.web.dto.get.TableDTO;
import com.application.web.dto.post.NewRoomDTO;
import com.application.web.dto.post.NewTableDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Restaurant Management", description = "Controller for managing restaurant operations")
@RestController
@RequestMapping("/restaurant")
// @PreAuthorize("@securityService.isRestaurantUserPermission(#idRestaurantUser)")
@SecurityRequirement(name = "restaurantBearerAuth")
public class RestaurantController {

	// Creare un tester che fa le richieste all'api e ne verifica la correttezza dei
	// risultati.

	private final RestaurantService restaurantService;
	private final RoomService roomService;
	private final TableService tableService;

	public RestaurantController(RestaurantService restaurantService,
			RoomService roomService,
			TableService tableService) {
		this.restaurantService = restaurantService;
		this.roomService = roomService;
		this.tableService = tableService;
	}

	/* -- === *** ROOMS AND TABLES *** === --- */

	@GetMapping(value = "/rooms")
	@Operation(summary = "Get rooms of a restaurant", description = "Retrieve the rooms of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = RoomDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<RoomDTO>> getRooms() {
		Collection<RoomDTO> rooms = roomService.findByRestaurant(ControllerUtils.getCurrentRestaurant().getId());
		return new ResponseEntity<>(rooms, HttpStatus.OK);
	}

	@GetMapping(value = "/room/{roomId}/tables")
	@Operation(summary = "Get tables of a room", description = "Retrieve the tables of a specific room")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TableDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant or room not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<TableDTO>> getTables(@PathVariable Long roomId) {
		// TODO: restituisce tutti i tavoli non di un ristorante specifico credo non lo
		// ho fatto io
		Collection<TableDTO> tables = tableService.findByRoom(roomId);
		return new ResponseEntity<>(tables, HttpStatus.OK);
	}

	@PostMapping(value = "/room")
	@Operation(summary = "Add a room to a restaurant", description = "Add a new room to a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public GenericResponse addRoom(@RequestBody NewRoomDTO roomDto) {
		// TODO: a quale ristorante associa la stanza?
		roomService.createRoom(roomDto);
		return new GenericResponse("success");
	}

	@PostMapping(value = "/table")
	@Operation(summary = "Add a table to a room", description = "Add a new table to a specific room")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Restaurant or room not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public GenericResponse addTable(@RequestParam NewTableDTO tableDto) {
		// TODO: a quale ristorante associa il tavolo
		tableService.createTable(tableDto);
		return new GenericResponse("success");
	}

	@Operation(summary = "Set no-show time limit", description = "Set the time limit for no-show reservations")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	@PostMapping(value = "/no_show_time_limit")
	public GenericResponse setNoShowTimeLimit(@RequestParam int minutes) {
		restaurantService.setNoShowTimeLimit(ControllerUtils.getCurrentRestaurant().getId(), minutes);
		return new GenericResponse("success");
	}

	@GetMapping(value = "/types")
	@Operation(summary = "Get types of a restaurant", description = "Retrieve the types of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = String.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<String>> getRestaurantTypesNames() {
		List<String> types = restaurantService.getRestaurantTypesNames();
		return new ResponseEntity<>(types, HttpStatus.OK);
	}

	@Operation(summary = "Remove a table", description = "Remove a specific table by its ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "404", description = "Table not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	@DeleteMapping(value = "/table/remove/{tableId}")
	public GenericResponse removeTable(@PathVariable Long tableId) {
		tableService.deleteTable(tableId);
		return new GenericResponse("Table removed successfully");
	}

	@Operation(summary = "Remove a room", description = "Remove a specific room by its ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "404", description = "Room not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	@DeleteMapping(value = "/room/remove/{roomId}")
	public GenericResponse removeRoom(@PathVariable Long roomId) {
		roomService.deleteRoom(roomId);
		return new GenericResponse("Room removed successfully");
	}

}
