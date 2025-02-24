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
import com.application.service.RestaurantUserService;
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
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Restaurant", description = "Admin management APIs for the Restaurant")
public class AdminRestaurantController {

	private final RestaurantService restaurantService;
	private final ReservationService reservationService;
	private final RestaurantUserService restaurantUserService;
	private final RoomService roomService;
	private final TableService tableService;

	public AdminRestaurantController(RestaurantService restaurantService, ReservationService reservationService,
			RestaurantUserService restaurantUserService, RoomService roomService,
			TableService tableService) {
		this.restaurantService = restaurantService;
		this.reservationService = reservationService;
		this.restaurantUserService = restaurantUserService;
		this.roomService = roomService;
		this.tableService = tableService;
	}

	@Operation(summary = "Get all reservations of a restaurant", description = "Ottieni tutte le prenotazioni di un ristorante")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Ristorante non trovato")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{idRestaurant}/reservation")
	public Collection<ReservationDTO> getReservations(
			@PathVariable Long idRestaurant,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		Collection<ReservationDTO> reservations = reservationService.getReservations(idRestaurant, start, end);
		return reservations;
	}

	@Operation(summary = "Get all accepted reservations of a restaurant", description = "Ottieni tutte le prenotazioni accettate di un ristorante")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Ristorante non trovato")
	})
	// @PreAuthorize("@securityService.hasRestaurantUserPermissionOnRestaurantWithId(#idRestaurant)
	// or hasRole('ADMIN')")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{idRestaurant}/reservation/accepted")
	public Collection<ReservationDTO> getAcceptedReservations(
			@PathVariable Long idRestaurant,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		Collection<ReservationDTO> reservations = reservationService.getAcceptedReservations(idRestaurant, start, end);
		return reservations;
	}

	@Operation(summary = "Get all reservations of a restaurant with pagination", description = "Ottieni tutte le prenotazioni di un ristorante con paginazione")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Ristorante non trovato")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{idRestaurant}/reservation/pageable")
	public ResponseEntity<Page<ReservationDTO>> getReservationsPageable(
			@PathVariable Long idRestaurant,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end,
			@RequestParam int page,
			@RequestParam int size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<ReservationDTO> reservations = reservationService.getReservationsPageable(idRestaurant, start, end,
				pageable);
		return new ResponseEntity<>(reservations, HttpStatus.OK);
	}

	// TODO PAGEABLE
	@Operation(summary = "Get all pending reservations of a restaurant", description = "Ottieni tutte le prenotazioni in attesa di un ristorante")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Ristorante non trovato")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{idRestaurant}/reservation/pending")
	public Collection<ReservationDTO> getPendingReservations(
			@PathVariable Long idRestaurant,
			@RequestParam(required = false) LocalDate start,
			@RequestParam(required = false) LocalDate end) {

		Collection<ReservationDTO> reservations;
		if (end != null && start != null) {
			reservations = reservationService.getPendingReservations(idRestaurant, start, end);
		} else if (start != null) {
			reservations = reservationService.getPendingReservations(idRestaurant, start);
		} else if (end != null) {
			throw new IllegalArgumentException("end cannot be null if start is not null");
		} else {
			reservations = reservationService.getPendingReservations(idRestaurant);
		}
		return reservations;
	}

	@Operation(summary = "Accept a user", description = "Accetta un utente per un ristorante specifico")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Ristorante o utente non trovato")
	})
	@PostMapping("{idRestaurant}/user/accept")
	public GenericResponse acceptUser(@PathVariable Long idRestaurant) {
		// TODO : verificare che venga messo chi è l'utente ad accettare la prenotazione
		restaurantUserService.acceptUser(idRestaurant);
		return new GenericResponse("success");
	}

	@GetMapping(value = "/{idRestaurant}/services")
	@Operation(summary = "Get services of a restaurant", description = "Ottieni i servizi di un ristorante")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ServiceDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_READ')")
	public ResponseEntity<Collection<ServiceDTO>> getServices(@PathVariable Long idRestaurant) {
		Collection<ServiceDTO> services = restaurantService.getServices(idRestaurant);
		return new ResponseEntity<>(services, HttpStatus.OK);
	}

	/* -- === *** ROOMS AND TABLES *** === --- */

	@GetMapping(value = "/{idRestaurant}/rooms")
	@Operation(summary = "Get rooms of a restaurant", description = "Ottieni le sale di un ristorante")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = RoomDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_READ')")
	public ResponseEntity<Collection<RoomDTO>> getRooms(@PathVariable Long idRestaurant) {
		Collection<RoomDTO> rooms = roomService.findByRestaurant(idRestaurant);
		return new ResponseEntity<>(rooms, HttpStatus.OK);
	}

	@GetMapping(value = "/{idRestaurant}/room/{roomId}/tables")
	@Operation(summary = "Get tables of a room", description = "Ottieni i tavoli di una sala")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TableDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Ristorante o sala non trovato"),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_READ')")
	public ResponseEntity<Collection<TableDTO>> getTables(@PathVariable Long idRestaurant, @PathVariable Long roomId) {
		Collection<TableDTO> tables = tableService.findByRoom(roomId);
		return new ResponseEntity<>(tables, HttpStatus.OK);
	}

	@PostMapping(value = "/{idRestaurant}/room")
	@Operation(summary = "Add a room to a restaurant", description = "Aggiungi una sala a un ristorante")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	public GenericResponse addRoom(@PathVariable Long idRestaurant, @RequestBody NewRoomDTO roomDto) {
		// TODO: sistemare idRestaurant
		roomService.createRoom(roomDto);
		return new GenericResponse("success");
	}

	@PostMapping(value = "/{idRestaurant}/table")
	@Operation(summary = "Add a table to a room", description = "Aggiungi un tavolo a una sala")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Ristorante o sala non trovato"),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	public GenericResponse addTable(@PathVariable Long idRestaurant, @RequestParam NewTableDTO tableDto) {
		// TODO: sistemare idRestaurant
		tableService.createTable(tableDto);
		return new GenericResponse("success");
	}

	@Operation(summary = "Set no show time limit", description = "Imposta il limite di tempo per il no show")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@PostMapping(value = "{idRestaurant}/no-show-time-limit")
	public GenericResponse setNoShowTimeLimit(@PathVariable Long idRestaurant, @RequestParam int minutes) {
		restaurantService.setNoShowTimeLimit(idRestaurant, minutes);
		return new GenericResponse("success");
	}

	@GetMapping(value = "{idRestaurant}/types")
	@Operation(summary = "Get types of a restaurant", description = "Ottieni i tipi di un ristorante")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = String.class)))),
			@ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	public ResponseEntity<Collection<String>> getRestaurantTypesNames(@PathVariable Long idRestaurant) {
		List<String> types = restaurantService.getRestaurantTypesNames(idRestaurant);
		return new ResponseEntity<>(types, HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Create category", description = "Creates a new category for the specified restaurant by its ID")
	@ApiResponse(responseCode = "200", description = "Category created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
	@ApiResponse(responseCode = "400", description = "Invalid request")
	@PostMapping("/createRestaurantCategory")
	public GenericResponse createCategory(@RequestBody RestaurantCategoryDTO restaurantCategoryDto) {
		restaurantService.createRestaurantCategory(restaurantCategoryDto);
		return new GenericResponse("Category created successfully");
	}
	// TODO verificare che non lo cancello effettivamente dal db

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Delete category", description = "Deletes a category by its ID")
	@ApiResponse(responseCode = "200", description = "Category deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
	@ApiResponse(responseCode = "400", description = "Invalid request")
	@DeleteMapping("/deleteRestaurantCategory/{idCategory}")
	public GenericResponse deleteCategory(@PathVariable Long idCategory) {
		restaurantService.deleteRestaurantCategory(idCategory);
		return new GenericResponse("Category deleted successfully");
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Update category", description = "Updates an existing category by its ID")
	@ApiResponse(responseCode = "200", description = "Category updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
	@ApiResponse(responseCode = "400", description = "Invalid request")
	@PutMapping("/updateRestaurantCategory/{idCategory}")
	public GenericResponse updateCategory(@PathVariable Long idCategory,
			@RequestBody RestaurantCategoryDTO restaurantCategoryDto) {
		restaurantService.updateRestaurantCategory(idCategory, restaurantCategoryDto);
		return new GenericResponse("Category updated successfully");
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Enable restaurant", description = "Enables a restaurant by its primary email")
	@ApiResponse(responseCode = "200", description = "Restaurant enabled successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
	@ApiResponse(responseCode = "400", description = "Invalid request")
	@PutMapping("/{idRestaurant}/enableRestaurant")
	public GenericResponse enableRestaurant(@PathVariable Long idRestaurant) {
		restaurantService.enableRestaurant(idRestaurant);
		return new GenericResponse("Restaurant enabled successfully");
	}

	@Operation(summary = "Delete restaurant", description = "Deletes a restaurant by its ID")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@ApiResponse(responseCode = "200", description = "Restaurant deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
	@ApiResponse(responseCode = "400", description = "Invalid request")
	@PutMapping("/{idRestaurant}/deleteRestaurant")
	public GenericResponse deleteRestaurant(@PathVariable Long idRestaurant) {
		restaurantService.deleteRestaurant(idRestaurant);
		return new GenericResponse("Restaurant deleted successfully");
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Create restaurant", description = "Creates a new restaurant")
	@ApiResponse(responseCode = "200", description = "Restaurant created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
	@ApiResponse(responseCode = "400", description = "Invalid request")
	@PostMapping("/createRestaurant")
	public GenericResponse createRestaurant(@RequestBody RestaurantDTO restaurantDto) {
		restaurantService.createRestaurant(restaurantDto);
		return new GenericResponse("Restaurant created successfully");
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Change restaurant email", description = "Changes the email of a restaurant by its ID")
	@ApiResponse(responseCode = "200", description = "Restaurant email changed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
	@ApiResponse(responseCode = "400", description = "Invalid request")
	@PutMapping("/{idRestaurant}/changeEmail")
	public GenericResponse changeRestaurantEmail(@PathVariable Long idRestaurant, @RequestBody String newEmail) {
		restaurantService.changeRestaurantEmail(idRestaurant, newEmail);
		return new GenericResponse("Restaurant email changed successfully");
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Mark restaurant as deleted", description = "Marks a restaurant as deleted similar to disable by its ID")
	@ApiResponse(responseCode = "200", description = "Restaurant marked as deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
	@ApiResponse(responseCode = "400", description = "Invalid request")
	@DeleteMapping("/{idRestaurant}/markAsDeleted")
	public GenericResponse markRestaurantAsDeleted(@PathVariable Long idRestaurant, @RequestParam boolean deleted) {
		restaurantService.setRestaurantDeleted(idRestaurant, deleted);
		return new GenericResponse("Restaurant marked as deleted successfully");
	}

}
