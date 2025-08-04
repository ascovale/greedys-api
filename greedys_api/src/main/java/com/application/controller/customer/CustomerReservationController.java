package com.application.controller.customer;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.application.persistence.model.customer.Customer;
import com.application.service.reservation.CustomerReservationService;
import com.application.web.dto.get.ReservationDTO;
import com.application.web.dto.post.customer.CustomerNewReservationDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/customer/reservation")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reservation", description = "APIs for managing reservations of the customer")
public class CustomerReservationController {
	@Autowired
	private CustomerReservationService customerReservationService;

	@Operation(summary = "The customer user asks for a reservation", description = "Endpoint for the customer to request a reservation")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation requested successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReservationDTO.class))),
			@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/ask")
	public ResponseEntity<?> askReservation(@RequestBody CustomerNewReservationDTO DTO, @AuthenticationPrincipal Customer customer) {
		customerReservationService.createReservation(DTO, customer);
		return ResponseEntity.ok().build();
	}

	@PreAuthorize("@securityCustomerService.hasPermissionOnReservation(#reservationId)")
	@Operation(summary = "The customer user deletes a reservation", description = "Endpoint for the customer to delete a reservation")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation deleted successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@DeleteMapping("/{reservationId}/delete")
	public ResponseEntity<?> deleteReservation(@PathVariable Long reservationId) {
		customerReservationService.deleteReservation(reservationId);
		return ResponseEntity.ok().build();
	}

	@PreAuthorize("@securityCustomerService.hasPermissionOnReservation(#reservationId)")
	@Operation(summary = "The customer user requests a reservation modification", description = "Endpoint for the customer to request a reservation modification")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation modification requested successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/{reservationId}/request_modify")
	public ResponseEntity<?> requestModifyReservation(@PathVariable Long reservationId,
			@RequestBody CustomerNewReservationDTO DTO,
			@AuthenticationPrincipal Customer customer) {
		customerReservationService.requestModifyReservation(reservationId, DTO, customer);
		return ResponseEntity.ok().build();
	}

	@PreAuthorize("@securityCustomerService.hasPermissionOnReservation(#reservationId)")
	@Operation(summary = "The customer user rejects a reservation", description = "Endpoint for the customer to reject a reservation created by the restaurant or admin")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation rejected successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PutMapping("/{reservationId}/reject")
	public ResponseEntity<?> rejectReservationCreatedByAdminOrRestaurant(@PathVariable Long reservationId) {
		customerReservationService.rejectReservation(reservationId);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Get user's reservations", description = "Retrieve the list of reservations for the user")
	@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class))))
	@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
	@ApiResponse(responseCode = "404", description = "User not found")
	@GetMapping("/reservations")
	public Collection<ReservationDTO> getCustomerReservations(@AuthenticationPrincipal Customer customer) {
		return customerReservationService.findAllCustomerReservations(customer.getId());
	}

	@PreAuthorize("@securityCustomerService.hasPermissionOnReservation(#reservationId)")
	@Operation(summary = "Get a single reservation by ID", description = "Retrieve a specific reservation for the user by its ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Reservation retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReservationDTO.class))),
		@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
		@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
		@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@GetMapping("/{reservationId}")
	public ResponseEntity<ReservationDTO> getReservationById(@PathVariable Long reservationId) {
		ReservationDTO reservationDTO = customerReservationService.findReservationById(reservationId);
		return ResponseEntity.ok(reservationDTO);
	}
}
