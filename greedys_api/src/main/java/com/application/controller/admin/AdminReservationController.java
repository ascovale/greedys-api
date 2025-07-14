package com.application.controller.admin;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.reservation.Reservation;
import com.application.service.reservation.AdminReservationService;
import com.application.web.dto.get.ReservationDTO;
import com.application.web.dto.post.admin.AdminNewReservationDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping({"/admin/reservation"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Reservation", description = "Admin Reservation Management")
public class AdminReservationController {
	private final AdminReservationService adminReservationService;

	public AdminReservationController(AdminReservationService adminReservationService) {
		this.adminReservationService = adminReservationService;
	}

	@Operation(summary = "Create a new reservation", description = "Endpoint to create a new reservation")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/new")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	public ResponseEntity<?> createReservation(@RequestBody AdminNewReservationDTO DTO) {
		adminReservationService.createReservation(DTO);
		return ResponseEntity.ok().build();
	}

	@PutMapping("/{reservationId}/accept")
	@Operation(summary = "Accept a reservation", description = "Endpoint to accept a reservation by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation accepted successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid reservation ID", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
			@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	public ResponseEntity<?> acceptReservation(@PathVariable Long reservationId) {
		adminReservationService.setStatus(reservationId, Reservation.Status.ACCEPTED);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Reject a reservation", description = "Endpoint to reject a reservation by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation rejected successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid reservation ID", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
			@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PutMapping("/{reservationId}/reject")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	public ResponseEntity<?> rejectReservation(@PathVariable Long reservationId) {
		adminReservationService.setStatus(reservationId, Reservation.Status.REJECTED);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Mark a reservation as no show", description = "Endpoint to mark a reservation as no show by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation marked as no show successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid reservation ID", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
			@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PutMapping("/{reservationId}/no_show")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	public ResponseEntity<?> markReservationNoShow(@PathVariable Long reservationId) {
		adminReservationService.setStatus(reservationId, Reservation.Status.NO_SHOW);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Mark a reservation as seated", description = "Endpoint to mark a reservation as seated by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation marked as seated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid reservation ID", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
			@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PutMapping("/{reservationId}/seated")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	public ResponseEntity<?> markReservationSeated(@PathVariable Long reservationId) {
		adminReservationService.setStatus(reservationId, Reservation.Status.SEATED);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Delete a reservation", description = "Endpoint to delete a reservation by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation deleted successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid reservation ID", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
			@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PutMapping("/{reservationId}/delete")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	public ResponseEntity<?> deleteReservation(@PathVariable Long reservationId) {
		adminReservationService.setStatus(reservationId, Reservation.Status.DELETED);
		return ResponseEntity.ok().build();
	}

}
