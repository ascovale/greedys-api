package com.application.controller.restaurantUser;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.RestaurantUserService;
import com.application.web.dto.get.RestaurantUserDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "RestaurantUser", description = "Controller per la gestione degli utenti di un ristorante")
@RestController
@RequestMapping("/restaurant-user/{idRestaurantUser}/user")
//@PreAuthorize("@securityService.isRestaurantUserPermission(#idRestaurantUser)")
@SecurityRequirement(name = "bearerAuth")
public class RestaurantUserController {
    private final RestaurantUserService restaurantUserService;

    public RestaurantUserController(RestaurantUserService restaurantUserService) {
        this.restaurantUserService = restaurantUserService;
    }

    @Operation(summary = "Add a restaurant user as manager", description = "Aggiungi un utente di un ristorante come manager")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utente aggiunto come manager con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
            @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
    })
    @PreAuthorize("@securityService.hasRestaurantUserPrivilege(#idRestaurantUser, 'PRIVILEGE_ADD_MANAGER')")
    @PostMapping(value = "/add-manager/{idUser}")
    public ResponseEntity<RestaurantUserDTO> addRestaurantUserAsManager(@PathVariable Long idRestaurantUser,
            @PathVariable Long idUser) {
        RestaurantUserDTO updatedUser = restaurantUserService.addRestaurantUserRole(idRestaurantUser, idUser,
                "ROLE_MANAGER");
        return new ResponseEntity<RestaurantUserDTO>(updatedUser, HttpStatus.OK);
    }

    @Operation(summary = "Add a restaurant user as chef", description = "Aggiungi un utente di un ristorante come chef")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utente aggiunto come chef con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
            @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
    })
    @PreAuthorize("@securityService.hasRestaurantUserPrivilege(#idRestaurantUser, 'PRIVILEGE_ADD_CHEF')")
    @PostMapping(value = "/add-chef/{idUser}")
    public ResponseEntity<RestaurantUserDTO> addRestaurantUserAsChef(@PathVariable Long idRestaurantUser,
            @PathVariable Long idUser) {
        RestaurantUserDTO updatedUser = restaurantUserService.addRestaurantUserRole(idRestaurantUser, idUser,
                "ROLE_CHEF");
        return new ResponseEntity<RestaurantUserDTO>(updatedUser, HttpStatus.OK);
    }

    @Operation(summary = "Add a restaurant user as waiter", description = "Aggiungi un utente di un ristorante come cameriere")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utente aggiunto come cameriere con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
            @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
    })
    @PreAuthorize("@securityService.hasRestaurantUserPrivilege(#idRestaurantUser, 'PRIVILEGE_ADD_WAITER')")
    @PostMapping(value = "/add-waiter/{idUser}")
    public ResponseEntity<RestaurantUserDTO> addRestaurantUserAsWaiter(@PathVariable Long idRestaurantUser,
            @PathVariable Long idUser) {
        RestaurantUserDTO updatedUser = restaurantUserService.addRestaurantUserRole(idRestaurantUser, idUser,
                "ROLE_WAITER");
        return new ResponseEntity<RestaurantUserDTO>(updatedUser, HttpStatus.OK);
    }

    @Operation(summary = "Add a restaurant user as viewer", description = "Aggiungi un utente di un ristorante come visualizzatore")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utente aggiunto come visualizzatore con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
            @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
    })
    @PreAuthorize("@securityService.hasRestaurantUserPrivilege(#idRestaurantUser, 'PRIVILEGE_ADD_VIEWER')")
    @PostMapping(value = "/add-viewer/{idUser}")
    public ResponseEntity<RestaurantUserDTO> addRestaurantUserAsViewer(@PathVariable Long idRestaurantUser,
            @PathVariable Long idUser) {
        RestaurantUserDTO updatedUser = restaurantUserService.addRestaurantUserRole(idRestaurantUser, idUser,
                "ROLE_VIEWER");
        return new ResponseEntity<RestaurantUserDTO>(updatedUser, HttpStatus.OK);
    }

    @Operation(summary = "Disable a restaurant user", description = "Disabilita un utente di un ristorante")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utente disabilitato con successo"),
            @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
    })
    @DeleteMapping(value = "/disable-user/{idUser}")
    public ResponseEntity<Void> disableRestaurantUser(@PathVariable Long idRestaurantUser, @PathVariable Long idUser) {
        restaurantUserService.disableRestaurantUser(idRestaurantUser, idUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "Change a restaurant user role to chef", description = "Cambia il ruolo di un utente di un ristorante a chef")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ruolo cambiato a chef con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
            @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
    })
    @PreAuthorize("@securityService.hasRestaurantUserPrivilege(#idRestaurantUser, 'PRIVILEGE_CHANGE_ROLE_TO_CHEF')")
    @PutMapping(value = "/change-role/chef/{idUser}")
    public ResponseEntity<RestaurantUserDTO> changeRestaurantUserRoleToChef(@PathVariable Long idRestaurantUser,
            @PathVariable Long idUser) {
        RestaurantUserDTO updatedUser = restaurantUserService.changeRestaurantUserRole(idRestaurantUser, idUser,
                "ROLE_CHEF");
        return new ResponseEntity<RestaurantUserDTO>(updatedUser, HttpStatus.OK);
    }

    @Operation(summary = "Change a restaurant user role to waiter", description = "Cambia il ruolo di un utente di un ristorante a cameriere")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ruolo cambiato a cameriere con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
            @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
    })
    @PreAuthorize("@securityService.hasRestaurantUserPrivilege(#idRestaurantUser, 'PRIVILEGE_CHANGE_ROLE_TO_WAITER')")
    @PutMapping(value = "/change-role/waiter/{idUser}")
    public ResponseEntity<RestaurantUserDTO> changeRestaurantUserRoleToWaiter(@PathVariable Long idRestaurantUser,
            @PathVariable Long idUser) {
        RestaurantUserDTO updatedUser = restaurantUserService.changeRestaurantUserRole(idRestaurantUser, idUser,
                "ROLE_WAITER");
        return new ResponseEntity<RestaurantUserDTO>(updatedUser, HttpStatus.OK);
    }

    @Operation(summary = "Change a restaurant user role to viewer", description = "Cambia il ruolo di un utente di un ristorante a visualizzatore")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ruolo cambiato a visualizzatore con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
            @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
    })
    @PreAuthorize("@securityService.hasRestaurantUserPrivilege(#idRestaurantUser, 'PRIVILEGE_CHANGE_ROLE_TO_VIEWER')")
    @PutMapping(value = "/change-role/viewer/{idUser}")
    public ResponseEntity<RestaurantUserDTO> changeRestaurantUserRoleToViewer(@PathVariable Long idRestaurantUser,
            @PathVariable Long idUser) {
        RestaurantUserDTO updatedUser = restaurantUserService.changeRestaurantUserRole(idRestaurantUser, idUser,
                "ROLE_VIEWER");
        return new ResponseEntity<RestaurantUserDTO>(updatedUser, HttpStatus.OK);
    }

    @Operation(summary = "Change a restaurant user role to manager", description = "Cambia il ruolo di un utente di un ristorante a manager")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ruolo cambiato a manager con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
            @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
    })
    @PreAuthorize("@securityService.hasRestaurantUserPrivilege(#idRestaurantUser, 'PRIVILEGE_CHANGE_ROLE_TO_MANAGER')")
    @PutMapping(value = "/change-role/manager/{idUser}")
    public ResponseEntity<RestaurantUserDTO> changeRestaurantUserRoleToManager(@PathVariable Long idRestaurantUser,
            @PathVariable Long idUser) {
        RestaurantUserDTO updatedUser = restaurantUserService.changeRestaurantUserRole(idRestaurantUser, idUser,
                "ROLE_MANAGER");
        return new ResponseEntity<RestaurantUserDTO>(updatedUser, HttpStatus.OK);
    }
}
