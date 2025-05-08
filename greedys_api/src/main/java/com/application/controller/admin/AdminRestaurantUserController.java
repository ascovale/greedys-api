package com.application.controller.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.service.RestaurantUserService;
import com.application.service.authentication.RestaurantAuthenticationService;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RequestMapping("/admin/restaurant/user")
@RestController
@SecurityRequirement(name = "adminBearerAuth")
@Tag(name = "8. Restaurant User", description = "Admin management APIs for the RestaurantUser")
public class AdminRestaurantUserController {
    // TODO: aggiungere ruoli e permessi ai ruoli come metodi

    // TODO Forse manca il delete restaurant user e quindi anche altri utenti

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminRestaurantUserController.class);
    private final RestaurantUserService restaurantUserService;
    private final RestaurantAuthenticationService restaurantAuthenticationService;

    @Autowired
    public AdminRestaurantUserController(RestaurantUserService restaurantUserService, RestaurantAuthenticationService restaurantAuthenticationService) {
        this.restaurantUserService = restaurantUserService;
        this.restaurantAuthenticationService = restaurantAuthenticationService;
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_USER_WRITE')")
    @Operation(summary = "Block restaurant user", description = "Blocks a restaurant user by their ID")
    @ApiResponse(responseCode = "200", description = "Restaurant user blocked successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/{restaurantUserId}/block")
    public GenericResponse blockRestaurantUser(@PathVariable Long restaurantUserId) {
        restaurantUserService.updateRestaurantUserStatus(restaurantUserId, RestaurantUser.Status.BLOCKED);
        return new GenericResponse("Restaurant user blocked successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_USER_WRITE')")
    @Operation(summary = "Enable restaurant user", description = "Enables a restaurant user by their ID")
    @ApiResponse(responseCode = "200", description = "Restaurant user enabled successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/{restaurantUserId}/enable")
    public GenericResponse enableRestaurantUser(@PathVariable Long restaurantUserId) {
        restaurantUserService.updateRestaurantUserStatus(restaurantUserId, RestaurantUser.Status.ENABLED);
        return new GenericResponse("Restaurant user enabled successfully");
    }

    // TODO: perchè voglio dire idOldOwner
    // scrivere newOwnerId
    // non basta passare forse bisogna cambiare solo mail?

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_USER_WRITE')")
    @Operation(summary = "Change restaurant owner", description = "Changes the owner of a restaurant")
    @ApiResponse(responseCode = "200", description = "Restaurant owner changed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/{restaurantId}/changeOwner/{idOldOwner}/{idNewOwner}")
    public GenericResponse changeRestaurantOwner(@PathVariable Long restaurantId, @PathVariable Long idOldOwner,
            @PathVariable Long idNewOwner) {
        // TODO updateRestaurantUserStatus
        restaurantUserService.changeRestaurantOwner(restaurantId, idOldOwner, idNewOwner);
        return new GenericResponse("Restaurant owner changed successfully");
    }

    //@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_SWITCH_TO_RESTAURANT_USER')")
    @GetMapping("/login/{restaurantUserId}")
    @Operation(summary = "Get JWT Token of a restaurant user", description = "Returns the JWT token of a restaurant user")
    public ResponseEntity<?> loginHasRestaurantUser(@PathVariable Long restaurantUserId, HttpServletRequest request) {
        try {
            return ResponseEntity.ok(restaurantAuthenticationService.adminLoginToRestaurantUser(restaurantUserId, request));
        } catch (UnsupportedOperationException e) {
            LOGGER.error("Authentication failed: {}", e.getMessage());
            return ResponseEntity.status(401).body("Authentication failed: Invalid username or password.");
        }
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_USER_READ')")
    @Operation(summary = "Get restaurant users", description = "Retrieves the list of users for a specific restaurant")
    @ApiResponse(responseCode = "200", description = "List of restaurant users retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUser.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @GetMapping("/{restaurantId}/users")
    public ResponseEntity<?> getRestaurantUsers(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(restaurantUserService.getRestaurantUsersByRestaurantId(restaurantId));
    }

}
