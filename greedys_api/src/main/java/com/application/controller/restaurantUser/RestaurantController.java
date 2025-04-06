package com.application.controller.restaurantUser;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.controller.utils.ControllerUtils;
import com.application.service.ReservationService;
import com.application.service.RestaurantService;
import com.application.service.RoomService;
import com.application.service.TableService;
import com.application.web.dto.get.ReservationDTO;
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
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Restaurant", description = "Controller per la gestione dei ristoranti")
@RestController
@RequestMapping("/restaurant")
// @PreAuthorize("@securityService.isRestaurantUserPermission(#idRestaurantUser)")
@SecurityRequirement(name = "restaurantBearerAuth")
public class RestaurantController {

	// Creare un tester che fa le richieste all'api e ne verifica la correttezza dei
	// risultati.

	private final RestaurantService restaurantService;
	private final ReservationService reservationService;
	private final RoomService roomService;
	private final TableService tableService;

	public RestaurantController(RestaurantService restaurantService, ReservationService reservationService,
			RoomService roomService,
			TableService tableService) {
		this.restaurantService = restaurantService;
		this.reservationService = reservationService;
		this.roomService = roomService;
		this.tableService = tableService;
	}

	@Operation(summary = "Get all reservations of a restaurant", description = "Ottieni tutte le prenotazioni di un ristorante", tags = {
			"restaurant-api" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Ristorante non trovato")

	})
	@GetMapping(value = "/reservations")
	public Collection<ReservationDTO> getReservations(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		Collection<ReservationDTO> reservations = reservationService.getRestaurantReservations(start, end);
		return reservations;
	}

	@Operation(summary = "Get all accepted reservations of a restaurant", description = "Ottieni tutte le prenotazioni accettate di un ristorante", tags = {
			"restaurant-api" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Ristorante non trovato")
	})
	@GetMapping(value = "/reservation/accepted")
	public Collection<ReservationDTO> getAcceptedReservations(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		Collection<ReservationDTO> reservations = reservationService
				.getAcceptedReservations(start, end);
		return reservations;
	}

	@Operation(summary = "Get all reservations of a restaurant with pagination", description = "Ottieni tutte le prenotazioni di un ristorante con paginazione", tags = {
			"restaurant-api" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Ristorante non trovato")
	})
	@GetMapping(value = "/reservation/pageable")
	public ResponseEntity<Page<ReservationDTO>> getReservationsPageable(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end,
			@RequestParam int page,
			@RequestParam int size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<ReservationDTO> reservations = reservationService
				.getReservationsPageable(start, end, pageable);
		return new ResponseEntity<>(reservations, HttpStatus.OK);
	}

	@Operation(summary = "Get all pending reservations of a restaurant", description = "Ottieni tutte le prenotazioni in attesa di un ristorante", tags = {
			"restaurant-api" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Ristorante non trovato")
	})
	@GetMapping(value = "/reservation/pending")
	public Collection<ReservationDTO> getPendingReservations(
			@RequestParam(required = false) LocalDate start,
			@RequestParam(required = false) LocalDate end) {

		Collection<ReservationDTO> reservations;
		if (end != null && start != null) {
			reservations = reservationService.getPendingReservations(start, end);
		} else if (start != null) {
			reservations = reservationService.getPendingReservations(start);
		} else if (end != null) {
			throw new IllegalArgumentException("end cannot be null if start is not null");
		} else {
			reservations = reservationService
					.getPendingReservationsFromRestaurantUser(ControllerUtils.getCurrentRestaurantUser().getId());
		}
		return reservations;
	}

	// questo metodo forse dovrebbe essere public tranne che questo può vedere anche
	// quelli disabilitati

	@GetMapping(value = "/services")
	@Operation(summary = "Get services of a restaurant", description = "Ottieni i servizi di un ristorante", tags = {
			"restaurant-api" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ServiceDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	public ResponseEntity<Collection<ServiceDTO>> getServices() {
		Collection<ServiceDTO> services = restaurantService.getServices(ControllerUtils.getCurrentRestaurant().getId());
		return new ResponseEntity<>(services, HttpStatus.OK);
	}

	/* -- === *** ROOMS AND TABLES *** === --- */

	@GetMapping(value = "/rooms")
	@Operation(summary = "Get rooms of a restaurant", description = "Ottieni le sale di un ristorante", tags = {
			"restaurant-api" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = RoomDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	public ResponseEntity<Collection<RoomDTO>> getRooms() {
		Collection<RoomDTO> rooms = roomService.findByRestaurant(ControllerUtils.getCurrentRestaurant().getId());
		return new ResponseEntity<>(rooms, HttpStatus.OK);
	}

	@GetMapping(value = "/room/{roomId}/tables")
	@Operation(summary = "Get tables of a room", description = "Ottieni i tavoli di una sala", tags = {
			"restaurant-api" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TableDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Ristorante o sala non trovato"),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	public ResponseEntity<Collection<TableDTO>> getTables(@PathVariable Long roomId) {
		// TODO: restituisce tutti i tavoli non di un ristorante specifico credo non lo
		// ho fatto io
		Collection<TableDTO> tables = tableService.findByRoom(roomId);
		return new ResponseEntity<>(tables, HttpStatus.OK);
	}

	@PostMapping(value = "/room")
	@Operation(summary = "Add a room to a restaurant", description = "Aggiungi una sala a un ristorante", tags = {
			"restaurant-api" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	public GenericResponse addRoom(@RequestBody NewRoomDTO roomDto) {
		// TODO: a quale ristorante associa la stanza
		roomService.createRoom(roomDto);
		return new GenericResponse("success");
	}

	@PostMapping(value = "/table")
	@Operation(summary = "Add a table to a room", description = "Aggiungi un tavolo a una sala", tags = {
			"restaurant-api" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Ristorante o sala non trovato"),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	public GenericResponse addTable(@RequestParam NewTableDTO tableDto) {
		// TODO: a quale ristorante associa il tavolo
		tableService.createTable(tableDto);
		return new GenericResponse("success");
	}

	@Operation(summary = "Set no show time limit", description = "Imposta il limite di tempo per il no show", tags = {
			"restaurant-api" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	@PostMapping(value = "/no_show_time_limit")
	public GenericResponse setNoShowTimeLimit(@RequestParam int minutes) {
		restaurantService.setNoShowTimeLimit(ControllerUtils.getCurrentRestaurant().getId(), minutes);
		return new GenericResponse("success");
	}

	@GetMapping(value = "/types")
	@Operation(summary = "Get types of a restaurant", description = "Ottieni i tipi di un ristorante", tags = {
			"restaurant-api" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = String.class)))),
			@ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	public ResponseEntity<Collection<String>> getRestaurantTypesNames() {
		List<String> types = restaurantService.getRestaurantTypesNames();
		return new ResponseEntity<>(types, HttpStatus.OK);
	}
}
