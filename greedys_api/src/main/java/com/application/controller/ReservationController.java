package com.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.application.persistence.model.user.Notification;
import com.application.persistence.model.user.User;
import com.application.service.ReservationService;
import com.application.web.dto.post.NewReservationDTO;

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
@RequestMapping("/reservation/costumer")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reservation", description = "APIs for managing reservations")
public class ReservationController {
	@Autowired
	private ReservationService<Notification> reservationService;

	@Autowired
	private ResourceLoader resourceLoader;

	@Operation(summary = "The customer user ask for a reservation", description = "Endpoint to ask for a reservation")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation requested successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/ask")
	public ResponseEntity<?> askReservation(@RequestBody NewReservationDTO DTO) {
		// TODO: Rimuovere id user che non serve da dentro NewReservationDTO creando
		// NewReservationCustomerDTO
		reservationService.askForReservation(DTO, getCurrentUser());
		return ResponseEntity.ok().build();
	}

	private User getCurrentUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof User) {
			return ((User) principal);
		} else {
			System.out.println("Questo non dovrebbe succedere");
			return null;
		}
	}

	@Operation(summary = "The customer user cancels a reservation", description = "Endpoint to cancel a reservation")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Reservation cancelled successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
		@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/cancel")
	public ResponseEntity<?> cancelReservation(@RequestBody Long reservationId) {
		reservationService.cancelReservation(reservationId);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "The customer user requests a reservation modification", description = "Endpoint to request a reservation modification")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Reservation modification requested successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
		@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/modify")
	public ResponseEntity<?> requestModifyReservation(@RequestBody Long oldReservationId,@RequestBody NewReservationDTO DTO) {
		reservationService.requestModifyReservation(oldReservationId,DTO, getCurrentUser());
		return ResponseEntity.ok().build();
	}
	@Operation(summary = "The admin user accepts a reservation request", description = "Endpoint to accept a reservation request")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Reservation request accepted successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
		@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/accept")
	public ResponseEntity<?> acceptRequest(@RequestBody Long reservationRequestId) {
		reservationService.acceptReservationRequest(reservationRequestId, getCurrentUser());
		return ResponseEntity.ok().build();
	}
}
