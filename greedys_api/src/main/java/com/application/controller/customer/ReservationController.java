package com.application.controller.customer;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

/**
 * The ReservationController class is responsible for handling HTTP requests
 * related to reservations.
 * It contains methods for booking, creating reservations, retrieving
 * reservation lists, and managing the calendar.
 */
@RestController
@RequestMapping("/customer/reservation")
@SecurityRequirement(name = "customerBearerAuth")
@Tag(name = "Reservation", description = "APIs for managing reservations")
public class ReservationController {
	@Autowired
	private ReservationService reservationService;
	
	@Operation(summary = "The customer user ask for a reservation", description = "Endpoint to ask for a reservation")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservation requested successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/ask")
	public ResponseEntity<?> askReservation(@RequestBody CustomerNewReservationDTO DTO) {
		reservationService.askForReservation(DTO);
		return ResponseEntity.ok().build();
	}

	@PreAuthorize("authentication.principal.isEnabled() and @customerSecurityService.hasPermissionOnReservation(#reservationId)")
	@Operation(summary = "The customer user deletes a reservation", description = "Endpoint to delete a reservation")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Reservation deleted successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
		@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/{reservationId}/delete")
	public ResponseEntity<?> deleteReservation(@PathVariable Long reservationId) {
		reservationService.customerDeleteReservation(reservationId);
		return ResponseEntity.ok().build();
	}

	@PreAuthorize("authentication.principal.isEnabled() and @customerSecurityService.hasPermissionOnReservation(#reservationId)")
	@Operation(summary = "The customer user requests a reservation modification", description = "Endpoint to request a reservation modification")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Reservation modification requested successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
		@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping("/{reservationId}/request_modify")
	public ResponseEntity<?> requestModifyReservation(@PathVariable Long reservationId,@RequestBody CustomerNewReservationDTO DTO) {
		reservationService.requestModifyReservation(reservationId,DTO);
		return ResponseEntity.ok().build();
	}

	@PreAuthorize("authentication.principal.isEnabled() and @customerSecurityService.hasPermissionOnReservation(#reservationId)")
	@Operation(summary = "The customer user rejects a reservation", description = "Endpoint User reject a reservation created by the restaurant or admin")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Reservation rejected successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
		@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PutMapping("/{reservationId}/reject")
	public ResponseEntity<?> rejectReservationCreatedByAdminOrRestaurant(@PathVariable Long reservationId) {
		reservationService.rejectReservationCreatedByAdminOrRestaurant(reservationId);
		return ResponseEntity.ok().build();
	}

	@PreAuthorize("authentication.principal.isEnabled() and @customerSecurityService.hasPermissionOnReservation(#reservationId)")
	@Operation(summary = "Get user's reservations", description = "Recupera l'elenco delle prenotazioni dell'utente")
    @ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class))))
    @ApiResponse(responseCode = "404", description = "Utente non trovato")
    @GetMapping("/reservations")
    public Collection<ReservationDTO> getUserReservations() {
        return reservationService.findAllUserReservations(getUserId());
    }

	public Long getUserId() {
        return getCurrentUser().getId();
    }

    private Customer getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Customer) {
            return (Customer) authentication.getPrincipal();
        }
        return null;
    }
}
