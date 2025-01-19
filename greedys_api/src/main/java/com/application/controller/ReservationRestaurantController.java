package com.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.application.service.ReservationService;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.user.User;
import com.application.web.dto.post.NewReservationDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.Authentication;

/**
 * The ReservationRestaurantController class is responsible for handling HTTP requests
 * related to reservations for the restaurant.
 * It contains methods for booking, creating reservations, retrieving
 * reservation lists, and managing the calendar.
 */
@Controller
@RequestMapping("/reservation/restaurant")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reservation Restaurant", description = "APIs for managing reservations")
public class ReservationRestaurantController {

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private ResourceLoader resourceLoader;

	@Operation(summary = "Create a new reservation", description = "Endpoint to create a new reservation")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/")
	public ResponseEntity<?> createReservation(
			@RequestBody NewReservationDTO DTO) {
		reservationService.createReservation(DTO, getCurrentRestaurant());
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Accept a reservation", description = "Endpoint to accept a reservation by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation accepted successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid reservation ID", content = @Content),
			@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/accept")
	public ResponseEntity<?> acceptReservation(@RequestParam Long reservation_id) {
		Reservation res = reservationService.findById(reservation_id);
		res.setAccepted(true);
		reservationService.save(res);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Reject a reservation", description = "Endpoint to reject a reservation by its ID")
	@PostMapping("/reject")
	public ResponseEntity<?> rejectReservation(@RequestParam Long reservation_id) {
		Reservation res = reservationService.findById(reservation_id);
		res.setRejected(true);
		reservationService.save(res);
		return ResponseEntity.ok().build();
	}

	

	private Restaurant getCurrentRestaurant() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof User) {
			return ((User) principal).getRestaurantUser().getRestaurant();
		} else {
			System.out.println("Questo non dovrebbe succedere");
			return null;
		}
	}
}
