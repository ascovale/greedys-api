package com.application.controller.restaurant;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.restaurant.RestaurantCategory;
import com.application.service.ReservationService;
import com.application.service.RestaurantService;
import com.application.service.RestaurantUserService;
import com.application.service.RoomService;
import com.application.service.TableService;
import com.application.web.dto.get.ReservationDTO;
import com.application.web.dto.get.RestaurantDTO;
import com.application.web.dto.get.RestaurantUserDTO;
import com.application.web.dto.get.RoomDTO;
import com.application.web.dto.get.ServiceDTO;
import com.application.web.dto.get.SlotDTO;
import com.application.web.dto.get.TableDTO;
import com.application.web.dto.post.NewRoomDTO;
import com.application.web.dto.post.NewTableDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.ArraySchema;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Restaurant", description = "Controller per la gestione dei ristoranti")
@RestController
@RequestMapping("/restaurant/")
@SecurityRequirement(name = "bearerAuth")
public class RestaurantController {

	private final RestaurantService restaurantService;
	private final ReservationService reservationService;
	private final RestaurantUserService restaurantUserService;
	private final RoomService roomService;
	private final TableService tableService;

	public RestaurantController(RestaurantService restaurantService, ReservationService reservationService, 
								RestaurantUserService restaurantUserService, RoomService roomService, 
								TableService tableService) {
		this.restaurantService = restaurantService;
		this.reservationService = reservationService;
		this.restaurantUserService = restaurantUserService;
		this.roomService = roomService;
		this.tableService = tableService;
	}


	/*
	@Autowired
	private ApplicationEventPublisher eventPublisher;*/

	@Operation(summary = "Get all restaurants", description = "Ottieni tutti i ristoranti")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Operazione riuscita", 
                     content = @Content(mediaType = "application/json",
					 				array = @ArraySchema(
                                        schema = @Schema(implementation = RestaurantDTO.class)))),
        @ApiResponse(responseCode = "500", description = "Errore interno del server")
    })
    @GetMapping(value = "")
	public ResponseEntity<Collection<RestaurantDTO>> getRestaurants() {
		Collection<RestaurantDTO> restaurants = restaurantService.findAll().stream().map(r -> new RestaurantDTO(r)).toList();
		return new ResponseEntity<>(restaurants, HttpStatus.OK);
	}

	@GetMapping(value = "{id}/open-days")
	@Operation(summary = "Get open days of a restaurant", description = "Ottieni i giorni di apertura di un ristorante")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operazione riuscita",
						content = @Content(mediaType = "application/json",
									array = @ArraySchema(
											schema = @Schema(implementation = LocalDate.class)))),
		@ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
		@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	public ResponseEntity<Collection<String>> getOpenDays(
				@PathVariable Long id,
				@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
				@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end){
		Collection<String> openDays = restaurantService.getOpenDays(id, start, end);
		return new ResponseEntity<>(openDays, HttpStatus.OK);
	}

	
	@GetMapping(value = "{id}/closed-days")
	@Operation(summary = "Get closed days of a restaurant", description = "Ottieni i giorni di chiusura di un ristorante")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operazione riuscita",
						content = @Content(mediaType = "application/json",
									array = @ArraySchema(
											schema = @Schema(implementation = LocalDate.class)))),
		@ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
		@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	public ResponseEntity<Collection<LocalDate>> getClosedDays(
				@PathVariable Long id,
				@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
				@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end){
		Collection<LocalDate> openDays = restaurantService.getClosedDays(id, start, end);
		return new ResponseEntity<>(openDays, HttpStatus.OK);
	}

	@GetMapping(value = "{id}/day-slots")
	@Operation(summary = "Get day slots of a restaurant", description = "Ottieni gli slot giornalieri di un ristorante")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operazione riuscita",
						content = @Content(mediaType = "application/json",
									array = @ArraySchema(
											schema = @Schema(implementation = SlotDTO.class)))),
		@ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
		@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	public ResponseEntity<Collection<SlotDTO>> getDaySlots(
				@PathVariable Long id,
				@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate date){
		Collection<SlotDTO> slots = restaurantService.getDaySlots(id, date);
		return new ResponseEntity<>(slots, HttpStatus.OK);
	}

	@Operation(summary = "Get all reservations of a restaurant", description = "Ottieni tutte le prenotazioni di un ristorante")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Operazione riuscita", 
                     content = @Content(mediaType = "application/json",
					 				array = @ArraySchema(
                                        schema = @Schema(implementation = ReservationDTO.class)))),
        @ApiResponse(responseCode = "404", description = "Ristorante non trovato")
    })
    @GetMapping(value = "{id}/reservation")
	public Collection<ReservationDTO> getReservations(
				@PathVariable Long id,
				@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
				@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end){
		Collection<ReservationDTO> reservations = reservationService.getReservations(id, start, end);
		return reservations;
	}

	@Operation(summary = "Get all accepted reservations of a restaurant", description = "Ottieni tutte le prenotazioni accettate di un ristorante")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operazione riuscita", 
					 content = @Content(mediaType = "application/json",
					 				array = @ArraySchema(
										schema = @Schema(implementation = ReservationDTO.class)))),
		@ApiResponse(responseCode = "404", description = "Ristorante non trovato")
	})
	@GetMapping(value = "{id}/reservation/accepted")
	public Collection<ReservationDTO> getAcceptedReservations(
				@PathVariable Long id,
				@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
				@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end){
		Collection<ReservationDTO> reservations = reservationService.getAcceptedReservations(id, start, end);
		return reservations;
	}

	@Operation(summary = "Get all pending reservations of a restaurant", description = "Ottieni tutte le prenotazioni in attesa di un ristorante")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operazione riuscita", 
					 content = @Content(mediaType = "application/json",
					 				array = @ArraySchema(
										schema = @Schema(implementation = ReservationDTO.class)))),
		@ApiResponse(responseCode = "404", description = "Ristorante non trovato")
	})
	@GetMapping(value = "{id}/reservation/pending")
	public Collection<ReservationDTO> getPendingReservations(
				@PathVariable Long id,
				@RequestParam(required = false) LocalDate start,
				@RequestParam(required = false) LocalDate end) {

				Collection<ReservationDTO> reservations;
				if(end != null && start != null){
					reservations = reservationService.getPendingReservations(id, start, end);
				}
				else if(start != null){
					reservations = reservationService.getPendingReservations(id, start);
				}
				else if (end != null){
					throw new IllegalArgumentException("end cannot be null if start is not null");

				}
				else {
					reservations = reservationService.getPendingReservations(id);

				}
				return reservations;
	}

	@Operation(summary = "Get all users of a restaurant", description = "Ottieni tutti gli utenti di un ristorante")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Operazione riuscita", 
                     content = @Content(mediaType = "application/json",
					 				array = @ArraySchema(
                                        schema = @Schema(implementation = RestaurantUserDTO.class)))),
        @ApiResponse(responseCode = "404", description = "Ristorante non trovato")
    })
    @GetMapping(value = "{id}/user")
	public ResponseEntity<Collection<RestaurantUserDTO>> getRestaurantUsers(@PathVariable Long id) {
		Collection<RestaurantUserDTO> users = restaurantUserService.getRestaurantUsers(id);
		return new ResponseEntity<Collection<RestaurantUserDTO>>(users, HttpStatus.OK);
	
	}

	@Operation(summary = "Accept a user", description = "Accetta un utente per un ristorante specifico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Operazione riuscita", 
                     content = @Content(mediaType = "application/json", 
                                        schema = @Schema(implementation = GenericResponse.class))),
        @ApiResponse(responseCode = "404", description = "Ristorante o utente non trovato")
    })
    @PostMapping("{id}/user/accept")
    public GenericResponse acceptUser(@PathVariable Long id) {
		// TODO: implementare la verifica che l'utente sia effettivamente un utente del ristorante
        restaurantUserService.acceptUser(id);

        return new GenericResponse("success");
    }
	
/* 
	@Async
	private void asyncTmp(HttpServletRequest request, User registered) {
		eventPublisher
				.publishEvent(
						new OnRegistrationCompleteEvent(registered, request.getLocale(), getAppUrl(request)));

	}*/

	@RequestMapping(value = "/secured/agenda", method = RequestMethod.GET)
	public String agenda() {
		return "agenda";
	} 

	@Operation(summary = "Search restaurants by name", description = "Cerca ristoranti per nome")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Operazione riuscita", 
                     content = @Content(mediaType = "application/json",
					 				array = @ArraySchema(
                                        schema = @Schema(implementation = RestaurantDTO.class)))),
        @ApiResponse(responseCode = "404", description = "Ristorante non trovato")
    })
    @GetMapping(value = "/search")
	public ResponseEntity<Collection<RestaurantDTO>> searchRestaurants(@RequestParam String name) {
		Collection<RestaurantDTO> restaurants = restaurantService.findBySearchTerm(name);
		return new ResponseEntity<>(restaurants, HttpStatus.OK);
	}

	@GetMapping(value = "/{id}/services")
	@Operation(summary = "Get services of a restaurant", description = "Ottieni i servizi di un ristorante")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operazione riuscita",
						content = @Content(mediaType = "application/json",
									array = @ArraySchema(
											schema = @Schema(implementation = ServiceDTO.class)))),
		@ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
		@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	public ResponseEntity<Collection<ServiceDTO>> getServices(@PathVariable Long id){
		Collection<ServiceDTO> services = restaurantService.getServices(id);
		return new ResponseEntity<>(services, HttpStatus.OK);
	}

	/* -- === *** ROOMS AND TABLES *** === --- */

	@GetMapping(value = "/{id}/rooms")
	@Operation(summary = "Get rooms of a restaurant", description = "Ottieni le sale di un ristorante")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operazione riuscita",
						content = @Content(mediaType = "application/json",
									array = @ArraySchema(
											schema = @Schema(implementation = RoomDTO.class)))),
		@ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
		@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	public ResponseEntity<Collection<RoomDTO>> getRooms(@PathVariable Long id){
		Collection<RoomDTO> rooms = roomService.findByRestaurant(id);
		return new ResponseEntity<>(rooms, HttpStatus.OK);
	}

	@GetMapping(value = "/{id}/room/{roomId}/tables")
	@Operation(summary = "Get tables of a room", description = "Ottieni i tavoli di una sala")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operazione riuscita",
						content = @Content(mediaType = "application/json",
									array = @ArraySchema(
											schema = @Schema(implementation = TableDTO.class)))),
		@ApiResponse(responseCode = "404", description = "Ristorante o sala non trovato"),
		@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	public ResponseEntity<Collection<TableDTO>> getTables(@PathVariable Long id, @PathVariable Long roomId){
		Collection<TableDTO> tables = tableService.findByRoom(roomId);
		return new ResponseEntity<>(tables, HttpStatus.OK);
	}

	@PostMapping(value = "/room")
	@Operation(summary = "Add a room to a restaurant", description = "Aggiungi una sala a un ristorante")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operazione riuscita",
						content = @Content(mediaType = "application/json",
									schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
		@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	public GenericResponse addRoom(@RequestBody NewRoomDTO roomDto){
		roomService.createRoom(roomDto);
		return new GenericResponse("success");
	}

	@PostMapping(value = "/table")
	@Operation(summary = "Add a table to a room", description = "Aggiungi un tavolo a una sala")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operazione riuscita",
						content = @Content(mediaType = "application/json",
									schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "404", description = "Ristorante o sala non trovato"),
		@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	public GenericResponse addTable(@RequestParam NewTableDTO tableDto){
		tableService.createTable(tableDto);
		return new GenericResponse("success");
	}

	@Operation(summary = "Set now show time limit", description = "Imposta il limite di tempo per il no show")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operazione riuscita",
					 content = @Content(mediaType = "application/json",
										schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
		@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	@PostMapping(value = "{id}/now-show-time-limit")
	public GenericResponse setNowShowTimeLimit(@PathVariable Long id, @RequestParam int minutes) {
		restaurantService.setNowShowTimeLimit(id, minutes);
		return new GenericResponse("success");
	}

	@GetMapping(value = "{id}/types")
	@Operation(summary = "Get types of a restaurant", description = "Ottieni i tipi di un ristorante")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operazione riuscita",
					 content = @Content(mediaType = "application/json",
								array = @ArraySchema(
										schema = @Schema(implementation = String.class)))),
		@ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
		@ApiResponse(responseCode = "400", description = "Richiesta non valida")
	})
	public ResponseEntity<Collection<String>> getRestaurantTypesNames(@PathVariable Long id) {
		List<String> types = restaurantService.getRestaurantTypesNames(id);
		return new ResponseEntity<>(types, HttpStatus.OK);
	}
}
