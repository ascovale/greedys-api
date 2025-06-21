package com.application.controller.admin;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.ReservationService;
import com.application.service.RestaurantService;
import com.application.service.RoomService;
import com.application.service.TableService;
import com.application.web.dto.RestaurantCategoryDTO;
import com.application.web.dto.get.ReservationDTO;
import com.application.web.dto.get.RestaurantDTO;
import com.application.web.dto.get.RoomDTO;
import com.application.web.dto.get.ServiceDTO;
import com.application.web.dto.get.TableDTO;
import com.application.web.dto.post.NewRoomDTO;
import com.application.web.dto.post.NewTableDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequestMapping("/admin/restaurant")
@RestController
@SecurityRequirement(name = "adminBearerAuth")
@Tag(name = "Restaurant", description = "Admin Restaurant Management")
public class AdminRestaurantController {

	//TODO: cambiare convenzione invece di restaurantId usare id_restaurant
	//TODO:Scrivere per aggiungere togliere privilegi customer ma anche restaurant user e andmin

	//TODO: Aggiungere createRestaurant

	private final RestaurantService restaurantService;
	private final ReservationService reservationService;
	private final RoomService roomService;
	private final TableService tableService;

	public AdminRestaurantController(RestaurantService restaurantService, ReservationService reservationService,
			RoomService roomService,
			TableService tableService) {
		this.restaurantService = restaurantService;
		this.reservationService = reservationService;
		this.roomService = roomService;
		this.tableService = tableService;
	}

	@Operation(summary = "Get all reservations of a restaurant", description = "Retrieve all reservations of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{restaurantId}/reservation")
	public Collection<ReservationDTO> getReservations(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		Collection<ReservationDTO> reservations = reservationService.getReservations(restaurantId, start, end);
		return reservations;
	}

	@Operation(summary = "Get all accepted reservations of a restaurant", description = "Retrieve all accepted reservations of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{restaurantId}/reservation/accepted")
	public Collection<ReservationDTO> getAcceptedReservations(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		Collection<ReservationDTO> reservations = reservationService.getAcceptedReservations(restaurantId, start, end);
		return reservations;
	}

	@Operation(summary = "Get all reservations of a restaurant with pagination", description = "Retrieve all reservations of a restaurant with pagination")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{restaurantId}/reservation/pageable")
	public ResponseEntity<Page<ReservationDTO>> getReservationsPageable(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end,
			@RequestParam int page,
			@RequestParam int size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<ReservationDTO> reservations = reservationService.getReservationsPageable(restaurantId, start, end,
				pageable);
		return new ResponseEntity<>(reservations, HttpStatus.OK);
	}

	@Operation(summary = "Get all pending reservations of a restaurant", description = "Retrieve all pending reservations of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{restaurantId}/reservation/pending")
	public Collection<ReservationDTO> getPendingReservations(
			@PathVariable Long restaurantId,
			@RequestParam(required = false) LocalDate start,
			@RequestParam(required = false) LocalDate end) {

		Collection<ReservationDTO> reservations;
		if (end != null && start != null) {
			reservations = reservationService.getPendingReservations(restaurantId, start, end);
		} else if (start != null) {
			reservations = reservationService.getPendingReservations(restaurantId, start);
		} else if (end != null) {
			throw new IllegalArgumentException("end cannot be null if start is not null");
		} else {
			reservations = reservationService.getPendingReservations(restaurantId);
		}
		return reservations;
	}

	@Operation(summary = "Get all pending reservations of a restaurant with pagination", description = "Retrieve all pending reservations of a restaurant with pagination")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
		@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{restaurantId}/reservation/pending/pageable")
	public ResponseEntity<Page<ReservationDTO>> getPendingReservationsPageable(
		@PathVariable Long restaurantId,
		@RequestParam(required = false) LocalDate start,
		@RequestParam(required = false) LocalDate end,
		@RequestParam int page,
		@RequestParam int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<ReservationDTO> reservations;

		if (end != null && start != null) {
		reservations = reservationService.getPendingReservationsPageable(restaurantId, start, end, pageable);
		} else if (start != null) {
		reservations = reservationService.getPendingReservationsPageable(restaurantId, start, pageable);
		} else if (end != null) {
		throw new IllegalArgumentException("end cannot be null if start is not null");
		} else {
		reservations = reservationService.getPendingReservationsPageable(restaurantId, pageable);
		}
		return new ResponseEntity<>(reservations, HttpStatus.OK);
	}

/* 
	@Operation(summary = "Accept a user", description = "Accept a user for a specific restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Restaurant or user not found")
	})
	@PostMapping("{RUserId}/accept")
	public GenericResponse acceptUser(@PathVariable Long RUserId) {
		RUserService.acceptRUser(RUserId);
		return new GenericResponse("success");
	}
*/
	@GetMapping(value = "/{restaurantId}/services")
	@Operation(summary = "Get services of a restaurant", description = "Retrieve the services of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ServiceDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_READ')")
	public ResponseEntity<Collection<ServiceDTO>> getServices(@PathVariable Long restaurantId) {
		Collection<ServiceDTO> services = restaurantService.getServices(restaurantId);
		return new ResponseEntity<>(services, HttpStatus.OK);
	}

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

	@Operation(summary = "Set no show time limit", description = "Set the no-show time limit for reservations")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "405", description = "Method not allowed"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@PostMapping(value = "{restaurantId}/no_show_time_limit")
	public GenericResponse setNoShowTimeLimit(@PathVariable Long restaurantId, @RequestParam int minutes) {
		restaurantService.setNoShowTimeLimit(restaurantId, minutes);
		return new GenericResponse("success");
	}

	@GetMapping(value = "{restaurantId}/categories")
	@Operation(summary = "Get types of a restaurant", description = "Retrieve the types of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = String.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "405", description = "Method not allowed"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	public ResponseEntity<Collection<String>> getRestaurantTypesNames(@PathVariable Long restaurantId) {
		List<String> types = restaurantService.getRestaurantTypesNames(restaurantId);
		return new ResponseEntity<>(types, HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Create category", description = "Create a new category for the specified restaurant")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Category created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "400", description = "Invalid request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "405", description = "Method not allowed"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PostMapping("/category/new")
	public GenericResponse createCategory(@RequestBody RestaurantCategoryDTO restaurantCategoryDto) {
		restaurantService.createRestaurantCategory(restaurantCategoryDto);
		return new GenericResponse("Category created successfully");
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Delete category", description = "Delete a category by its ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Category deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "400", description = "Invalid request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "405", description = "Method not allowed"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@DeleteMapping("/category/{categoryId}/delete")
	public GenericResponse deleteCategory(@PathVariable Long categoryId) {
		restaurantService.deleteRestaurantCategory(categoryId);
		return new GenericResponse("Category deleted successfully");
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Update category", description = "Update an existing category by its ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Category updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "400", description = "Invalid request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "405", description = "Method not allowed"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PutMapping("/category/{categoryId}/update")
	public GenericResponse updateCategory(@PathVariable Long categoryId,
			@RequestBody RestaurantCategoryDTO restaurantCategoryDto) {
		restaurantService.updateRestaurantCategory(categoryId, restaurantCategoryDto);
		return new GenericResponse("Category updated successfully");
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Enable restaurant", description = "Enable a restaurant by its primary email")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Restaurant enabled successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "400", description = "Invalid request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "405", description = "Method not allowed"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PutMapping("/{restaurantId}/enable_restaurant")
	public GenericResponse enableRestaurant(@PathVariable Long restaurantId) {
		restaurantService.enableRestaurant(restaurantId);
		return new GenericResponse("Restaurant enabled successfully");
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Create restaurant", description = "Create a new restaurant")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Restaurant created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "400", description = "Invalid request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "405", description = "Method not allowed"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PostMapping("/new")
	public GenericResponse createRestaurant(@RequestBody RestaurantDTO restaurantDto) {
		restaurantService.createRestaurant(restaurantDto);
		return new GenericResponse("Restaurant created successfully");
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Change restaurant email", description = "Change the email of a restaurant by its ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Restaurant email changed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "400", description = "Invalid request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "405", description = "Method not allowed"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PutMapping("/{restaurantId}/change_email")
	public GenericResponse changeRestaurantEmail(@PathVariable Long restaurantId, @RequestBody String newEmail) {
		restaurantService.changeRestaurantEmail(restaurantId, newEmail);
		return new GenericResponse("Restaurant email changed successfully");
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Mark restaurant as deleted", description = "Mark a restaurant as deleted or disabled by its ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Restaurant marked as deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "400", description = "Invalid request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "405", description = "Method not allowed"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@DeleteMapping("/{restaurantId}/delete")
	public GenericResponse markRestaurantAsDeleted(@PathVariable Long restaurantId, @RequestParam boolean deleted) {
		restaurantService.setRestaurantDeleted(restaurantId, deleted);
		return new GenericResponse("Restaurant marked as deleted successfully");
	}

	

}
