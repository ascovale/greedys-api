package com.application.agency.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.agency.persistence.model.user.AgencyUser;
import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.service.reservation.ReservationService;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.web.dto.reservation.RestaurantNewReservationDTO;
import com.application.restaurant.web.dto.reservation.RestaurantReservationWithExistingCustomerDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for managing agency reservations.
 * Allows travel agencies to create and manage group reservations for restaurants.
 */
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/agency/reservation")
@Tag(name = "Agency Reservation Management", description = "Controller for managing agency reservations")
@RequiredArgsConstructor
@Slf4j
public class AgencyReservationController extends BaseController {
	private final ReservationService reservationService;
	private final RestaurantDAO restaurantDAO;

	/**
	 * Create a new reservation for a restaurant through an agency.
	 * Note: Agency must specify restaurantId in the path.
	 */
	@Operation(summary = "Create a new reservation for a restaurant", description = "Endpoint to create a new reservation through agency for a specific restaurant")
	@CreateApiResponses
	@PostMapping("/restaurant/{restaurantId}/new")
	@PreAuthorize("hasAuthority('PRIVILEGE_AGENCY_USER_RESERVATION_WRITE')")
	public ResponseEntity<ReservationDTO> createReservation(
			@PathVariable Long restaurantId,
			@RequestBody RestaurantNewReservationDTO dto,
			@AuthenticationPrincipal AgencyUser agencyUser) {
		log.debug("Received reservation DTO from agency for restaurant {}: {}", restaurantId, dto);
		return executeCreate("create reservation", "Reservation created successfully", () -> {
			Restaurant restaurant = restaurantDAO.findById(restaurantId)
				.orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));
			return reservationService.createReservation(dto, restaurant);
		});
	}

	/**
	 * Create a new reservation with existing customer
	 */
	@Operation(summary = "Create a new reservation with existing customer", description = "Endpoint to create a new reservation using existing customer from agenda")
	@CreateApiResponses
	@PostMapping("/restaurant/{restaurantId}/new-with-existing-customer")
	@PreAuthorize("hasAuthority('PRIVILEGE_AGENCY_USER_RESERVATION_WRITE')")
	public ResponseEntity<ReservationDTO> createReservationWithExistingCustomer(
			@PathVariable Long restaurantId,
			@RequestBody RestaurantReservationWithExistingCustomerDTO dto,
			@AuthenticationPrincipal AgencyUser agencyUser) {
		log.debug("Received reservation DTO with existing customer from agency for restaurant {}: {}", restaurantId, dto);
		return executeCreate("create reservation with existing customer", "Reservation created successfully", () -> {
			Restaurant restaurant = restaurantDAO.findById(restaurantId)
				.orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));
			return reservationService.createReservationWithExistingCustomer(dto, restaurant);
		});
	}

	/**
	 * Accept a reservation
	 */
	@PutMapping("/{reservationId}/accept")
	@Operation(summary = "Accept a reservation", description = "Endpoint to accept a reservation by its ID")
	@ReadApiResponses
	@PreAuthorize("hasAuthority('PRIVILEGE_AGENCY_USER_RESERVATION_WRITE')")
	public ResponseEntity<ReservationDTO> acceptReservation(@PathVariable Long reservationId) {
		return execute("accept reservation", () -> reservationService.setStatus(reservationId, Reservation.Status.ACCEPTED));
	}

	/**
	 * Reject a reservation
	 */
	@PutMapping("/{reservationId}/reject")
	@Operation(summary = "Reject a reservation", description = "Endpoint to reject a reservation by its ID")
	@ReadApiResponses
	@PreAuthorize("hasAuthority('PRIVILEGE_AGENCY_USER_RESERVATION_WRITE')")
	public ResponseEntity<ReservationDTO> rejectReservation(@PathVariable Long reservationId) {
		return execute("reject reservation", () -> reservationService.setStatus(reservationId, Reservation.Status.REJECTED));
	}

	/**
	 * Mark a reservation as no-show
	 */
	@PutMapping("/{reservationId}/no_show")
	@Operation(summary = "Mark a reservation as no show", description = "Endpoint to mark a reservation as no show by its ID")
	@ReadApiResponses
	@PreAuthorize("hasAuthority('PRIVILEGE_AGENCY_USER_RESERVATION_WRITE')")
	public ResponseEntity<ReservationDTO> markReservationNoShow(@PathVariable Long reservationId) {
		return execute("mark reservation no show", () -> reservationService.setStatus(reservationId, Reservation.Status.NO_SHOW));
	}

	/**
	 * Mark a reservation as seated
	 */
	@PutMapping("/{reservationId}/seated")
	@Operation(summary = "Mark a reservation as seated", description = "Endpoint to mark a reservation as seated by its ID")
	@ReadApiResponses
	@PreAuthorize("hasAuthority('PRIVILEGE_AGENCY_USER_RESERVATION_WRITE')")
	public ResponseEntity<ReservationDTO> markReservationSeated(@PathVariable Long reservationId) {
		return execute("mark reservation seated", () -> reservationService.setStatus(reservationId, Reservation.Status.SEATED));
	}

}
