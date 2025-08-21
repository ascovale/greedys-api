package com.application.restaurant.controller;

import java.time.LocalDate;
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
import com.application.common.controller.annotation.WrapperDataType;
import com.application.common.controller.annotation.WrapperType;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.service.reservation.ReservationService;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.service.RestaurantNotificationService;
import com.application.restaurant.web.dto.reservation.RestaurantNewReservationDTO;

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
	@PostMapping("/new")
	@PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE') && @securityRUserService.isSlotOwnedByAuthenticatedUser(#dto.idSlot)")
	@WrapperType(dataClass = ReservationDTO.class, type = WrapperDataType.DTO, responseCode = "201")
	public ResponseEntity<ResponseWrapper<ReservationDTO>> createReservation(@RequestBody RestaurantNewReservationDTO dto,
			@AuthenticationPrincipal RUser rUser) {
		log.debug("Received reservation DTO: {}", dto);
		log.debug("DTO userName: {}, userEmail: {}, userPhoneNumber: {}, pax: {}, kids: {}, idSlot: {}, reservationDay: {}", 
			dto.getUserName(), dto.getUserEmail(), dto.getUserPhoneNumber(), dto.getPax(), dto.getKids(), dto.getIdSlot(), dto.getReservationDay());
		return executeCreate("create reservation", "Reservation created successfully", () -> {
			return reservationService.createReservation(dto, rUser.getRestaurant());
		});
	}

	@PutMapping("/{reservationId}/accept")
	@Operation(summary = "Accept a reservation", description = "Endpoint to accept a reservation by its ID")
	@PreAuthorize("@securityRUserService.hasPermissionOnReservation(#reservationId)")
	@WrapperType(dataClass = ReservationDTO.class, type = WrapperDataType.DTO)
	public ResponseEntity<ResponseWrapper<ReservationDTO>> acceptReservation(@PathVariable Long reservationId) {
		return execute("accept reservation", () -> reservationService.setStatus(reservationId, Reservation.Status.ACCEPTED));
	}

	@PutMapping("/{reservationId}/reject")
	@Operation(summary = "Reject a reservation", description = "Endpoint to reject a reservation by its ID")
	@WrapperType(dataClass = ReservationDTO.class, type = WrapperDataType.DTO)
	public ResponseEntity<ResponseWrapper<ReservationDTO>> rejectReservation(@PathVariable Long reservationId) {
		return execute("reject reservation", () -> reservationService.setStatus(reservationId, Reservation.Status.REJECTED));
	}

	@PutMapping("/{reservationId}/no_show")
	@Operation(summary = "Mark a reservation as no show", description = "Endpoint to mark a reservation as no show by its ID")
	@WrapperType(dataClass = ReservationDTO.class, type = WrapperDataType.DTO)
	public ResponseEntity<ResponseWrapper<ReservationDTO>> markReservationNoShow(@PathVariable Long reservationId) {
		return execute("mark reservation no show", () -> reservationService.setStatus(reservationId, Reservation.Status.NO_SHOW));
	}

	@PutMapping("/{reservationId}/seated")
	@Operation(summary = "Mark a reservation as seated", description = "Endpoint to mark a reservation as seated by its ID")
	@WrapperType(dataClass = ReservationDTO.class, type = WrapperDataType.DTO)
	public ResponseEntity<ResponseWrapper<ReservationDTO>> markReservationSeated(@PathVariable Long reservationId) {
		return execute("mark reservation seated", () -> reservationService.setStatus(reservationId, Reservation.Status.SEATED));
	}

	@Operation(summary = "Accept a reservation modification request", description = "Endpoint to accept a reservation modification request by its ID")
	@PutMapping("/accept_modification/{modId}")
	@WrapperType(dataClass = ReservationDTO.class, type = WrapperDataType.DTO)
	public ResponseEntity<ResponseWrapper<ReservationDTO>> acceptReservationModificationRequest(@PathVariable Long modId) {
		return execute("accept reservation modification", () -> reservationService.AcceptReservatioModifyRequestAndReturnDTO(modId));
	}

	@Operation(summary = "Get all reservations of a restaurant", description = "Retrieve all reservations of a restaurant")
	@GetMapping(value = "/reservations")
	
	@WrapperType(dataClass = ReservationDTO.class, type = WrapperDataType.LIST)
	public ResponseEntity<ResponseWrapper<List<ReservationDTO>>> getReservations(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end,
			@AuthenticationPrincipal RUser rUser) {
		return executeList("get reservations", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			Collection<ReservationDTO> reservations = reservationService.getReservations(restaurantId, start, end);
			return reservations instanceof List ? (List<ReservationDTO>) reservations
					: new java.util.ArrayList<>(reservations);
		});
	}

	@Operation(summary = "Get all accepted reservations of a restaurant", description = "Retrieve all accepted reservations of a restaurant")
	@GetMapping(value = "/accepted/get")
	
	@WrapperType(dataClass = ReservationDTO.class, type = WrapperDataType.LIST)
	public ResponseEntity<ResponseWrapper<List<ReservationDTO>>> getAcceptedReservations(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end,
			@AuthenticationPrincipal RUser rUser) {
		return executeList("get accepted reservations", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			Collection<ReservationDTO> reservations = reservationService.getAcceptedReservations(restaurantId, start,
					end);
			return reservations instanceof List ? (List<ReservationDTO>) reservations
					: new java.util.ArrayList<>(reservations);
		});
	}

	@Operation(summary = "Get all reservations of a restaurant with pagination", description = "Retrieve all reservations of a restaurant with pagination")
	@GetMapping(value = "/pageable")
	@WrapperType(dataClass = ReservationDTO.class, type = WrapperDataType.PAGE)
	public ResponseEntity<ResponseWrapper<Page<ReservationDTO>>> getReservationsPageable(
			@AuthenticationPrincipal RUser rUser,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end,
			@RequestParam int page,
			@RequestParam int size) {
		return executePaginated("get reservations pageable", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			Pageable pageable = PageRequest.of(page, size);
			return reservationService.getReservationsPageable(restaurantId, start, end, pageable);
		});
	}

	@Operation(summary = "Get all pending reservations of a restaurant", description = "Retrieve all pending reservations of a restaurant with optional date filtering")
	@GetMapping(value = "/pending/get")
	
	@WrapperType(dataClass = ReservationDTO.class, type = WrapperDataType.LIST)
	public ResponseEntity<ResponseWrapper<List<ReservationDTO>>> getPendingReservations(
			@RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end,
			@AuthenticationPrincipal RUser rUser) {
		return executeList("get pending reservations", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			Collection<ReservationDTO> reservations = reservationService.getPendingReservations(restaurantId, start,
					end);
			return reservations instanceof List ? (List<ReservationDTO>) reservations
					: new java.util.ArrayList<>(reservations);
		});
	}
}
