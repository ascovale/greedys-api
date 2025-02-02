package com.application.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.application.service.ReservationService;
import com.application.web.dto.post.admin.AdminNewReservationDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The ReservationRestaurantController class is responsible for handling HTTP
 * requests related to reservations for the restaurant.
 * It contains methods for creating, accepting, rejecting, marking as no-show, and marking as seated reservations.
 */
@Controller
@RequestMapping({"/admin/reservation"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reservation Admin", description = "APIs for managing reservations from the administrator")
public class AdminReservationController {

	private ReservationService reservationService;

	@Autowired
	public AdminReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	/**
	 * Creates a new reservation.
	 *
	 * @param idRestaurant the ID of the restaurant
	 * @param DTO the data transfer object containing reservation details
	 * @return ResponseEntity indicating the result of the operation
	 */
	@Operation(summary = "Create a new reservation", description = "Endpoint to create a new reservation")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/new_reservation")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> createReservation(@RequestBody AdminNewReservationDTO DTO) {
		reservationService.createAdminReservation(DTO);
		return ResponseEntity.ok().build();
	}

	/**
	 * Accepts a reservation by its ID.
	 *
	 * @param idRestaurant the ID of the restaurant
	 * @param reservationId the ID of the reservation
	 * @param accepted the acceptance status
	 * @return ResponseEntity indicating the result of the operation
	 */
	@Operation(summary = "Accept a reservation", description = "Endpoint to accept a reservation by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation accepted successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid reservation ID", content = @Content),
			@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PutMapping("/{reservationId}/accept")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> acceptReservation(@PathVariable Long reservationId, @RequestParam Boolean accepted) {
		reservationService.adminMarkReservationAccepted(reservationId, accepted);
		return ResponseEntity.ok().build();
	}

	/**
	 * Rejects a reservation by its ID.
	 *
	 * @param idRestaurant the ID of the restaurant
	 * @param reservationId the ID of the reservation
	 * @param rejected the rejection status
	 * @return ResponseEntity indicating the result of the operation
	 */
	@Operation(summary = "Reject a reservation", description = "Endpoint to reject a reservation by its ID")
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/{reservationId}/reject")
	public ResponseEntity<?> rejectReservation(@PathVariable Long reservationId, @RequestParam Boolean rejected) {
		reservationService.adminMarkReservationRejected(reservationId, rejected);
		return ResponseEntity.ok().build();
	}

	/**
	 * Marks a reservation as no show by its ID.
	 *
	 * @param idRestaurant the ID of the restaurant
	 * @param reservationId the ID of the reservation
	 * @param noShow the no-show status
	 * @return ResponseEntity indicating the result of the operation
	 */
	@Operation(summary = "Mark a reservation as no show", description = "Endpoint to mark a reservation as no show by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation marked as no show successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid reservation ID", content = @Content),
			@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PutMapping("/{reservationId}/no-show")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> markReservationNoShow(@PathVariable Long reservationId, @RequestParam Boolean noShow) {
		reservationService.adminMarkReservationNoShow(reservationId, noShow);
		return ResponseEntity.ok().build();
	}


	/**
	 * Marks a reservation as seated by its ID.
	 *
	 * @param idRestaurant the ID of the restaurant
	 * @param reservationId the ID of the reservation
	 * @param seated the seated status
	 * @return ResponseEntity indicating the result of the operation
	 */
	@Operation(summary = "Mark a reservation as seated", description = "Endpoint to mark a reservation as seated by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation marked as seated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid reservation ID", content = @Content),
			@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PutMapping("/{reservationId}/seated")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> markReservationSeated(@PathVariable Long reservationId, @RequestParam Boolean seated) {
		reservationService.adminMarkReservationSeated(reservationId, seated);
		return ResponseEntity.ok().build();
	}
	/**
	 * Delets a reservation by its ID.
	 *
	 * @param reservationId the ID of the reservation
	 * @return ResponseEntity indicating the result of the operation
	 */
	@Operation(summary = "Delete a reservation", description = "Endpoint to delete a reservation by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation deleted successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid reservation ID", content = @Content),
			@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PutMapping("/{reservationId}/delete")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deleteReservation(@PathVariable Long reservationId) {
		reservationService.adminDeleteReservation(reservationId);
		return ResponseEntity.ok().build();
	}
}
