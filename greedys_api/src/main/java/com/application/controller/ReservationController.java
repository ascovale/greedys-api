package com.application.controller;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.application.service.ReservationService;
import com.application.persistence.model.reservation.Reservation;
import com.application.web.dto.post.NewReservationDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.RequestBody;


/**
 * The ReservationController class is responsible for handling HTTP requests
 * related to reservations.
 * It contains methods for booking, creating reservations, retrieving
 * reservation lists, and managing the calendar.
 */
@Controller
@RequestMapping("/reservation")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reservation", description = "APIs for managing reservations")
public class ReservationController {

	@Autowired
	private ReservationService reservationService;

	@Autowired
    private ResourceLoader resourceLoader;

	/**
	 * Initializes the WebDataBinder to handle custom data binding for LocalDate objects.
	 * 
	 * @param binder The WebDataBinder object.
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(LocalDate.class, new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), true, 10));
	}

	@Operation(
    summary = "Create a new reservation",
    description = "Endpoint to create a new reservation"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Reservation created successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
		@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/")
	public ResponseEntity<?> reserveTable(
		@RequestBody NewReservationDTO DTO) {
		reservationService.createReservation(DTO);
		return ResponseEntity.ok().build();
	}

	@Operation(
	summary = "Ask for a reservation",
	description = "Endpoint to ask for a reservation"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Reservation requested successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
		@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/ask")
	public ResponseEntity<?> askReservation(
		@RequestBody NewReservationDTO DTO) {
		reservationService.askForReservation(DTO);
		return ResponseEntity.ok().build();
	}

	@Operation(
    summary = "Accept a reservation",
    description = "Endpoint to accept a reservation by its ID"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Reservation accepted successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid reservation ID", content = @Content),
		@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
		@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/confirm")
	public ResponseEntity<?> confirmReservation(@RequestParam Long reservation_id) {
		Reservation res = reservationService.findById(reservation_id);
		res.setAccepted(true);
		reservationService.save(res);
		return ResponseEntity.ok().build();
	}

	@Operation(
	summary = "get reservation script",
	description = "Endpoint to get the reservation script"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Reservation script retrieved successfully",
						content = @Content(mediaType = "application/javascript")),
		@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@GetMapping("/script") 
	public ResponseEntity<Resource> getScript(@RequestParam Long reservation_id) {
	    try {
            Resource resource = resourceLoader.getResource("classpath:static/reservation.js");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/javascript")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
