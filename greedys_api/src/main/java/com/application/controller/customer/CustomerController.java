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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.customer.Customer;
import com.application.security.user.ISecurityUserService;
import com.application.service.CustomerService;
import com.application.web.dto.get.CustomerDTO;
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
    private final CustomerService customerService;
    private final MessageSource messages;
    private final ISecurityUserService securityCustomerService;

    //TODO Le mail dovrebbero essere mandate nel service 

    public CustomerController(CustomerService customerService,
            MessageSource messages, @Qualifier("customerSecurityService") ISecurityUserService securityCustomerService) {
        this.customerService = customerService;
        this.messages = messages;
        this.securityCustomerService = securityCustomerService;
    }

    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Get Customer ID", description = "Ottiene l'ID del customer corrente", responses = {
            @ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Long.class))),
            @ApiResponse(responseCode = "401", description = "Non autorizzato"),
            @ApiResponse(responseCode = "403", description = "Accesso negato"),
            @ApiResponse(responseCode = "404", description = "Utente non trovato")
    })
    @GetMapping("/id")
    public Long getCustomerId() {
        return getCurrentCustomer().getId();
    }
    
    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Get current customer details", description = "Ottiene i dettagli del customer corrente", responses = {
            @ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerDTO.class))),
            @ApiResponse(responseCode = "401", description = "Non autorizzato"),
            @ApiResponse(responseCode = "403", description = "Accesso negato")
    })
    @GetMapping("/details")
    public CustomerDTO getCustomerDetails() {
        Customer currentCustomer = getCurrentCustomer();
        if (currentCustomer == null) {
            throw new IllegalStateException("Current customer not found");
        }
        return new CustomerDTO(currentCustomer);
    }
    //TODO: impostiazione preferenze notifiche
    //TODO: impostiazione preferenze nel sistema ad esempio come ricevere notifica però è più restaurant

/* TODO: Implementare questo
    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Get customer options", description = "Ottiene le opzioni configurate per il customer corrente", responses = {
        @ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "401", description = "Non autorizzato"),
        @ApiResponse(responseCode = "403", description = "Accesso negato")
    })
    @GetMapping("/options")
    public Map<String, Object> getCustomerOptions() {
        Customer currentUser = getCurrentCustomer();
        if (currentUser == null) {
        throw new IllegalStateException("Current customer not found");
        }
        return customerService.getCustomerOptions(currentUser.getId());
    } */

    private Customer getCurrentCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Customer) {
            return (Customer) authentication.getPrincipal();
        }
        return null;
    }

    //TODO: Questo metodo dovrebbe essere disponibile anche dalle altre tipologie di utente
    //TODO: Implementare customerStats visualizza caratteristiche no show ecc
    //TODO: Implementare restaurant Stats
     
    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Get customer by id", description = "Recupera un customer specifico tramite il suo ID")
    @ApiResponse(responseCode = "200", description = "Operazione riuscita", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerDTO.class)))
    @ApiResponse(responseCode = "404", description = "Utente non trovato")
    @GetMapping("/get")
    public CustomerDTO getCustomer() {
        return new CustomerDTO(getCurrentCustomer());
    }

    @PreAuthorize("authentication.principal.isEnabled()")
    // ------------------- Password Management ----------------------------- //
    @Operation(summary = "Reset customer password by email", description = "Invia un'email per il reset della password all'utente specificato tramite email")
    @ApiResponse(responseCode = "200", description = "Email per il reset della password inviata con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Richiesta non valida")
    @PostMapping(value = "/password/reset")
    public GenericResponse resetPassword(final HttpServletRequest request,
            @Parameter(description = "Email dell'utente per cui resettare la password") @RequestParam("email") final String userEmail) {
        final Customer customer = customerService.findUserByEmail(userEmail);
        if (customer != null) {
            final String token = UUID.randomUUID().toString();
            customerService.createPasswordResetTokenForUser(customer, token);
            // mailSender.send(constructResetTokenEmail(getAppUrl(request),
            // request.getLocale(), token, customer));
        }
        return new GenericResponse(messages.getMessage("message.resetPasswordEmail", null, request.getLocale()));
    }

    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Generate new token for password change", description = "Cambia la password dell'utente dopo aver verificato la vecchia password")
    @ApiResponse(responseCode = "200", description = "Password cambiata con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Password vecchia non valida o dati non validi")
    @PostMapping(value = "/password/new_token")
    public GenericResponse changeUserPassword(
            @Parameter(description = "Locale per i messaggi di risposta") final Locale locale,
            @Parameter(description = "DTO con la vecchia e la nuova password", required = true) @Valid UpdatePasswordDTO passwordDto) {
        if (!customerService.checkIfValidOldPassword(getCustomerId(), passwordDto.getOldPassword())) {
            throw new InvalidOldPasswordException();
        }
        customerService.changeUserPassword(getCustomerId(), passwordDto.getNewPassword());
        return new GenericResponse(messages.getMessage("message.updatePasswordSuc", null, locale));
    }

    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Confirm password change with token", description = "Conferma il cambio della password utilizzando un token")
    @ApiResponse(responseCode = "200", description = "Password cambiata con successo o token non valido", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    @PutMapping(value = "/password/confirm")
    public String confirmPasswordChange(
            @Parameter(description = "Token di reset della password") @RequestParam final String token) {
        final String result = securityCustomerService.validatePasswordResetToken(getCustomerId(), token);
        if (result != null) {
            return "invalidToken";
        }
        return "success";
    }

    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Delete customer", description = "Cancella un customer specifico tramite il suo ID")
    @ApiResponse(responseCode = "200", description = "Customer cancellato con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "404", description = "Customer non trovato")
    @DeleteMapping("/delete")
    public GenericResponse deleteCustomer() {
        customerService.deleteUserById(getCustomerId());
        return new GenericResponse("Customer deleted successfully");
    }

    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Update customer", description = "Modifica i dettagli di un customer specifico tramite il suo ID")
    @ApiResponse(responseCode = "200", description = "Utente aggiornato con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "404", description = "Utente non trovato")
    @PutMapping("/update")
    public GenericResponse updateCustomer(@Valid @RequestBody NewCustomerDTO customerDto) {
        customerService.updateUser(getCustomerId(), customerDto);
        return new GenericResponse("Customer modified successfully");
    }

    //TODO: implementare il metodo nel service per segnalare un abuso
    // L'abuso deve essere segnalato all'admin del sistema e deve contenere anche un messaggio
    /* 
    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Report restaurant abuse", description = "Segnala un abuso da parte di un ristorante")
    @ApiResponse(responseCode = "200", description = "Abuso segnalato con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
    @PostMapping("/report/{restaurantId}")
    public GenericResponse reportRestaurantAbuse(@PathVariable Long restaurantId) {
        customerService.reportRestaurantAbuse(restaurantId);
        return new GenericResponse("Abuse reported successfully");
    }*/

}
