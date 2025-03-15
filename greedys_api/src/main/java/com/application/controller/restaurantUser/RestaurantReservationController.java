package com.application.controller.restaurantUser;

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
import com.application.web.dto.post.restaurant.RestaurantNewReservationDTO;

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
@SecurityRequirement(name = "bearerAuth")
//@PreAuthorize("@securityService.isRestaurantUserPermission(#idRestaurantUser)")
@RequestMapping("/restaurant_user/reservation")
@Tag(name = "Reservation Restaurant", description = "APIs for managing reservations from the restaurant")
public class RestaurantReservationController {

	private ReservationService reservationService;

	@Autowired
	public RestaurantReservationController(ReservationService reservationService) {
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
	@PreAuthorize("@securityService.hasPermissionOnReservation(#idRestaurantUser, #DTO.reservationId)")
	public ResponseEntity<?> createReservation(@PathVariable Long idRestaurantUser, @RequestBody RestaurantNewReservationDTO dto) {
		reservationService.createRestaurantReservation(dto);
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
	@PreAuthorize("@securityService.hasPermissionOnReservation(#idRestaurantUser, #reservationId)")
	public ResponseEntity<?> acceptReservation(@PathVariable Long idRestaurantUser, @PathVariable Long reservationId, @RequestParam Boolean accepted) {
		reservationService.markReservationAcceptedFromRestauantUser(idRestaurantUser, reservationId, accepted);
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
	@PutMapping("/{reservationId}/reject")
	public ResponseEntity<?> rejectReservation(@PathVariable Long idRestaurantUser, @PathVariable Long reservationId, @RequestParam Boolean rejected) {
		reservationService.markReservationRejectedFromRestauantUser(idRestaurantUser, reservationId, rejected);
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
	public ResponseEntity<?> markReservationNoShow(@PathVariable Long idRestaurantUser, @PathVariable Long reservationId, @RequestParam Boolean noShow) {
		reservationService.markReservationNoShowFromRestauantUser(idRestaurantUser, reservationId, noShow);
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
	public ResponseEntity<?> markReservationSeated(@PathVariable Long idRestaurantUser, @PathVariable Long reservationId, @RequestParam Boolean seated) {
		reservationService.markReservationSeatedFromRestauantUser(idRestaurantUser, reservationId, seated);
		return ResponseEntity.ok().build();
	}
	
	/**
	 * Accepts a reservation modification request by its ID.
	 *
	 * @param reservationId the ID of the reservation
	 * @return ResponseEntity indicating the result of the operation
	 */
	@Operation(summary = "Accept a reservation modification request", description = "Endpoint to accept a reservation modification request by its ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Reservation modification request accepted successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid reservation ID", content = @Content),
		@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
		@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PutMapping("/{reservationId}/accept_modification")
	public ResponseEntity<?> acceptReservationModificationRequest( @PathVariable Long reservationId) {
		reservationService.restaurantAcceptReservatioModifyRequest(reservationId);
		return ResponseEntity.ok().build();
	}


}
