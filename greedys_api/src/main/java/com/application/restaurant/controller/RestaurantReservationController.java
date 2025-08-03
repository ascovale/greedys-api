package com.application.restaurant.controller;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.service.reservation.ReservationService;
import com.application.common.web.ListResponseWrapper;
import com.application.common.web.PageResponseWrapper;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.service.RestaurantNotificationService;
import com.application.restaurant.web.dto.reservation.RestaurantNewReservationDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

	@Operation(summary = "Create a new reservation", description = "Endpoint to create a new reservation")
	@ApiResponse(responseCode = "201", description = "Reservation created successfully", 
                content = @Content(schema = @Schema(implementation = String.class)))
	@PostMapping("/new")
	@PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE')")
	@CreateApiResponses
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseWrapper<String> createReservation(@RequestBody RestaurantNewReservationDTO dto, @AuthenticationPrincipal RUser rUser) {
		return executeCreate("create reservation", "Reservation created successfully", () -> {
			reservationService.createReservation(dto, rUser.getRestaurant());
			restaurantNotificationService.sendNotificationToAllUsers(
					"New reservation created",
					"Reservation for " + dto.getPax() + " pax on " + dto.getReservationDay() + " at " + dto.getIdSlot(),
					null,
					rUser.getRestaurant().getId()
			);
			return "success";
		});
	}

	@PutMapping("/{reservationId}/accept")
	@Operation(summary = "Accept a reservation", description = "Endpoint to accept a reservation by its ID")
	@ApiResponse(responseCode = "200", description = "Reservation accepted successfully", 
                content = @Content(schema = @Schema(implementation = String.class)))
	@PreAuthorize("@securityRUserService.hasPermissionOnReservation(#reservationId)")
	@ResponseStatus(HttpStatus.OK)
	public ResponseWrapper<String> acceptReservation(@PathVariable Long reservationId) {
		return executeVoid("accept reservation", "Reservation accepted successfully", () -> 
			reservationService.setStatus(reservationId, Reservation.Status.ACCEPTED));
	}

	@PutMapping("/{reservationId}/reject")
	@Operation(summary = "Reject a reservation", description = "Endpoint to reject a reservation by its ID")
	@ApiResponse(responseCode = "200", description = "Reservation rejected successfully", 
                content = @Content(schema = @Schema(implementation = String.class)))
	@ResponseStatus(HttpStatus.OK)
	public ResponseWrapper<String> rejectReservation(@PathVariable Long reservationId) {
		return executeVoid("reject reservation", "Reservation rejected successfully", () -> 
			reservationService.setStatus(reservationId, Reservation.Status.REJECTED));
	}

	@PutMapping("/{reservationId}/no_show")
	@Operation(summary = "Mark a reservation as no show", description = "Endpoint to mark a reservation as no show by its ID")
	@ApiResponse(responseCode = "200", description = "Reservation marked as no show successfully", 
                content = @Content(schema = @Schema(implementation = String.class)))
	@ResponseStatus(HttpStatus.OK)
	public ResponseWrapper<String> markReservationNoShow(@PathVariable Long reservationId) {
		return executeVoid("mark reservation no show", "Reservation marked as no show successfully", () -> 
			reservationService.setStatus(reservationId, Reservation.Status.NO_SHOW));
	}

	@PutMapping("/{reservationId}/seated")
	@Operation(summary = "Mark a reservation as seated", description = "Endpoint to mark a reservation as seated by its ID")
	@ApiResponse(responseCode = "200", description = "Reservation marked as seated successfully", 
                content = @Content(schema = @Schema(implementation = String.class)))
	@ResponseStatus(HttpStatus.OK)
	public ResponseWrapper<String> markReservationSeated(@PathVariable Long reservationId) {
		return executeVoid("mark reservation seated", "Reservation marked as seated successfully", () -> 
			reservationService.setStatus(reservationId, Reservation.Status.SEATED));
	}

	@Operation(summary = "Accept a reservation modification request", description = "Endpoint to accept a reservation modification request by its ID")
	@ApiResponse(responseCode = "200", description = "Reservation modification accepted successfully", 
                content = @Content(schema = @Schema(implementation = String.class)))
	@PutMapping("/accept_modification/{modId}")
	@ResponseStatus(HttpStatus.OK)
	public ResponseWrapper<String> acceptReservationModificationRequest(@PathVariable Long modId) {
		return executeVoid("accept reservation modification", "Reservation modification accepted successfully", () -> 
			reservationService.AcceptReservatioModifyRequest(modId));
	}

	@Operation(summary = "Get all reservations of a restaurant", description = "Retrieve all reservations of a restaurant")
	@ApiResponse(responseCode = "200", description = "Reservations retrieved successfully", 
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class))))
	@GetMapping(value = "/reservations")
	@ReadApiResponses
	@ResponseStatus(HttpStatus.OK)
	public ListResponseWrapper<ReservationDTO> getReservations(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end,
			@AuthenticationPrincipal RUser rUser) {
		return executeList("get reservations", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			Collection<ReservationDTO> reservations = reservationService.getReservations(restaurantId, start, end);
			return reservations instanceof List ? (List<ReservationDTO>) reservations : new java.util.ArrayList<>(reservations);
		});
	}

	@Operation(summary = "Get all accepted reservations of a restaurant", description = "Retrieve all accepted reservations of a restaurant")
	@ApiResponse(responseCode = "200", description = "Accepted reservations retrieved successfully", 
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class))))
	@GetMapping(value = "/accepted/get")
	@ReadApiResponses
	@ResponseStatus(HttpStatus.OK)
	public ListResponseWrapper<ReservationDTO> getAcceptedReservations(
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end,
			@AuthenticationPrincipal RUser rUser) {
		return executeList("get accepted reservations", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			Collection<ReservationDTO> reservations = reservationService.getAcceptedReservations(restaurantId, start, end);
			return reservations instanceof List ? (List<ReservationDTO>) reservations : new java.util.ArrayList<>(reservations);
		});
	}

	@Operation(summary = "Get all reservations of a restaurant with pagination", description = "Retrieve all reservations of a restaurant with pagination")
	@ApiResponse(responseCode = "200", description = "Paginated reservations retrieved successfully",
        content = @Content(schema = @Schema(
            type = "object",
            description = "Page<ReservationDTO> - Spring Page object with content array of ReservationDTO and pagination metadata. The 'content' property is an array of ReservationDTO objects.",
            requiredProperties = {"content", "totalElements", "totalPages", "size", "number"},
            subTypes = {ReservationDTO.class},
            example = """
                {
                    "content": [
                        {"id": 1, "customerName": "John Doe", "status": "PENDING", "reservationDay": "2024-01-15", "pax": 4},
                        {"id": 2, "customerName": "Jane Smith", "status": "ACCEPTED", "reservationDay": "2024-01-15", "pax": 2}
                    ],
                    "totalElements": 50,
                    "totalPages": 5,
                    "size": 10,
                    "number": 0,
                    "first": true,
                    "last": false,
                    "empty": false
                }
                """
        )))
	@GetMapping(value = "/pageable")
	@ReadApiResponses
	@ResponseStatus(HttpStatus.OK)
	public PageResponseWrapper<ReservationDTO> getReservationsPageable(
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
	@ApiResponse(responseCode = "200", description = "Pending reservations retrieved successfully", 
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class))))
	@GetMapping(value = "/pending/get")
	@ReadApiResponses
	@ResponseStatus(HttpStatus.OK)
	public ListResponseWrapper<ReservationDTO> getPendingReservations(
			@RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
			@RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end,
			@AuthenticationPrincipal RUser rUser) {
		return executeList("get pending reservations", () -> {
			Long restaurantId = rUser.getRestaurant().getId();
			Collection<ReservationDTO> reservations = reservationService.getPendingReservations(restaurantId, start, end);
			return reservations instanceof List ? (List<ReservationDTO>) reservations : new java.util.ArrayList<>(reservations);
		});
	}
}
