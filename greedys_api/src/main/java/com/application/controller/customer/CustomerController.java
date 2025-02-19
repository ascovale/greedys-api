package com.application.controller.customer;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.user.Customer;
import com.application.security.user.ISecurityUserService;
import com.application.service.CustomerService;
import com.application.service.ReservationService;
import com.application.web.dto.get.ReservationDTO;
import com.application.web.dto.get.UserDTO;
import com.application.web.dto.post.customer.CustomerNewReservationDTO;
import com.application.web.dto.put.UpdatePasswordDTO;
import com.application.web.error.InvalidOldPasswordException;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Tag(name = "Customer", description = "Controller per la gestione dei customer")
@RestController
@RequestMapping("/customer")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    private final CustomerService userService;
    private final ReservationService reservationService;
    private final MessageSource messages;
    private final ISecurityUserService securityUserService;

    public CustomerController(CustomerService userService, ReservationService reservationService,
            MessageSource messages, ISecurityUserService securityUserService) {
        this.userService = userService;
        this.reservationService = reservationService;
        this.messages = messages;
        this.securityUserService = securityUserService;
    }

    @Operation(summary = "Get user ID", description = "Ottiene l'ID dell'utente corrente", responses = {
            @ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Long.class))),
            @ApiResponse(responseCode = "401", description = "Non autorizzato"),
            @ApiResponse(responseCode = "403", description = "Accesso negato"),
            @ApiResponse(responseCode = "404", description = "Utente non trovato")
    })
    @GetMapping("/id")
    public Long getUserId() {
        return getCurrentUser().getId();
    }

    private Customer getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userService.findUserByEmail(userDetails.getUsername());
        }
        return null;
    }

    @Operation(summary = "Get user by id", description = "Recupera un utente specifico tramite il suo ID")
    @ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class)))
    @ApiResponse(responseCode = "404", description = "Utente non trovato")
    @GetMapping("/{id}")
    public UserDTO getUser(@PathVariable Long id) {
        return userService.findById(id);
    }

    // ------------------- Password Management ----------------------------- //
    @Operation(summary = "Reset user password by email", description = "Invia un'email per il reset della password all'utente specificato tramite email")
    @ApiResponse(responseCode = "200", description = "Email per il reset della password inviata con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Richiesta non valida")
    @PostMapping(value = "/password/reset")
    public GenericResponse resetPassword(final HttpServletRequest request,
            @Parameter(description = "Email dell'utente per cui resettare la password") @RequestParam("email") final String userEmail) {
        final Customer user = userService.findUserByEmail(userEmail);
        if (user != null) {
            final String token = UUID.randomUUID().toString();
            userService.createPasswordResetTokenForUser(user, token);
            // mailSender.send(constructResetTokenEmail(getAppUrl(request),
            // request.getLocale(), token, user));
        }
        return new GenericResponse(messages.getMessage("message.resetPasswordEmail", null, request.getLocale()));
    }

    // change user password
    @Operation(summary = "Generate new token for password change", description = "Cambia la password dell'utente dopo aver verificato la vecchia password")
    @ApiResponse(responseCode = "200", description = "Password cambiata con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Password vecchia non valida o dati non validi")
    @PostMapping(value = "{id}/password")
    public GenericResponse changeUserPassword(
            @Parameter(description = "Locale per i messaggi di risposta") final Locale locale,
            @Parameter(description = "ID dell'utente") @PathVariable Long id,
            @Parameter(description = "DTO con la vecchia e la nuova password", required = true) @Valid UpdatePasswordDTO passwordDto) {
        if (!userService.checkIfValidOldPassword(id, passwordDto.getOldPassword())) {
            throw new InvalidOldPasswordException();
        }
        userService.changeUserPassword(id, passwordDto.getNewPassword());
        return new GenericResponse(messages.getMessage("message.updatePasswordSuc", null, locale));
    }

    @Operation(summary = "Confirm password change with token", description = "Conferma il cambio della password utilizzando un token")
    @ApiResponse(responseCode = "200", description = "Password cambiata con successo o token non valido", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    @PutMapping(value = "/{id}/password")
    public String confirmPasswordChange(
            @Parameter(description = "ID dell'utente") @PathVariable final long id,
            @Parameter(description = "Token di reset della password") @RequestParam final String token) {
        final String result = securityUserService.validatePasswordResetToken(id, token);
        if (result != null) {
            return "invalidToken";
        }
        return "success";
    }

    @Operation(summary = "Get user's reservations", description = "Recupera l'elenco delle prenotazioni dell'utente")
    @ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservationDTO.class))))
    @ApiResponse(responseCode = "404", description = "Utente non trovato")
    @GetMapping("{id}/reservations")
    public Collection<ReservationDTO> getUserReservations(@PathVariable Long id) {
        return reservationService.findAllUserReservations(id);
    }

    @Operation(summary = "Delete user", description = "Cancella un utente specifico tramite il suo ID")
    @ApiResponse(responseCode = "200", description = "Utente cancellato con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "404", description = "Utente non trovato")
    @DeleteMapping("/{id}")
    public GenericResponse deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return new GenericResponse("User deleted successfully");
    }

    @Operation(summary = "Update user", description = "Modifica i dettagli di un utente specifico tramite il suo ID")
    @ApiResponse(responseCode = "200", description = "Utente aggiornato con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class)))
    @ApiResponse(responseCode = "404", description = "Utente non trovato")
    @PutMapping("/{id}")
    public GenericResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO userDto) {
        userService.updateUser(id, userDto);
        return new GenericResponse("User modified successfully");
    }

    @Operation(summary = "Report restaurant abuse", description = "Segnala un abuso da parte di un ristorante")
    @ApiResponse(responseCode = "200", description = "Abuso segnalato con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
    @PostMapping("/report/{restaurantId}")
    public GenericResponse reportRestaurantAbuse(@PathVariable Long userId, @PathVariable Long restaurantId) {
        userService.reportRestaurantAbuse(restaurantId);
        return new GenericResponse("Abuse reported successfully");
    }

    @Operation(summary = "Request reservation modification", description = "Richiede una modifica alla prenotazione specificata")
    @ApiResponse(responseCode = "200", description = "Richiesta di modifica inviata con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "404", description = "Prenotazione non trovata")
    @PutMapping("/reservations/request-modify")
    public GenericResponse requestModifyReservation(
            @PathVariable Long reservationId,
            @RequestParam CustomerNewReservationDTO reservationDto) {
        reservationService.requestModifyReservation(reservationId, reservationDto);
        return new GenericResponse("Reservation modification requested successfully");
    }
}
