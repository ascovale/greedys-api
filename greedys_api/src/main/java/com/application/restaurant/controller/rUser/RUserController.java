package com.application.restaurant.controller.rUser;

import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.utils.ControllerUtils;
import com.application.common.web.dto.get.RUserDTO;
import com.application.common.web.error.InvalidOldPasswordException;
import com.application.common.web.util.GenericResponse;
import com.application.restaurant.dao.RestaurantRoleDAO;
import com.application.restaurant.model.user.RUser;
import com.application.restaurant.service.RUserService;
import com.application.restaurant.web.post.NewRUserDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Restaurant User Management", description = "Controller for managing restaurant users")
@RestController
@RequestMapping("/restaurant/user")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class RUserController {

    private final RUserService RUserService;
    private final MessageSource messages;
    private final RestaurantRoleDAO roleDAO;

    /**
     * Assigns a specific role to a restaurant user.
     *
     * @param role            The role to assign.
     * @param RUserId The ID of the restaurant user.
     * @return ResponseEntity containing the updated user details.
     */
    @Operation(summary = "Add a role to a restaurant user", description = "Assign a specific role to an existing restaurant user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role assigned successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RUserDTO.class))),
            @ApiResponse(responseCode = "404", description = "User or role not found"),
            @ApiResponse(responseCode = "400", description = "Invalid role")
    })
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_' + #role.toUpperCase() + '_WRITE')")
    @PostMapping(value = "/add_role")
    public ResponseEntity<RUserDTO> addRoleToRUser(
            @RequestParam String role,
            @RequestParam Long RUserId) {
        validateRole(role);
        RUserDTO updatedUser = RUserService.addRUserRole(RUserId, "ROLE_" + role.toUpperCase());
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Removes a specific role from a restaurant user.
     *
     * @param role            The role to remove.
     * @param RUserId The ID of the restaurant user.
     * @return ResponseEntity containing the updated user details.
     */
    @Operation(summary = "Remove a role from a restaurant user", description = "Remove a specific role from an existing restaurant user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role removed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RUserDTO.class))),
            @ApiResponse(responseCode = "404", description = "User or role not found"),
            @ApiResponse(responseCode = "400", description = "Invalid role")
    })
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_' + #role.toUpperCase() + '_WRITE')")
    @PostMapping(value = "/remove_role")
    public ResponseEntity<RUserDTO> removeRoleFromRUser(
            @RequestParam String role,
            @RequestParam Long RUserId) {
        validateRole(role);
        RUserDTO updatedUser = RUserService.removeRUserRole(RUserId, "ROLE_" + role.toUpperCase());
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Validates if the provided role exists in the database.
     *
     * @param role The role to validate.
     * @throws IllegalArgumentException if the role is invalid.
     */
    private void validateRole(String role) {
        if (roleDAO.findByName(role) == null) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

    /**
     * Disables a restaurant user.
     *
     * @param RUserId The ID of the restaurant user to disable.
     * @return ResponseEntity with HTTP status.
     */
    @Operation(summary = "Disable a restaurant user", description = "Disable a restaurant user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User disabled successfully"),
            @ApiResponse(responseCode = "404", description = "User or restaurant not found")
    })
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MANAGER_WRITE')")
    @DeleteMapping(value = "/disable_user/{RUserId}")
    public ResponseEntity<Void> disableRUser(@PathVariable Long RUserId) {
        RUserService.disableRUser(ControllerUtils.getCurrentRUser().getId(), RUserId);
        return ResponseEntity.ok().build();
    }

    /**
     * Changes the user's password after verifying the old password.
     *
     * @param locale      The locale for response messages.
     * @param oldPassword The old password.
     * @param newPassword The new password.
     * @param email       The user's email (optional).
     * @return GenericResponse indicating the result of the operation.
     */
    @Operation(summary = "Generate new token for password change", description = "Changes the user's password after verifying the old password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid old password or invalid data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(value = "/password/new_token")
    public GenericResponse changeUserPassword(
            @Parameter(description = "Locale for response messages") final Locale locale,
            @Parameter(description = "The old password", required = true) @RequestParam String oldPassword,
            @Parameter(description = "The new password", required = true) @RequestParam String newPassword,
            @Parameter(description = "The user's email (optional)") @RequestParam(required = false) String email) {
        if (!RUserService.checkIfValidOldPassword(getRUserId(), oldPassword)) {
            throw new InvalidOldPasswordException();
        }
        RUserService.changeRUserPassword(getRUserId(), newPassword);
        return new GenericResponse(messages.getMessage("message.updatePasswordSuc", null, locale));
    }

    /**
     * Retrieves the ID of the current restaurant user.
     *
     * @return The ID of the current restaurant user.
     */
    @Operation(summary = "Get restaurant user ID", description = "Retrieves the ID of the current restaurant user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Long.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/id")
    public Long getRUserId() {
        return getCurrentRUser().getId();
    }

    /**
     * Retrieves the current authenticated restaurant user.
     *
     * @return The current authenticated restaurant user, or null if not authenticated.
     */
    private RUser getCurrentRUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof RUser) {
            System.out.println("\n\n\n\nAuthenticated restaurant user found.\n\n\n");
            System.out.println("Authorities: " + authentication.getAuthorities());
            System.out.println("User: " + authentication.getPrincipal());
            return (RUser) authentication.getPrincipal();
        }
        System.out.println("\n\n\n\nNo authenticated restaurant user found.\n\n\n");
        return null;
    }

    /**
     * Adds a new user to a restaurant.
     */
    @PostMapping(value = "/new")
    @Operation(summary = "Add a user to a restaurant", description = "Add a new user to a restaurant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RUserDTO.class))),
            @ApiResponse(responseCode = "404", description = "Restaurant not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<RUserDTO> addRUserToRestaurant(
            @RequestBody NewRUserDTO RUserDTO,
            @RequestParam Long restaurantId) {
        RUserDTO createdUser = RUserService.addRUserToRestaurant(RUserDTO, restaurantId);
        return new ResponseEntity<>(createdUser, HttpStatus.OK);
    }

    /**
     * Retrieves details of the current restaurant user.
     */
    @Operation(summary = "Get restaurant user details", description = "Retrieve details of the current restaurant user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RUserDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/get")
    public ResponseEntity<RUserDTO> getRUserDetails() {
        RUser currentUser = getCurrentRUser();
        if (currentUser == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        RUserDTO userDTO = new RUserDTO(currentUser);
        return new ResponseEntity<>(userDTO, HttpStatus.OK);
    }

    /**
     * Restituisce i permessi (authorities) dell'utente autenticato.
     *
     * @return Lista dei permessi dell'utente corrente.
     */
    @Operation(summary = "Get user authorities", description = "Restituisce i permessi dell'utente autenticato")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/authorities")
    public ResponseEntity<List<String>> getRUserAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<String> authorities = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();
        return new ResponseEntity<>(authorities, HttpStatus.OK);
    }
}