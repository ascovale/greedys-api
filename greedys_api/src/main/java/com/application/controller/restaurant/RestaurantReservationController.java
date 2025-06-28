package com.application.controller.restaurant;

import java.time.LocalDate;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.controller.utils.ControllerUtils;
import com.application.persistence.model.restaurant.user.RUser;
import com.application.service.ReservationService;
import com.application.web.dto.get.ReservationDTO;
import com.application.web.dto.post.restaurant.RestaurantNewReservationDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The ReservationRestaurantController class is responsible for handling HTTP
 * requests related to reservations for the restaurant.
 * It contains methods for creating, accepting, rejecting, marking as no-show,
 * and marking as seated reservations.
 */
@RestController
@SecurityRequirement(name = "restaurantBearerAuth")
@RequestMapping("/restaurant/reservation")
@Tag(name = "Restaurant Reservation", description = "APIs for managing reservations from the restaurant")
public class RestaurantReservationController {
	//
	private ReservationService reservationService;

	
	public RestaurantReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@Operation(summary = "Create a new reservation", description = "Endpoint to create a new reservation")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/new")
	@PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE')")
	public ResponseEntity<?> createReservation(@RequestBody RestaurantNewReservationDTO dto) {
		// TODO: rivedere i permessi va bene che controllo il ristorante
		// ma devo controllare enabled e che abbia il permesso di scrivere
		reservationService.createReservation(dto);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Accept a reservation", description = "Endpoint to accept a reservation by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation accepted successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid reservation ID", content = @Content),
			@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PutMapping("/{reservationId}/accept")
	@PreAuthorize("@securityRUserService.hasPermissionOnReservation(#reservationId)")
	public ResponseEntity<?> acceptReservation(@PathVariable Long reservationId, @RequestParam Boolean accepted) {
		reservationService.markReservationAcceptedFromRestauantUser(getCurrentRUserId(), reservationId,
				accepted);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Reject a reservation", description = "Endpoint to reject a reservation by its ID")
	@PutMapping("/{reservationId}/reject")
	public ResponseEntity<?> rejectReservation(@PathVariable Long reservationId, @RequestParam Boolean rejected) {
		reservationService.markReservationRejectedFromRestauantUser(getCurrentRUserId(), reservationId,
				rejected);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Mark a reservation as no show", description = "Endpoint to mark a reservation as no show by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation marked as no show successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid reservation ID", content = @Content),
			@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PutMapping("/{reservationId}/no_show")
	public ResponseEntity<?> markReservationNoShow(@PathVariable Long reservationId, @RequestParam Boolean noShow) {
		reservationService.markReservationNoShowFromRestauantUser(getCurrentRUserId(), reservationId, noShow);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Mark a reservation as seated", description = "Endpoint to mark a reservation as seated by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation marked as seated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid reservation ID", content = @Content),
			@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PutMapping("/{reservationId}/seated")
	public ResponseEntity<?> markReservationSeated(@PathVariable Long reservationId, @RequestParam Boolean seated) {
		reservationService.markReservationSeatedFromRestauantUser(getCurrentRUserId(), reservationId, seated);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Accept a reservation modification request", description = "Endpoint to accept a reservation modification request by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation modification request accepted successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid reservation ID", content = @Content),
			@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PutMapping("/{reservationId}/accept_modification")
	public ResponseEntity<?> acceptReservationModificationRequest(@PathVariable Long reservationId) {
		reservationService.restaurantAcceptReservatioModifyRequest(reservationId);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Get all reservations of a restaurant", description = "Retrieve all reservations of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")

	})
	@GetMapping(value = "/reservations")
	public Collection<ReservationDTO> getReservations(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		Collection<ReservationDTO> reservations = reservationService.getRestaurantReservations(start, end);
		return reservations;
	}

	@Operation(summary = "Get all accepted reservations of a restaurant", description = "Retrieve all accepted reservations of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@GetMapping(value = "/accepted/get")
	public Collection<ReservationDTO> getAcceptedReservations(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		Collection<ReservationDTO> reservations = reservationService.getAcceptedReservations(start, end);
		return reservations;
	}

	@Operation(summary = "Get all reservations of a restaurant with pagination", description = "Retrieve all reservations of a restaurant with pagination")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@GetMapping(value = "/pageable")
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

	@Operation(summary = "Get all pending reservations of a restaurant", description = "Retrieve all pending reservations of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@GetMapping(value = "/pending/get")
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
					.getPendingReservationsFromRUser(ControllerUtils.getCurrentRUser().getId());
		}
		return reservations;
	}

	private Long getCurrentRUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof RUser) {
			return ((RUser) authentication.getPrincipal()).getId();
		}
		return null;
	}

}
