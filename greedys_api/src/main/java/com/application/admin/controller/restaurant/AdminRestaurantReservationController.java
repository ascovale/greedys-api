package com.application.admin.controller.restaurant;

import java.time.LocalDate;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.admin.service.AdminReservationService;
import com.application.common.web.dto.get.ReservationDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/admin/restaurant")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Restaurant Reservations", description = "Admin Restaurant Reservation Operations")
@RequiredArgsConstructor
@Slf4j
public class AdminRestaurantReservationController {

	private final AdminReservationService adminReservationService;

	//TODO Questo dovrebbe ritornare anche un pageable
	@Operation(summary = "Get all reservations of a restaurant", description = "Retrieve all reservations of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{restaurantId}/reservation")
	public Collection<ReservationDTO> getReservations(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		Collection<ReservationDTO> reservations = adminReservationService.getReservations(restaurantId, start, end);
		return reservations;
	}

	@Operation(summary = "Get all accepted reservations of a restaurant", description = "Retrieve all accepted reservations of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{restaurantId}/reservation/accepted")
	public Collection<ReservationDTO> getAcceptedReservations(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		Collection<ReservationDTO> reservations = adminReservationService.getAcceptedReservations(restaurantId, start, end);
		return reservations;
	}

	@Operation(summary = "Get all reservations of a restaurant with pagination", description = "Retrieve all reservations of a restaurant with pagination")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{restaurantId}/reservation/pageable")
	public ResponseEntity<Page<ReservationDTO>> getReservationsPageable(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end,
			@RequestParam int page,
			@RequestParam int size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<ReservationDTO> reservations = adminReservationService.getReservationsPageable(restaurantId, start, end,
				pageable);
		return new ResponseEntity<>(reservations, HttpStatus.OK);
	}

	@Operation(summary = "Get all pending reservations of a restaurant", description = "Retrieve all pending reservations of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{restaurantId}/reservation/pending")
	public Collection<ReservationDTO> getPendingReservations(
			@PathVariable Long restaurantId,
			@RequestParam(required = false) LocalDate start,
			@RequestParam(required = false) LocalDate end) {

		Collection<ReservationDTO> reservations;

		reservations = adminReservationService.getPendingReservations(restaurantId, start, end);
		return reservations;
	}

	@Operation(summary = "Get all pending reservations of a restaurant with pagination", description = "Retrieve all pending reservations of a restaurant with pagination")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class)))),
		@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{restaurantId}/reservation/pending/pageable")
	public ResponseEntity<Page<ReservationDTO>> getPendingReservationsPageable(
		@PathVariable Long restaurantId,
		@RequestParam(required = false) LocalDate start,
		@RequestParam(required = false) LocalDate end,
		@RequestParam int page,
		@RequestParam int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<ReservationDTO> reservations;

		reservations = adminReservationService.getPendingReservationsPageable(restaurantId, start, end, pageable);

		return new ResponseEntity<>(reservations, HttpStatus.OK);
	}
}
