package com.application.controller.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.application.service.ReservationService;
import com.application.web.dto.post.customer.CustomerNewReservationDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The ReservationController class is responsible for handling HTTP requests
 * related to reservations.
 * It contains methods for booking, creating reservations, retrieving
 * reservation lists, and managing the calendar.
 */
@Controller
@RequestMapping("/customer/reservation")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reservation", description = "APIs for managing reservations")
public class ReservationController {
	@Autowired
	private ReservationService reservationService;
	
	@Operation(summary = "The customer user ask for a reservation", description = "Endpoint to ask for a reservation")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation requested successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/ask")
	public ResponseEntity<?> askReservation(@RequestBody CustomerNewReservationDTO DTO) {
		reservationService.askForReservation(DTO);
		return ResponseEntity.ok().build();
	}

	@PreAuthorize("@customerSecurityService.hasPermissionOnReservation(#reservationId)")
	@Operation(summary = "The customer user deletes a reservation", description = "Endpoint to delete a reservation")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Reservation deleted successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
		@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/delete")
	public ResponseEntity<?> deleteReservation(@RequestBody Long reservationId) {
		reservationService.customerDeleteReservation(reservationId);
		return ResponseEntity.ok().build();
	}

	@PreAuthorize("@customerSecurityService.hasPermissionOnReservation(#oldReservationId)")
	@Operation(summary = "The customer user requests a reservation modification", description = "Endpoint to request a reservation modification")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Reservation modification requested successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
		@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/request-modify")
	public ResponseEntity<?> requestModifyReservation(@RequestBody Long oldReservationId,@RequestBody CustomerNewReservationDTO DTO) {
		reservationService.requestModifyReservation(oldReservationId,DTO);
		return ResponseEntity.ok().build();
	}

	@PreAuthorize("@customerSecurityService.hasPermissionOnReservation(#reservationId)")
	@Operation(summary = "The customer user rejects a reservation", description = "Endpoint User reject a reservation created by the restaurant or admin")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Reservation rejected successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
		@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PutMapping("/reject")
	public ResponseEntity<?> rejectReservationCreatedByAdminOrRestaurant(@RequestBody Long reservationId) {
		reservationService.rejectReservationCreatedByAdminOrRestaurant(reservationId);
		return ResponseEntity.ok().build();
	}
}
