
package com.application.customer.controller;

import java.util.Collection;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.web.ApiResponse;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.customer.persistence.model.Customer;
import com.application.customer.service.reservation.CustomerReservationService;
import com.application.customer.web.dto.reservations.CustomerNewReservationDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/customer/reservation")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reservation", description = "APIs for managing reservations of the customer")
@RequiredArgsConstructor
@Slf4j
public class CustomerReservationController extends BaseController {
	private final CustomerReservationService customerReservationService;

	@Operation(summary = "The customer user asks for a reservation", description = "Endpoint for the customer to request a reservation")
	@PostMapping("/ask")
	@CreateApiResponses
	public ResponseEntity<ApiResponse<String>> askReservation(@RequestBody CustomerNewReservationDTO DTO, @AuthenticationPrincipal Customer customer) {
		return executeVoid("askReservation", "Reservation requested successfully", () -> 
			customerReservationService.createReservation(DTO, customer));
	}

	@PreAuthorize("@securityCustomerService.hasPermissionOnReservation(#reservationId)")
	@Operation(summary = "The customer user deletes a reservation", description = "Endpoint for the customer to delete a reservation")
	@DeleteMapping("/{reservationId}/delete")
	public ResponseEntity<ApiResponse<String>> deleteReservation(@PathVariable Long reservationId) {
		return executeVoid("deleteReservation", "Reservation deleted successfully", () -> 
			customerReservationService.deleteReservation(reservationId));
	}

	@PreAuthorize("@securityCustomerService.hasPermissionOnReservation(#reservationId)")
	@Operation(summary = "The customer user requests a reservation modification", description = "Endpoint for the customer to request a reservation modification")
	@CreateApiResponses
	@PostMapping("/{reservationId}/request_modify")
	public ResponseEntity<ApiResponse<String>> requestModifyReservation(@PathVariable Long reservationId,
			@RequestBody CustomerNewReservationDTO DTO,
			@AuthenticationPrincipal Customer customer) {
		return executeVoid("requestModifyReservation", "Reservation modification requested successfully", () -> 
			customerReservationService.requestModifyReservation(reservationId, DTO, customer));
	}

	@PreAuthorize("@securityCustomerService.hasPermissionOnReservation(#reservationId)")
	@Operation(summary = "The customer user rejects a reservation", description = "Endpoint for the customer to reject a reservation created by the restaurant or admin")
	@PutMapping("/{reservationId}/reject")
	public ResponseEntity<ApiResponse<String>> rejectReservationCreatedByAdminOrRestaurant(@PathVariable Long reservationId) {
		return executeVoid("rejectReservation", "Reservation rejected successfully", () -> 
			customerReservationService.rejectReservation(reservationId));
	}

	@Operation(summary = "Get user's reservations", description = "Retrieve the list of reservations for the user")
	@GetMapping("/reservations")
	@ReadApiResponses
	public ResponseEntity<ApiResponse<Collection<ReservationDTO>>> getCustomerReservations(@AuthenticationPrincipal Customer customer) {
		return execute("getCustomerReservations", () -> customerReservationService.findAllCustomerReservations(customer.getId()));
	}

	@PreAuthorize("@securityCustomerService.hasPermissionOnReservation(#reservationId)")
	@Operation(summary = "Get a single reservation by ID", description = "Retrieve a specific reservation for the user by its ID")
	@ReadApiResponses
	@GetMapping("/{reservationId}")
	public ResponseEntity<ApiResponse<ReservationDTO>> getReservationById(@PathVariable Long reservationId) {
		return execute("getReservationById", () -> customerReservationService.findReservationById(reservationId));
	}
}
