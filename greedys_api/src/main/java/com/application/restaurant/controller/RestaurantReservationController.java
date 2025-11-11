package com.application.restaurant.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.service.reservation.ReservationService;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.service.RestaurantNotificationService;
import com.application.restaurant.web.dto.reservation.RestaurantNewReservationDTO;
import com.application.restaurant.web.dto.reservation.RestaurantReservationWithExistingCustomerDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The ReservationRestaurantController class is responsible for handling HTTP
 * requests related to reservations for the restaurant.
 * It contains methods for creating, accepting, rejecting, marking as no-show,
 * and marking as seated reservations.
 */
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/restaurant/reservation")
@Tag(name = "Restaurant Reservation Management", description = "Controller for managing restaurant reservations")
@RequiredArgsConstructor
@Slf4j
public class RestaurantReservationController extends BaseController {
	private final ReservationService reservationService;
	private final RestaurantNotificationService restaurantNotificationService;

	//TODO: Aggiungere verifica che lo slot sia del ristorante
	@Operation(summary = "Create a new reservation", description = "Endpoint to create a new reservation")
	@CreateApiResponses
	@PostMapping("/new")
	@PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE') && @securityRUserService.isSlotOwnedByAuthenticatedUser(#dto.idSlot)")
	public ResponseEntity<ReservationDTO> createReservation(@RequestBody RestaurantNewReservationDTO dto,
			@AuthenticationPrincipal RUser rUser) {
		log.debug("Received reservation DTO: {}", dto);
		log.debug("DTO userName: {}, userEmail: {}, userPhoneNumber: {}, pax: {}, kids: {}, idSlot: {}, reservationDay: {}", 
			dto.getUserName(), dto.getUserEmail(), dto.getUserPhoneNumber(), dto.getPax(), dto.getKids(), dto.getIdSlot(), dto.getReservationDay());
		return executeCreate("create reservation", "Reservation created successfully", () -> {
			return reservationService.createReservation(dto, rUser.getRestaurant());
		});
	}

	//TODO: Aggiungere verifica che il customer sia nell'agenda del ristorante
	@Operation(summary = "Create a new reservation with existing customer", description = "Endpoint to create a new reservation using existing customer from agenda")
	@CreateApiResponses
	@PostMapping("/new-with-existing-customer")
	@PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE') && @securityRUserService.isSlotOwnedByAuthenticatedUser(#dto.idSlot)")
	public ResponseEntity<ReservationDTO> createReservationWithExistingCustomer(@RequestBody RestaurantReservationWithExistingCustomerDTO dto,
			@AuthenticationPrincipal RUser rUser) {
		log.debug("Received reservation DTO with existing customer: {}", dto);
		log.debug("DTO customerId: {}, userName: {}, pax: {}, kids: {}, idSlot: {}, reservationDay: {}", 
			dto.getCustomerId(), dto.getUserName(), dto.getPax(), dto.getKids(), dto.getIdSlot(), dto.getReservationDay());
		return executeCreate("create reservation with existing customer", "Reservation created successfully", () -> {
			return reservationService.createReservationWithExistingCustomer(dto, rUser.getRestaurant());
		});
	}

	@PutMapping("/{reservationId}/accept")
	@Operation(summary = "Accept a reservation", description = "Endpoint to accept a reservation by its ID")
	@ReadApiResponses
	@PreAuthorize("@securityRUserService.hasPermissionOnReservation(#reservationId)")
	public ResponseEntity<ReservationDTO> acceptReservation(@PathVariable Long reservationId) {
		return execute("accept reservation", () -> reservationService.setStatus(reservationId, Reservation.Status.ACCEPTED));
	}

	@PutMapping("/{reservationId}/reject")
	@Operation(summary = "Reject a reservation", description = "Endpoint to reject a reservation by its ID")
	@ReadApiResponses
	public ResponseEntity<ReservationDTO> rejectReservation(@PathVariable Long reservationId) {
		return execute("reject reservation", () -> reservationService.setStatus(reservationId, Reservation.Status.REJECTED));
	}

	@PutMapping("/{reservationId}/no_show")
	@Operation(summary = "Mark a reservation as no show", description = "Endpoint to mark a reservation as no show by its ID")
	@ReadApiResponses
	public ResponseEntity<ReservationDTO> markReservationNoShow(@PathVariable Long reservationId) {
		return execute("mark reservation no show", () -> reservationService.setStatus(reservationId, Reservation.Status.NO_SHOW));
	}

	@PutMapping("/{reservationId}/seated")
	@Operation(summary = "Mark a reservation as seated", description = "Endpoint to mark a reservation as seated by its ID")
	@ReadApiResponses
	public ResponseEntity<ReservationDTO> markReservationSeated(@PathVariable Long reservationId) {
		return execute("mark reservation seated", () -> reservationService.setStatus(reservationId, Reservation.Status.SEATED));
	}

	@Operation(summary = "Accept a reservation modification request", description = "Endpoint to accept a reservation modification request by its ID")
	@ReadApiResponses
	@PutMapping("/accept_modification/{modId}")
	public ResponseEntity<ReservationDTO> acceptReservationModificationRequest(@PathVariable Long modId) {
		return execute("accept reservation modification", () -> reservationService.AcceptReservatioModifyRequestAndReturnDTO(modId));
	}

	@Operation(summary = "Get all reservations of a restaurant", description = "Retrieve all reservations of a restaurant")
	@ReadApiResponses
	@GetMapping(value = "/reservations")
	
	public ResponseEntity<List<ReservationDTO>> getReservations(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
			@AuthenticationPrincipal RUser rUser) {
		return executeList("get reservations", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			Collection<ReservationDTO> reservations = reservationService.getReservations(restaurantId, start, end);
			return reservations instanceof List ? (List<ReservationDTO>) reservations
					: new java.util.ArrayList<>(reservations);
		});
	}

	@Operation(summary = "Get all accepted reservations of a restaurant", description = "Retrieve all accepted reservations of a restaurant")
	@ReadApiResponses
	@GetMapping(value = "/accepted/get")
	
	public ResponseEntity<List<ReservationDTO>> getAcceptedReservations(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
			@AuthenticationPrincipal RUser rUser) {
		return executeList("get accepted reservations", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			LocalDate startDate = start != null ? start.toLocalDate() : null;
			LocalDate endDate = end != null ? end.toLocalDate() : null;
			Collection<ReservationDTO> reservations = reservationService.getAcceptedReservations(restaurantId, startDate,
					endDate);
			return reservations instanceof List ? (List<ReservationDTO>) reservations
					: new java.util.ArrayList<>(reservations);
		});
	}

	@Operation(summary = "Get all reservations of a restaurant with pagination", description = "Retrieve all reservations of a restaurant with pagination")
	@ReadApiResponses
	@GetMapping(value = "/pageable")
	public ResponseEntity<Page<ReservationDTO>> getReservationsPageable(
			@AuthenticationPrincipal RUser rUser,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
			@RequestParam int page,
			@RequestParam int size) {
		return executePaginated("get reservations pageable", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			Pageable pageable = PageRequest.of(page, size);
			LocalDate startDate = start != null ? start.toLocalDate() : null;
			LocalDate endDate = end != null ? end.toLocalDate() : null;
			return reservationService.getReservationsPageable(restaurantId, startDate, endDate, pageable);
		});
	}

	@Operation(summary = "Get all pending reservations of a restaurant", description = "Retrieve all pending reservations of a restaurant with optional date filtering")
	@ReadApiResponses
	@GetMapping(value = "/pending/get")
	
	public ResponseEntity<List<ReservationDTO>> getPendingReservations(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
			@AuthenticationPrincipal RUser rUser) {
		return executeList("get pending reservations", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			Collection<ReservationDTO> reservations = reservationService.getPendingReservations(restaurantId, start,
					end);
			return reservations instanceof List ? (List<ReservationDTO>) reservations
					: new java.util.ArrayList<>(reservations);
		});
	}

	@Operation(summary = "Get all reservations of a customer in a restaurant", 
			description = "Retrieve all reservations for a specific customer in the authenticated restaurant")
	@ReadApiResponses
	@GetMapping(value = "/customer/{customerId}")
	@PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_READ')")
	public ResponseEntity<List<ReservationDTO>> getCustomerReservations(
			@PathVariable Long customerId,
			@AuthenticationPrincipal RUser rUser) {
		return executeList("get customer reservations", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			
			// Get reservations for the customer in this specific restaurant directly from database
			Collection<ReservationDTO> reservations = reservationService.getCustomerReservationsByRestaurant(customerId, restaurantId);
			return reservations instanceof List ? (List<ReservationDTO>) reservations
					: new java.util.ArrayList<>(reservations);
		});
	}

}

