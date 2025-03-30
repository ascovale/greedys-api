package com.application.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.service.RestaurantUserService;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequestMapping("/admin/restaurant/user")
@RestController
@SecurityRequirement(name = "adminBearerAuth")
@Tag(name = "Admin RestaurantUser", description = "Admin management APIs for the RestaurantUser")
public class AdminRestaurantUserController {
    //TODO: aggiungere ruoli e permessi ai ruoli come metodi

    //TODO Forse manca il delete restaurant user e quindi anche altri utenti

    private final RestaurantUserService restaurantUserService;

    @Autowired
    public AdminRestaurantUserController(RestaurantUserService restaurantUserService) {
        this.restaurantUserService = restaurantUserService;
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

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_USER_WRITE')")
    @Operation(summary = "Change restaurant owner", description = "Changes the owner of a restaurant")
    @ApiResponse(responseCode = "200", description = "Restaurant owner changed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/{idRestaurant}/changeOwner/{idOldOwner}/{idNewOwner}")
    public GenericResponse changeRestaurantOwner(@PathVariable Long idRestaurant, @PathVariable Long idOldOwner,
            @PathVariable Long idNewOwner) {
                //TODO updateRestaurantUserStatus
        restaurantUserService.changeRestaurantOwner(idRestaurant, idOldOwner, idNewOwner);
        return new GenericResponse("Restaurant owner changed successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_SWITCH_TO_RESTAURANT_USER')")
    @Operation(summary = "Switch to restaurant user", description = "Switches to restaurant user mode")
    @ApiResponse(responseCode = "200", description = "Switched to restaurant user mode successfully")
    @GetMapping("/switch_to_restaurantUser")
    public String switchToRestaurantUser() {
        return "redirect:/admin/home";
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_SWITCH_TO_RESTAURANT_USER')")
    @Operation(summary = "Exit restaurant user", description = "Exits the restaurant user mode and redirects to admin home")
    @ApiResponse(responseCode = "200", description = "Exited restaurant user mode successfully")
    @GetMapping("/exit_restaurantUser")
    public String exitRestaurantUser() {
        return "redirect:/admin/home";
    }

}
