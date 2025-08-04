package com.application.admin.controller.restaurant;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.service.reservation.ReservationService;
import com.application.common.web.ListResponseWrapper;
import com.application.common.web.PageResponseWrapper;
import com.application.common.web.dto.reservations.ReservationDTO;

import io.swagger.v3.oas.annotations.Operation;
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
public class AdminRestaurantReservationController extends BaseController {

	private final ReservationService reservationService;

	@Operation(summary = "Get all reservations of a restaurant", description = "Retrieve all reservations of a restaurant")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{restaurantId}/reservation")
	@ReadApiResponses
	public ResponseEntity<ListResponseWrapper<ReservationDTO>> getReservations(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		return executeList("get reservations", () -> {
			Collection<ReservationDTO> reservations = reservationService.getReservations(restaurantId, start, end);
			return reservations instanceof List ? (List<ReservationDTO>) reservations : new java.util.ArrayList<>(reservations);
		});
	}

	@Operation(summary = "Get all accepted reservations of a restaurant", description = "Retrieve all accepted reservations of a restaurant")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{restaurantId}/reservation/accepted")
	@ReadApiResponses
	public ResponseEntity<ListResponseWrapper<ReservationDTO>> getAcceptedReservations(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		return executeList("get accepted reservations", () -> {
			Collection<ReservationDTO> reservations = reservationService.getAcceptedReservations(restaurantId, start, end);
			return reservations instanceof List ? (List<ReservationDTO>) reservations : new java.util.ArrayList<>(reservations);
		});
	}

	@Operation(summary = "Get all reservations of a restaurant with pagination", description = "Retrieve all reservations of a restaurant with pagination")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{restaurantId}/reservation/pageable")
	@ReadApiResponses
	public ResponseEntity<PageResponseWrapper<ReservationDTO>> getReservationsPageable(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end,
			@RequestParam int page,
			@RequestParam int size) {
		Pageable pageable = PageRequest.of(page, size);
		return executePaginated("get reservations pageable", () -> reservationService.getReservationsPageable(restaurantId, start, end, pageable));
	}

	@Operation(summary = "Get all pending reservations of a restaurant", description = "Retrieve all pending reservations of a restaurant")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{restaurantId}/reservation/pending")
	@ReadApiResponses
	public ResponseEntity<ListResponseWrapper<ReservationDTO>> getPendingReservations(
			@PathVariable Long restaurantId,
			@RequestParam(required = false) LocalDate start,
			@RequestParam(required = false) LocalDate end) {
		return executeList("get pending reservations", () -> {
			Collection<ReservationDTO> reservations = reservationService.getPendingReservations(restaurantId, start, end);
			return reservations instanceof List ? (List<ReservationDTO>) reservations : new java.util.ArrayList<>(reservations);
		});
	}

	@Operation(summary = "Get all pending reservations of a restaurant with pagination", description = "Retrieve all pending reservations of a restaurant with pagination")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{restaurantId}/reservation/pending/pageable")
	@ReadApiResponses
	public ResponseEntity<PageResponseWrapper<ReservationDTO>> getPendingReservationsPageable(
		@PathVariable Long restaurantId,
		@RequestParam(required = false) LocalDate start,
		@RequestParam(required = false) LocalDate end,
		@RequestParam int page,
		@RequestParam int size) {
		Pageable pageable = PageRequest.of(page, size);
		return executePaginated("get pending reservations pageable", () -> reservationService.getPendingReservationsPageable(restaurantId, start, end, pageable));
	}
}
