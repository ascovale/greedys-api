package com.application.controller.pub;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.RestaurantService;
import com.application.service.RoomService;
import com.application.service.SlotService;
import com.application.service.TableService;
import com.application.web.dto.get.RestaurantDTO;
import com.application.web.dto.get.RoomDTO;
import com.application.web.dto.get.ServiceDTO;
import com.application.web.dto.get.SlotDTO;
import com.application.web.dto.get.TableDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "1. Restaurant", description = "Controller for managing restaurants")
@RestController
@SecurityRequirement(name = "bearerAuth")
public class PublicRestaurantController {

	private final RestaurantService restaurantService;
	private final RoomService roomService;
	private final TableService tableService;
	private SlotService slotService;

	public PublicRestaurantController(RestaurantService restaurantService,
			RoomService roomService,
			TableService tableService,
			SlotService slotService) {
		this.restaurantService = restaurantService;
		this.roomService = roomService;
		this.tableService = tableService;
		this.slotService = slotService;
	}

	@Operation(summary = "Get all restaurants", description = "Retrieve all restaurants")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = RestaurantDTO.class)))),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@GetMapping(value = "")
	public ResponseEntity<Collection<RestaurantDTO>> getRestaurants() {
		Collection<RestaurantDTO> restaurants = restaurantService.findAll().stream().map(r -> new RestaurantDTO(r))
				.toList();
		return new ResponseEntity<>(restaurants, HttpStatus.OK);
	}

	@GetMapping(value = "/public/restaurant/{restaurantId}/open-days")
	@Operation(summary = "Get open days of a restaurant", description = "Retrieve the open days of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = LocalDate.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<String>> getOpenDays(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		Collection<String> openDays = restaurantService.getOpenDays(restaurantId, start, end);
		return new ResponseEntity<>(openDays, HttpStatus.OK);
	}

	@GetMapping(value = "/public/restaurant/{restaurantId}/closed-days")
	@Operation(summary = "Get closed days of a restaurant", description = "Retrieve the closed days of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = LocalDate.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<LocalDate>> getClosedDays(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		Collection<LocalDate> openDays = restaurantService.getClosedDays(restaurantId, start, end);
		return new ResponseEntity<>(openDays, HttpStatus.OK);
	}

	@GetMapping(value = "/public/restaurant/{restaurantId}/day-slots")
	@Operation(summary = "Get day slots of a restaurant", description = "Retrieve the daily slots of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SlotDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<SlotDTO>> getDaySlots(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate date) {
		Collection<SlotDTO> slots = restaurantService.getDaySlots(restaurantId, date);
		return new ResponseEntity<>(slots, HttpStatus.OK);
	}

	@Operation(summary = "Search restaurants by name", description = "Search for restaurants by name")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = RestaurantDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@GetMapping(value = "/public/restaurant/search")
	public ResponseEntity<Collection<RestaurantDTO>> searchRestaurants(@RequestParam String name) {
		Collection<RestaurantDTO> restaurants = restaurantService.findBySearchTerm(name);
		return new ResponseEntity<>(restaurants, HttpStatus.OK);
	}

	@GetMapping(value = "/public/restaurant/{restaurantId}/services")
	@Operation(summary = "Get services of a restaurant", description = "Retrieve the services of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ServiceDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<ServiceDTO>> getServices(@PathVariable Long restaurantId) {
		Collection<ServiceDTO> services = restaurantService.getServices(restaurantId);
		return new ResponseEntity<>(services, HttpStatus.OK);
	}

	/* -- === *** ROOMS AND TABLES *** === --- */

	@GetMapping(value = "/public/restaurant/{restaurantId}/rooms")
	@Operation(summary = "Get rooms of a restaurant", description = "Retrieve the rooms of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = RoomDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<RoomDTO>> getRooms(@PathVariable Long restaurantId) {
		Collection<RoomDTO> rooms = roomService.findByRestaurant(restaurantId);
		return new ResponseEntity<>(rooms, HttpStatus.OK);
	}

	@GetMapping(value = "/public/restaurant/room/{roomId}/tables")
	@Operation(summary = "Get tables of a room", description = "Retrieve the tables of a room")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TableDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant or room not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<TableDTO>> getTables(@PathVariable Long roomId) {
		Collection<TableDTO> tables = tableService.findByRoom(roomId);
		return new ResponseEntity<>(tables, HttpStatus.OK);
	}

	@GetMapping(value = "/public/restaurant/{restaurantId}/types")
	@Operation(summary = "Get types of a restaurant", description = "Retrieve the types of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = String.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<Collection<String>> getRestaurantTypesNames(@PathVariable Long restaurantId) {
		List<String> types = restaurantService.getRestaurantTypesNames(restaurantId);
		return new ResponseEntity<>(types, HttpStatus.OK);
	}
	@GetMapping(value = "/public/restaurant/{restaurantId}/slots")
	@Operation(summary = "Get all slots by restaurant ID", description = "Retrieve all available slots for a specific restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SlotDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public ResponseEntity<?> getAllSlotsByRestaurantId(@PathVariable Long restaurantId) {
		List<SlotDTO> slots = slotService.findSlotsByRestaurantId(restaurantId);
		return new ResponseEntity<>(slots, HttpStatus.OK);
	}

	@Operation(summary = "Get slot by id", description = "Retrieve a slot by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Slot found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SlotDTO.class))),
			@ApiResponse(responseCode = "404", description = "Slot not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	@GetMapping("public/restaurant/slot/{slotId}")
	public SlotDTO getSlotById(@PathVariable Long id) {
		return slotService.findById(id);
	}
}
