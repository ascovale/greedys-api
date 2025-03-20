package com.application.controller.customer;

import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.customer.Customer;
import com.application.security.user.ISecurityUserService;
import com.application.service.CustomerService;
import com.application.web.dto.get.UserDTO;
import com.application.web.dto.post.NewCustomerDTO;
import com.application.web.dto.put.UpdatePasswordDTO;
import com.application.web.error.InvalidOldPasswordException;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@SecurityRequirement(name = "customerBearerAuth")
public class CustomerController {
    private final CustomerService userService;
    private final MessageSource messages;
    private final ISecurityUserService securityUserService;    

    public CustomerController(CustomerService userService,
            MessageSource messages, @Qualifier("customerSecurityService") ISecurityUserService securityUserService) {
        this.userService = userService;
        this.messages = messages;
        this.securityUserService = securityUserService;
    }

    @PreAuthorize("authentication.principal.isEnabled()")
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
        if (authentication != null && authentication.getPrincipal() instanceof Customer) {
            return (Customer) authentication.getPrincipal();
        }
        return null;
    }

    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Get user by id", description = "Recupera un utente specifico tramite il suo ID")
    @ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class)))
    @ApiResponse(responseCode = "404", description = "Utente non trovato")
    @GetMapping("/getRestaurantUser")
    public UserDTO getUser() {
        return userService.findById(getUserId());
    }

    @PreAuthorize("authentication.principal.isEnabled()")
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

    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Generate new token for password change", description = "Cambia la password dell'utente dopo aver verificato la vecchia password")
    @ApiResponse(responseCode = "200", description = "Password cambiata con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Password vecchia non valida o dati non validi")
    @PostMapping(value = "/password")
    public GenericResponse changeUserPassword(
            @Parameter(description = "Locale per i messaggi di risposta") final Locale locale,
            @Parameter(description = "DTO con la vecchia e la nuova password", required = true) @Valid UpdatePasswordDTO passwordDto) {
        if (!userService.checkIfValidOldPassword(getUserId(), passwordDto.getOldPassword())) {
            throw new InvalidOldPasswordException();
        }
        userService.changeUserPassword(getUserId(), passwordDto.getNewPassword());
        return new GenericResponse(messages.getMessage("message.updatePasswordSuc", null, locale));
    }

    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Confirm password change with token", description = "Conferma il cambio della password utilizzando un token")
    @ApiResponse(responseCode = "200", description = "Password cambiata con successo o token non valido", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    @PutMapping(value = "/password")
    public String confirmPasswordChange(
            @Parameter(description = "Token di reset della password") @RequestParam final String token) {
        final String result = securityUserService.validatePasswordResetToken(getUserId(), token);
        if (result != null) {
            return "invalidToken";
        }
        return "success";
    }

    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Delete customer", description = "Cancella un utente specifico tramite il suo ID")
    @ApiResponse(responseCode = "200", description = "Customer cancellato con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "404", description = "Customer non trovato")
    @DeleteMapping("/delete")
    public GenericResponse deleteUser() {
        userService.deleteUserById(getUserId());
        return new GenericResponse("Customer deleted successfully");
    }

    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Update user", description = "Modifica i dettagli di un utente specifico tramite il suo ID")
    @ApiResponse(responseCode = "200", description = "Utente aggiornato con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "404", description = "Utente non trovato")
    @PutMapping("/update")
    public GenericResponse updateUser(@Valid @RequestBody NewCustomerDTO customerDto) {
        userService.updateUser(getUserId(), customerDto);
        return new GenericResponse("Customer modified successfully");
    }

    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Report restaurant abuse", description = "Segnala un abuso da parte di un ristorante")
    @ApiResponse(responseCode = "200", description = "Abuso segnalato con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
    @PostMapping("/report/{restaurantId}")
    public GenericResponse reportRestaurantAbuse(@PathVariable Long restaurantId) {
        userService.reportRestaurantAbuse(restaurantId);
        return new GenericResponse("Abuse reported successfully");
    }

}
