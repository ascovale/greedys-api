package com.application.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.admin.service.AdminReservationService;
import com.application.admin.web.dto.reservation.AdminNewReservationDTO;
import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.WrapperDataType;
import com.application.common.controller.annotation.WrapperType;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.service.reservation.ReservationService;
import com.application.common.web.ResponseWrapper;
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
	private final ReservationService reservationService;
	private final AdminReservationService adminReservationService;

	@Operation(summary = "Create a new reservation", description = "Endpoint to create a new reservation")
	
	@PostMapping("/new")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	@WrapperType(dataClass = ReservationDTO.class, type = WrapperDataType.DTO, responseCode = "201") 
	public ResponseEntity<ResponseWrapper<ReservationDTO>> createReservation(@RequestBody AdminNewReservationDTO DTO) {
		return executeCreate("create reservation", () -> {
			return adminReservationService.createReservation(DTO);
		});
	}

	@PutMapping("/{reservationId}/accept")
	@Operation(summary = "Accept a reservation", description = "Endpoint to accept a reservation by its ID")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	public ResponseEntity<ResponseWrapper<String>> acceptReservation(@PathVariable Long reservationId) {
		return executeVoid("accept reservation", "Reservation accepted successfully", () -> {
			reservationService.setStatus(reservationId, Reservation.Status.ACCEPTED);
		});
	}

	@Operation(summary = "Reject a reservation", description = "Endpoint to reject a reservation by its ID")
	@PutMapping("/{reservationId}/reject")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	public ResponseEntity<ResponseWrapper<String>> rejectReservation(@PathVariable Long reservationId) {
		return executeVoid("reject reservation", "Reservation rejected successfully", () -> {
			reservationService.setStatus(reservationId, Reservation.Status.REJECTED);
		});
	}

	@Operation(summary = "Mark a reservation as no show", description = "Endpoint to mark a reservation as no show by its ID")
	@PutMapping("/{reservationId}/no_show")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	public ResponseEntity<ResponseWrapper<String>> markReservationNoShow(@PathVariable Long reservationId) {
		return executeVoid("mark reservation no show", "Reservation marked as no show", () -> {
			reservationService.setStatus(reservationId, Reservation.Status.NO_SHOW);
		});
	}

	@Operation(summary = "Mark a reservation as seated", description = "Endpoint to mark a reservation as seated by its ID")
	@PutMapping("/{reservationId}/seated")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	public ResponseEntity<ResponseWrapper<String>> markReservationSeated(@PathVariable Long reservationId) {
		return executeVoid("mark reservation seated", "Reservation marked as seated", () -> {
			reservationService.setStatus(reservationId, Reservation.Status.SEATED);
		});
	}

	@Operation(summary = "Delete a reservation", description = "Endpoint to delete a reservation by its ID")
	@PutMapping("/{reservationId}/delete")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	public ResponseEntity<ResponseWrapper<String>> deleteReservation(@PathVariable Long reservationId) {
		return executeVoid("delete reservation", "Reservation deleted successfully", () -> {
			reservationService.setStatus(reservationId, Reservation.Status.DELETED);
		});
	}

	@Operation(summary = "Get reservation by ID", description = "Endpoint to get a reservation by its ID")
	@GetMapping("/{reservationId}")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_READ')")
	@WrapperType(dataClass = ReservationDTO.class, type = WrapperDataType.DTO)
    public ResponseEntity<ResponseWrapper<ReservationDTO>> getReservation(@PathVariable Long reservationId) {
		return execute("get reservation", () -> {
			return adminReservationService.findReservationById(reservationId);
		});
	}

	@Operation(summary = "Modify a reservation", description = "Endpoint to modify an existing reservation")
	@PutMapping("/{reservationId}/modify")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE')")
	@WrapperType(dataClass = ReservationDTO.class, type = WrapperDataType.DTO)
    public ResponseEntity<ResponseWrapper<ReservationDTO>> modifyReservation(@PathVariable Long reservationId, @RequestBody AdminNewReservationDTO reservationDto) {
		return execute("modify reservation", () -> {
			return adminReservationService.modifyReservation(reservationId, reservationDto);
		});
	}
}
