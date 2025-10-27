
package com.application.customer.controller;

import java.util.Collection;
import java.util.List;

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
	@CreateApiResponses
	@PostMapping("/ask")
	public ResponseEntity<ReservationDTO> askReservation(@RequestBody CustomerNewReservationDTO DTO, @AuthenticationPrincipal Customer customer) {
		return executeCreate("askReservation", "Reservation requested successfully", () -> {
			ReservationDTO reservationDTO = customerReservationService.createReservation(DTO, customer);
			return reservationDTO;
		});
	}

	@PreAuthorize("@securityCustomerService.hasPermissionOnReservation(#reservationId)")
	@Operation(summary = "The customer user deletes a reservation", description = "Endpoint for the customer to delete a reservation")
	@ReadApiResponses
	@DeleteMapping("/{reservationId}/delete")
	public ResponseEntity<String> deleteReservation(@PathVariable Long reservationId) {
		return execute("deleteReservation", () -> {
			customerReservationService.deleteReservation(reservationId);
			return "Reservation deleted successfully";
		});
	}

	@PreAuthorize("@securityCustomerService.hasPermissionOnReservation(#reservationId)")
	@Operation(summary = "The customer user requests a reservation modification", description = "Endpoint for the customer to request a reservation modification")
	@ReadApiResponses
	@PostMapping("/{reservationId}/request_modify")
	public ResponseEntity<String> requestModifyReservation(@PathVariable Long reservationId,
			@RequestBody CustomerNewReservationDTO DTO,
			@AuthenticationPrincipal Customer customer) {
		return execute("requestModifyReservation", () -> {
			customerReservationService.requestModifyReservation(reservationId, DTO, customer);
			return "Reservation modification requested successfully";
		});
	}

	@PreAuthorize("@securityCustomerService.hasPermissionOnReservation(#reservationId)")
	@Operation(summary = "The customer user rejects a reservation", description = "Endpoint for the customer to reject a reservation created by the restaurant or admin")
	@ReadApiResponses
	@PutMapping("/{reservationId}/reject")
	public ResponseEntity<String> rejectReservationCreatedByAdminOrRestaurant(@PathVariable Long reservationId) {
		return execute("rejectReservation", () -> {
			customerReservationService.rejectReservation(reservationId);
			return "Reservation rejected successfully";
		});
	}

	@Operation(summary = "Get user's reservations", description = "Retrieve the list of reservations for the user")
	@ReadApiResponses
	@GetMapping("/reservations")
	
    public ResponseEntity<List<ReservationDTO>> getCustomerReservations(@AuthenticationPrincipal Customer customer) {
		return executeList("getCustomerReservations", () -> {
			Collection<ReservationDTO> reservations = customerReservationService.findAllCustomerReservations(customer.getId());
			return reservations instanceof List ? (List<ReservationDTO>) reservations : List.copyOf(reservations);
		});
	}

	@PreAuthorize("@securityCustomerService.hasPermissionOnReservation(#reservationId)")
	@Operation(summary = "Get a single reservation by ID", description = "Retrieve a specific reservation for the user by its ID")
	@ReadApiResponses
	@GetMapping("/{reservationId}")
    public ResponseEntity<ReservationDTO> getReservationById(@PathVariable Long reservationId) {
		return execute("getReservationById", () -> customerReservationService.findReservationById(reservationId));
	}
}

