package com.application.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
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
import com.application.service.CustomerService;
import com.application.web.dto.AllergyDTO;
import com.application.web.dto.get.CustomerDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequestMapping("/admin/customer")
@RestController
@SecurityRequirement(name = "adminBearerAuth")
@Tag(name = "Admin Customer", description = "Admin management APIs for the Customer")
public class AdminCustomerController {
    //TODO: aggiungere ruoli e permessi ai ruoli come metodi
    private final CustomerService userService;
    @Autowired
    public AdminCustomerController(CustomerService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Create allergy", description = "Creates a new allergy for the specified user by their ID")
    @ApiResponse(responseCode = "200", description = "Allergy created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/allergy/create_allergy")
    public GenericResponse createAllergy(@RequestBody AllergyDTO allergyDto) {
        userService.createAllergy(allergyDto);
        return new GenericResponse("Allergy created successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Delete allergy", description = "Deletes an allergy by its ID")
    @ApiResponse(responseCode = "200", description = "Allergy deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @DeleteMapping("/allergy/delete_allergy/{idAllergy}")
    public GenericResponse deleteAllergy(@PathVariable Long idAllergy) {
        userService.deleteAllergy(idAllergy);
        return new GenericResponse("Allergy deleted successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Modify allergy", description = "Modifies an existing allergy")
    @ApiResponse(responseCode = "200", description = "Allergy modified successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/allergy/{idAllergy}/modify_allergy")
    public GenericResponse modifyAllergy(@PathVariable Long idAllergy, @RequestBody AllergyDTO allergyDto) {
        userService.modifyAllergy(idAllergy, allergyDto);
        return new GenericResponse("Allergy modified successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Block user", description = "Blocks a user by their ID")
    @ApiResponse(responseCode = "200", description = "User blocked successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/block_user/{userId}")
    public GenericResponse blockUser(@PathVariable Long userId) {
        userService.updateCustomerStatus(userId,Customer.Status.BLOCKED);
        return new GenericResponse("User blocked successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Enable user", description = "Enables a user by their ID")
    @ApiResponse(responseCode = "200", description = "User enabled successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/enable_user/{userId}")
    public GenericResponse enableUser(@PathVariable Long userId) {
        userService.updateCustomerStatus(userId,Customer.Status.ENABLED);
        return new GenericResponse("User enabled successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_READ')")
    @GetMapping("/customers")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "users";
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_READ')")
    @Operation(summary = "List users with pagination", description = "Returns a paginated list of users")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully", content = @Content(mediaType = "application/json"))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @GetMapping("/customers/page")
    public Page<CustomerDTO> listUsersWithPagination(@RequestParam int page, @RequestParam int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return userService.findAll(pageable);
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_SWITCH_TO_CUSTOMER')")
    @Operation(summary = "Switch to customer user", description = "Switches the current admin user to a customer user")
    @ApiResponse(responseCode = "200", description = "Switched to customer user successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @GetMapping("/admin/switch_to_customer_user")
    public String switchToCustomerUser() {
        return "redirect:/admin/home";
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_SWITCH_TO_CUSTOMER')")
    @Operation(summary = "Exit customer user", description = "Exits the current customer user session and returns to admin user")
    @ApiResponse(responseCode = "200", description = "Exited customer user successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @GetMapping("/admin/exit_customer_user")
    public String exitCustomerUser() {
        return "redirect:/admin/home";
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Add role to customer", description = "Adds a role to a customer by their ID")
    @ApiResponse(responseCode = "200", description = "Role added successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/{customerId}/add_role")
    public GenericResponse addRoleToCustomer(@PathVariable Long customerId, @RequestParam String role) {
        userService.addRoleToCustomer(customerId, role);
        return new GenericResponse("Role added successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Remove role from customer", description = "Removes a role from a customer by their ID")
    @ApiResponse(responseCode = "200", description = "Role removed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/{customerId}/remove_role")
    public GenericResponse removeRoleFromCustomer(@PathVariable Long customerId, @RequestParam String role) {
        userService.removeRoleFromCustomer(customerId, role);
        return new GenericResponse("Role removed successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Add permission to role", description = "Adds a permission to a role by its name")
    @ApiResponse(responseCode = "200", description = "Permission added successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/role/{roleName}/add_privilege")
    public GenericResponse addPrivilegeToRole(@PathVariable String roleName, @RequestParam String permission) {
        userService.addPrivilegeToRole(roleName, permission);
        return new GenericResponse("Permission added successfully");
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
        if (authentication != null && authentication.getPrincipal() instanceof Customer) {
            return (Customer) authentication.getPrincipal();
        }
        return null;
    }
    /* 
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
    }*/
}

