package com.application.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.admin.persistence.model.Admin;
import com.application.admin.service.AdminReservationService;
import com.application.admin.web.dto.reservation.AdminNewReservationDTO;
import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.web.dto.reservations.ReservationDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/admin/reservation"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Reservation", description = "Admin Reservation Management")
@RequiredArgsConstructor
public class AdminReservationController extends BaseController {
	private final AdminReservationService adminReservationService;

	@Operation(summary = "Create a new reservation", description = "Endpoint to create a new reservation")
	
	@PostMapping("/new")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	@CreateApiResponses	public ResponseEntity<ReservationDTO> createReservation(
			@AuthenticationPrincipal Admin admin,
			@RequestBody AdminNewReservationDTO DTO) {
		return executeCreate("create reservation", () -> {
			return adminReservationService.createReservation(DTO, admin.getId());
		});
	}

	@PutMapping("/{reservationId}/accept")
	@Operation(summary = "Accept a reservation", description = "Endpoint to accept a reservation by its ID")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	public ResponseEntity<ReservationDTO> acceptReservation(
			@AuthenticationPrincipal Admin admin,
			@PathVariable Long reservationId) {
		return execute("accept reservation", () -> {
			return adminReservationService.updateReservationStatus(reservationId, Reservation.Status.ACCEPTED, admin.getId());
		});
	}

	@Operation(summary = "Reject a reservation", description = "Endpoint to reject a reservation by its ID")
	@PutMapping("/{reservationId}/reject")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	public ResponseEntity<ReservationDTO> rejectReservation(
			@AuthenticationPrincipal Admin admin,
			@PathVariable Long reservationId) {
		return execute("reject reservation", () -> {
			return adminReservationService.updateReservationStatus(reservationId, Reservation.Status.REJECTED, admin.getId());
		});
	}

	@Operation(summary = "Mark a reservation as no show", description = "Endpoint to mark a reservation as no show by its ID")
	@PutMapping("/{reservationId}/no_show")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	public ResponseEntity<ReservationDTO> markReservationNoShow(
			@AuthenticationPrincipal Admin admin,
			@PathVariable Long reservationId) {
		return execute("mark reservation no show", "Reservation marked as no show", () -> {
			return adminReservationService.markReservationNoShow(reservationId, admin.getId());
		});
	}

	@Operation(summary = "Mark a reservation as seated", description = "Endpoint to mark a reservation as seated by its ID")
	@PutMapping("/{reservationId}/seated")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	public ResponseEntity<ReservationDTO> markReservationSeated(
			@AuthenticationPrincipal Admin admin,
			@PathVariable Long reservationId) {
		return execute("mark reservation seated", "Reservation marked as seated", () -> {
			return adminReservationService.markReservationSeated(reservationId, admin.getId());
		});
	}

	@Operation(summary = "Delete a reservation", description = "Endpoint to delete a reservation by its ID")
	@PutMapping("/{reservationId}/delete")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	public ResponseEntity<ReservationDTO> deleteReservation(
			@AuthenticationPrincipal Admin admin,
			@PathVariable Long reservationId) {
		return execute("delete reservation", "Reservation deleted successfully", () -> {
			return adminReservationService.updateReservationStatus(reservationId, Reservation.Status.DELETED, admin.getId());
		});
	}

	@Operation(summary = "Get reservation by ID", description = "Endpoint to get a reservation by its ID")
	@GetMapping("/{reservationId}")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_READ')")
    public ResponseEntity<ReservationDTO> getReservation(@PathVariable Long reservationId) {
		return execute("get reservation", () -> {
			return adminReservationService.findReservationById(reservationId);
		});
	}

	@Operation(summary = "Modify a reservation", description = "Endpoint to modify an existing reservation")
	@PutMapping("/{reservationId}/modify")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
    public ResponseEntity<ReservationDTO> modifyReservation(
			@AuthenticationPrincipal Admin admin,
			@PathVariable Long reservationId, 
			@RequestBody AdminNewReservationDTO reservationDto) {
		return execute("modify reservation", () -> {
			return adminReservationService.modifyReservation(reservationId, reservationDto, admin.getId());
		});
	}

	@Operation(summary = "Fix old reservations - fill missing userName", description = "Update all reservations with NULL or empty userName with default value (Guest + ID)")
	@PostMapping("/fix/fill-username")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	public ResponseEntity<String> fixOldReservationsUsername() {
		return execute("fix old reservations username", "Reservations fixed successfully", () -> {
			return adminReservationService.fixMissingUsernames();
		});
	}
}

