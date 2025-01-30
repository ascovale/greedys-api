package com.application.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.RestaurantService;
import com.application.service.RestaurantUserService;
import com.application.service.UserService;
import com.application.web.dto.AllergyDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequestMapping("/admin/user")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin RestaurantUser", description = "Admin management APIs for the RestaurantUser")
public class AdminUserRestaurantController {
    private final RestaurantUserService restaurantUserService;

    @Autowired
    public AdminUserRestaurantController(RestaurantUserService restaurantUserService) {
        this.restaurantUserService = restaurantUserService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Block restaurant user", description = "Blocks a restaurant user by their ID")
    @ApiResponse(responseCode = "200", description = "Restaurant user blocked successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/restaurantUser/{idRestaurantUser}/block")
    public GenericResponse blockRestaurantUser(@PathVariable Long idRestaurantUser) {
        restaurantUserService.blockRestaurantUser(idRestaurantUser);
        return new GenericResponse("Restaurant user blocked successfully");
    }
    /* 
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove restaurant user", description = "Removes a restaurant user by their ID")
    @ApiResponse(responseCode = "200", description = "Restaurant user removed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/restaurant/{idRestaurant}/removeUser")
    public GenericResponse removeRestaurantUser(@PathVariable Long idRestaurantUser) {
        restaurantUserService.removeRestaurantUser(idRestaurantUser);
        return new GenericResponse("Restaurant user removed successfully");
    }*/

    // TODO creare altre classi controller per la gestione
    // Creare le notifiche per l'admin quando viene aggiunto un ristorante cosi che
    // abilita un ristorante dopo che ha verificato i vari dati forniti
    // disabilita un utente di un ristorante
    // cambia password ad un ristorante
    // rimuovi recensioni successivamente
    // Creare la recensione interna ed esterna
    // voto del recensore
    // sia del ristorante sia del pubblico che ne da credibilità
}
