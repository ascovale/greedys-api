package com.application.controller.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.application.service.ReservationService;
import com.application.web.dto.get.ReservationDTO;
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
	//TODO scrivere tutti i permessi che hanno gli admin e ruoli
	//TODO modificare tutti i preauthorize
	//TODO Modificare tutti i DTO
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
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
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
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
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
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
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
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
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
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
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
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	public ResponseEntity<?> deleteReservation(@PathVariable Long reservationId) {
		reservationService.adminDeleteReservation(reservationId);
		return ResponseEntity.ok().build();
	}

	/**
	 * Retrieves a reservation by its ID.
	 *
	 * @param reservationId the ID of the reservation
	 * @return ResponseEntity containing the reservation DTO
	 */
	@Operation(summary = "Get a reservation", description = "Endpoint to get a reservation by its ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Reservation retrieved successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid reservation ID", content = @Content),
		@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
		@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@GetMapping("/{reservationId}")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_READ')")
	public ResponseEntity<?> getReservation(@PathVariable Long reservationId) {
		ReservationDTO reservationDTO = reservationService.getReservation(reservationId);
		if (reservationDTO != null) {
		return ResponseEntity.ok(reservationDTO);
		} else {
		return ResponseEntity.status(404).body("Reservation not found");
		}
	}

	/**
	 * Retrieves all reservations for a specific customer by their ID.
	 *
	 * @param customerId the ID of the customer
	 * @return ResponseEntity containing the list of reservations
	 */
	@Operation(summary = "Get customer reservations", description = "Endpoint to get all reservations for a specific customer by their ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Reservations retrieved successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid customer ID", content = @Content),
		@ApiResponse(responseCode = "404", description = "Customer not found", content = @Content),
		@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@GetMapping("/customer/{customerId}")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_READ')")
	public ResponseEntity<?> getCustomerReservations(@PathVariable Long customerId) {
		List<ReservationDTO> reservations = reservationService.getCustomerReservations(customerId);
		if (reservations != null && !reservations.isEmpty()) {
			return ResponseEntity.ok(reservations);
		} else {
			return ResponseEntity.status(404).body("No reservations found for the customer");
		}
	}

	/**
	 * Retrieves paginated reservations for a specific customer by their ID.
	 *
	 * @param customerId the ID of the customer
	 * @param pageable the pagination information
	 * @return ResponseEntity containing the paginated list of reservations
	 */
	@Operation(summary = "Get paginated customer reservations", description = "Endpoint to get paginated reservations for a specific customer by their ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Reservations retrieved successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid customer ID", content = @Content),
		@ApiResponse(responseCode = "404", description = "Customer not found", content = @Content),
		@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@GetMapping("/customer/{customerId}/paginated")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_READ')")
	public ResponseEntity<?> getCustomerReservationsPaginated(@PathVariable Long customerId, Pageable pageable) {
		Page<ReservationDTO> reservations = reservationService.getCustomerReservationsPaginated(customerId, pageable);
		if (reservations != null && !reservations.isEmpty()) {
			return ResponseEntity.ok(reservations);
		} else {
			return ResponseEntity.status(404).body("No reservations found for the customer");
		}
	}
}
