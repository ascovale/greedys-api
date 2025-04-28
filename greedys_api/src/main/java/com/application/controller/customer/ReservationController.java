package com.application.controller.customer;

import java.util.Collection;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.customer.Customer;
import com.application.service.ReservationService;
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
@SecurityRequirement(name = "customerBearerAuth")
@Tag(name = "4. Reservation", description = "APIs for managing reservations of the customer")
public class ReservationController {
	@Autowired
	private ReservationService reservationService;

	@Operation(summary = "The customer user asks for a reservation", description = "Endpoint for the customer to request a reservation")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation requested successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReservationDTO.class))),
			@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/ask")
	public ResponseEntity<ReservationDTO> askReservation(@RequestBody CustomerNewReservationDTO DTO) {
		ReservationDTO reservationDTO = reservationService.askForReservation(DTO);
		return ResponseEntity.ok(reservationDTO);
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
		reservationService.customerDeleteReservation(reservationId);
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
			@RequestBody CustomerNewReservationDTO DTO) {
		reservationService.requestModifyReservation(reservationId, DTO);
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
		reservationService.rejectReservationCreatedByAdminOrRestaurant(reservationId);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Get user's reservations", description = "Retrieve the list of reservations for the user")
	@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class))))
	@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
	@ApiResponse(responseCode = "404", description = "User not found")
	@GetMapping("/reservations")
	public Collection<ReservationDTO> getCustomerReservations() {
		System.out.println("->->->      Getting customer reservations");
		return reservationService.findAllCustomerReservations(getUserId());
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
		ReservationDTO reservationDTO = reservationService.findReservationById(reservationId);
		return ResponseEntity.ok(reservationDTO);
	}

	public Long getUserId() {
		Customer currentUser = getCurrentCustomer();
		if (currentUser == null) {
			throw new RuntimeException("Current user is null");
		}
		System.out.println(".-.-.-.-.-.-. Current user: " + currentUser.getId());
		return currentUser.getId();
	}

	private Customer getCurrentCustomer() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof Customer) {
			return (Customer) authentication.getPrincipal();
		} else {
			Logger.getLogger(ReservationController.class.getName()).warning("User not found in SecurityContextHolder");
			throw new RuntimeException("User not found");
		}
	}
	
}
