package com.application.admin.controller.restaurant;

import java.time.LocalDate;
import java.util.Collection;

import org.springframework.data.domain.Page;
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
import com.application.common.service.reservation.ReservationService;
import com.application.common.web.dto.ApiResponse;
import com.application.common.web.dto.get.ReservationDTO;

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
	public ResponseEntity<ApiResponse<Collection<ReservationDTO>>> getReservations(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		return execute("get reservations", () -> reservationService.getReservations(restaurantId, start, end));
	}

	@Operation(summary = "Get all accepted reservations of a restaurant", description = "Retrieve all accepted reservations of a restaurant")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{restaurantId}/reservation/accepted")
	public ResponseEntity<ApiResponse<Collection<ReservationDTO>>> getAcceptedReservations(
			@PathVariable Long restaurantId,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
		return execute("get accepted reservations", () -> reservationService.getAcceptedReservations(restaurantId, start, end));
	}

	@Operation(summary = "Get all reservations of a restaurant with pagination", description = "Retrieve all reservations of a restaurant with pagination")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{restaurantId}/reservation/pageable")
	public ResponseEntity<ApiResponse<Page<ReservationDTO>>> getReservationsPageable(
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
	public ResponseEntity<ApiResponse<Collection<ReservationDTO>>> getPendingReservations(
			@PathVariable Long restaurantId,
			@RequestParam(required = false) LocalDate start,
			@RequestParam(required = false) LocalDate end) {
		return execute("get pending reservations", () -> reservationService.getPendingReservations(restaurantId, start, end));
	}

	@Operation(summary = "Get all pending reservations of a restaurant with pagination", description = "Retrieve all pending reservations of a restaurant with pagination")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ')")
	@GetMapping(value = "{restaurantId}/reservation/pending/pageable")
	public ResponseEntity<ApiResponse<Page<ReservationDTO>>> getPendingReservationsPageable(
		@PathVariable Long restaurantId,
		@RequestParam(required = false) LocalDate start,
		@RequestParam(required = false) LocalDate end,
		@RequestParam int page,
		@RequestParam int size) {
		Pageable pageable = PageRequest.of(page, size);
		return executePaginated("get pending reservations pageable", () -> reservationService.getPendingReservationsPageable(restaurantId, start, end, pageable));
	}
}
