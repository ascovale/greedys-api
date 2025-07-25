package com.application.restaurant.controller.rUser;

import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
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

import com.application.common.controller.BaseController;
import com.application.common.web.dto.ApiResponse;
import com.application.common.web.dto.get.RUserDTO;
import com.application.common.web.error.InvalidOldPasswordException;
import com.application.restaurant.controller.utils.RestaurantControllerUtils;
import com.application.restaurant.persistence.dao.RestaurantRoleDAO;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.service.RUserService;
import com.application.restaurant.web.dto.post.NewRUserDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
public class RUserController extends BaseController {

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
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_' + #role.toUpperCase() + '_WRITE')")
    @PostMapping(value = "/add_role")
    public ResponseEntity<ApiResponse<RUserDTO>> addRoleToRUser(
            @RequestParam String role,
            @RequestParam Long RUserId) {
        return execute("add role to user", () -> {
            validateRole(role);
            return RUserService.addRUserRole(RUserId, "ROLE_" + role.toUpperCase());
        });
    }

    /**
     * Removes a specific role from a restaurant user.
     *
     * @param role            The role to remove.
     * @param RUserId The ID of the restaurant user.
     * @return ResponseEntity containing the updated user details.
     */
    @Operation(summary = "Remove a role from a restaurant user", description = "Remove a specific role from an existing restaurant user")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_' + #role.toUpperCase() + '_WRITE')")
    @PostMapping(value = "/remove_role")
    public ResponseEntity<ApiResponse<RUserDTO>> removeRoleFromRUser(
            @RequestParam String role,
            @RequestParam Long RUserId) {
        return execute("remove role from user", () -> {
            validateRole(role);
            return RUserService.removeRUserRole(RUserId, "ROLE_" + role.toUpperCase());
        });
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
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MANAGER_WRITE')")
    @DeleteMapping(value = "/disable_user/{RUserId}")
    public ResponseEntity<ApiResponse<String>> disableRUser(@PathVariable Long RUserId) {
        return executeVoid("disable user", "User disabled successfully", () -> 
            RUserService.disableRUser(RestaurantControllerUtils.getCurrentRUser().getId(), RUserId));
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
    @PostMapping(value = "/password/new_token")
    public ResponseEntity<ApiResponse<String>> changeUserPassword(
            @Parameter(description = "Locale for response messages") final Locale locale,
            @Parameter(description = "The old password", required = true) @RequestParam String oldPassword,
            @Parameter(description = "The new password", required = true) @RequestParam String newPassword,
            @Parameter(description = "The user's email (optional)") @RequestParam(required = false) String email) {
        return executeVoid("change user password", () -> {
            if (!RUserService.checkIfValidOldPassword(RestaurantControllerUtils.getRUserId(), oldPassword)) {
                throw new InvalidOldPasswordException();
            }
            RUserService.changeRUserPassword(RestaurantControllerUtils.getRUserId(), newPassword);
        });
    }

    


    /**
     * Adds a new user to a restaurant.
     */
    @PostMapping(value = "/new")
    @Operation(summary = "Add a user to a restaurant", description = "Add a new user to a restaurant")
    public ResponseEntity<ApiResponse<RUserDTO>> addRUserToRestaurant(
            @RequestBody NewRUserDTO RUserDTO,
            @RequestParam Long restaurantId) {
        return executeCreate("add user to restaurant", "User added to restaurant successfully", () -> 
            RUserService.addRUserToRestaurant(RUserDTO, restaurantId));
    }

    /**
     * Retrieves details of the current restaurant user.
     */
    @Operation(summary = "Get restaurant user details", description = "Retrieve details of the current restaurant user")
    @GetMapping("/get")
    public ResponseEntity<ApiResponse<RUserDTO>> getRUserDetails() {
        return execute("get user details", () -> {
            RUser currentUser = RestaurantControllerUtils.getCurrentRUser();
            if (currentUser == null) {
                throw new IllegalStateException("User not found");
            }
            return new RUserDTO(currentUser);
        });
    }

    /**
     * Restituisce i permessi (authorities) dell'utente autenticato.
     *
     * @return Lista dei permessi dell'utente corrente.
     */
    @Operation(summary = "Get user authorities", description = "Restituisce i permessi dell'utente autenticato")
    @GetMapping("/authorities")
    public ResponseEntity<ApiResponse<List<String>>> getRUserAuthorities() {
        return execute("get user authorities", () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getAuthorities() == null) {
                throw new SecurityException("No authentication found");
            }
            return authentication.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .toList();
        });
    }
}