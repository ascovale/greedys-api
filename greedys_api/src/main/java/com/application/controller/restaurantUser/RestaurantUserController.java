package com.application.controller.restaurantUser;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.application.controller.utils.ControllerUtils;
import com.application.persistence.dao.restaurant.RestaurantRoleDAO;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.service.RestaurantUserService;
import com.application.web.dto.get.RestaurantUserDTO;
import com.application.web.dto.post.NewRestaurantUserDTO;
import com.application.web.dto.put.UpdatePasswordDTO;
import com.application.web.error.InvalidOldPasswordException;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@Tag(name = "Restaurant User", description = "Controller for managing restaurant users")
@RestController
@RequestMapping("/restaurant/user")
@SecurityRequirement(name = "restaurantBearerAuth")
public class RestaurantUserController {

    private final RestaurantUserService restaurantUserService;
    private final MessageSource messages;
    private final RestaurantRoleDAO roleDAO;

    public RestaurantUserController(RestaurantUserService restaurantUserService, MessageSource messages, RestaurantRoleDAO roleDAO) {
        this.messages = messages;
        this.restaurantUserService = restaurantUserService;
        this.roleDAO = roleDAO;
    }

    /**
     * Assigns a specific role to a restaurant user.
     *
     * @param role            The role to assign.
     * @param restaurantUserId The ID of the restaurant user.
     * @return ResponseEntity containing the updated user details.
     */
    @Operation(summary = "Add a role to a restaurant user", description = "Assign a specific role to an existing restaurant user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role assigned successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
            @ApiResponse(responseCode = "404", description = "User or role not found"),
            @ApiResponse(responseCode = "400", description = "Invalid role")
    })
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_' + #role.toUpperCase() + '_WRITE')")
    @PostMapping(value = "/add_role")
    public ResponseEntity<RestaurantUserDTO> addRoleToRestaurantUser(
            @RequestParam String role,
            @RequestParam Long restaurantUserId) {
        validateRole(role);
        RestaurantUserDTO updatedUser = restaurantUserService.addRestaurantUserRole(restaurantUserId, "ROLE_" + role.toUpperCase());
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Removes a specific role from a restaurant user.
     *
     * @param role            The role to remove.
     * @param restaurantUserId The ID of the restaurant user.
     * @return ResponseEntity containing the updated user details.
     */
    @Operation(summary = "Remove a role from a restaurant user", description = "Remove a specific role from an existing restaurant user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role removed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
            @ApiResponse(responseCode = "404", description = "User or role not found"),
            @ApiResponse(responseCode = "400", description = "Invalid role")
    })
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_' + #role.toUpperCase() + '_WRITE')")
    @PostMapping(value = "/remove_role")
    public ResponseEntity<RestaurantUserDTO> removeRoleFromRestaurantUser(
            @RequestParam String role,
            @RequestParam Long restaurantUserId) {
        validateRole(role);
        RestaurantUserDTO updatedUser = restaurantUserService.removeRestaurantUserRole(restaurantUserId, "ROLE_" + role.toUpperCase());
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
     * @param restaurantUserId The ID of the restaurant user to disable.
     * @return ResponseEntity with HTTP status.
     */
    @Operation(summary = "Disable a restaurant user", description = "Disable a restaurant user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User disabled successfully"),
            @ApiResponse(responseCode = "404", description = "User or restaurant not found")
    })
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MANAGER_WRITE')")
    @DeleteMapping(value = "/disable_user")
    public ResponseEntity<Void> disableRestaurantUser(@PathVariable Long restaurantUserId) {
        restaurantUserService.disableRestaurantUser(ControllerUtils.getCurrentRestaurantUser().getId(), restaurantUserId);
        return ResponseEntity.ok().build();
    }

    /**
     * Changes the user's password after verifying the old password.
     *
     * @param locale      The locale for response messages.
     * @param passwordDto DTO containing the old and new passwords.
     * @return GenericResponse indicating the result of the operation.
     */
    @Operation(summary = "Generate new token for password change", description = "Changes the user's password after verifying the old password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid old password or invalid data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(value = "/password/new_token")
    public GenericResponse changeUserPassword(
            @Parameter(description = "Locale for response messages") final Locale locale,
            @Parameter(description = "DTO containing the old and new password", required = true) @Valid UpdatePasswordDTO passwordDto) {
        if (!restaurantUserService.checkIfValidOldPassword(getRestaurantUserId(), passwordDto.getOldPassword())) {
            throw new InvalidOldPasswordException();
        }
        restaurantUserService.changeRestaurantUserPassword(getRestaurantUserId(), passwordDto.getNewPassword());
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
    public Long getRestaurantUserId() {
        return getCurrentRestaurantUser().getId();
    }

    /**
     * Retrieves the current authenticated restaurant user.
     *
     * @return The current authenticated restaurant user, or null if not authenticated.
     */
    private RestaurantUser getCurrentRestaurantUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof RestaurantUser) {
            return (RestaurantUser) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Adds a new user to a restaurant.
     */
    @PostMapping(value = "/new")
    @Operation(summary = "Add a user to a restaurant", description = "Add a new user to a restaurant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
            @ApiResponse(responseCode = "404", description = "Restaurant not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<RestaurantUserDTO> addRestaurantUserToRestaurant(
            @RequestBody NewRestaurantUserDTO restaurantUserDTO,
            @RequestParam Long restaurantId) {
        RestaurantUserDTO createdUser = restaurantUserService.addRestaurantUserToRestaurant(restaurantUserDTO, restaurantId);
        return new ResponseEntity<>(createdUser, HttpStatus.OK);
    }

    /**
     * Adds a new user with a specific role to a restaurant.
     */
    @PostMapping(value = "/new_with_role")
    @Operation(summary = "Add a user with a role to a restaurant", description = "Add a new user with a specific role to a restaurant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
            @ApiResponse(responseCode = "404", description = "Restaurant or role not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<RestaurantUserDTO> addRestaurantUserToRestaurantWithRole(
            @RequestBody NewRestaurantUserDTO restaurantUserDTO,
            @RequestParam Long restaurantId,
            @RequestParam String roleName) {
        RestaurantUserDTO createdUser = restaurantUserService.addRestaurantUserToRestaurantWithRole(restaurantUserDTO, restaurantId, roleName);
        return new ResponseEntity<>(createdUser, HttpStatus.OK);
    }

    /**
     * Retrieves details of the current restaurant user.
     */
    @Operation(summary = "Get restaurant user details", description = "Retrieve details of the current restaurant user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/get")
    public ResponseEntity<RestaurantUserDTO> getRestaurantUserDetails() {
        RestaurantUser currentUser = getCurrentRestaurantUser();
        if (currentUser == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        RestaurantUserDTO userDTO = new RestaurantUserDTO(currentUser);
        return new ResponseEntity<>(userDTO, HttpStatus.OK);
    }
}