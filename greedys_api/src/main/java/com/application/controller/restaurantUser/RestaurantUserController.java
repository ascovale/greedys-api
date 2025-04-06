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

import com.application.controller.utils.ControllerUtils;
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
@RequestMapping("/restaurant/user")
@SecurityRequirement(name = "restaurantBearerAuth")
public class RestaurantUserController {
        private final RestaurantUserService restaurantUserService;

        public RestaurantUserController(RestaurantUserService restaurantUserService) {
                this.restaurantUserService = restaurantUserService;
        }
        //TODO aggiungere il metodo aggiungi restaurant user che prende anche il ruolo
        
        @Operation(summary = "Add a restaurant user as manager", description = "Aggiungi un utente di un ristorante come manager")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Utente aggiunto come manager con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
        })
        @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MANAGER_WRITE')")
        @PostMapping(value = "/add_manager")
        public ResponseEntity<RestaurantUserDTO> addRestaurantUserAsManager(@PathVariable Long restaurantUserId) {
                RestaurantUserDTO updatedUser = restaurantUserService.addRestaurantUserRole(restaurantUserId,
                                "ROLE_MANAGER");
                return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        }

        @Operation(summary = "Add a restaurant user as chef", description = "Aggiungi un utente di un ristorante come chef")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Utente aggiunto come chef con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
        })
        @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_CHEF_WRITE')")
        @PostMapping(value = "/add_chef")
        public ResponseEntity<RestaurantUserDTO> addRestaurantUserAsChef(
                        @PathVariable Long restaurantUserId) {
                RestaurantUserDTO updatedUser = restaurantUserService.addRestaurantUserRole(
                                ControllerUtils.getCurrentRestaurantUser().getId(),
                                "ROLE_CHEF");
                return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        }

        @Operation(summary = "Add a restaurant user as waiter", description = "Aggiungi un utente di un ristorante come cameriere")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Utente aggiunto come cameriere con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
        })
        @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_WAITER_WRITE')")
        @PostMapping(value = "/add_waiter")
        public ResponseEntity<RestaurantUserDTO> addRestaurantUserAsWaiter(
                        @PathVariable Long restaurantUserId) {
                RestaurantUserDTO updatedUser = restaurantUserService.addRestaurantUserRole(
                                ControllerUtils.getCurrentRestaurantUser().getId(),
                                "ROLE_WAITER");
                return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        }

        @Operation(summary = "Add a restaurant user as viewer", description = "Aggiungi un utente di un ristorante come visualizzatore")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Utente aggiunto come visualizzatore con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
        })
        @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_VIEWER_WRITE')")
        @PostMapping(value = "/add_viewer")
        public ResponseEntity<RestaurantUserDTO> addRestaurantUserAsViewer(
                        @PathVariable Long restaurantUserId) {
                RestaurantUserDTO updatedUser = restaurantUserService.addRestaurantUserRole(
                                ControllerUtils.getCurrentRestaurantUser().getId(),
                                "ROLE_VIEWER");
                return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        }

        @Operation(summary = "Disable a restaurant user", description = "Disabilita un utente di un ristorante")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Utente disabilitato con successo"),
                        @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
        })
        @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MANAGER_WRITE')")
        @DeleteMapping(value = "/disable_user")
        public ResponseEntity<Void> disableRestaurantUser(@PathVariable Long restaurantUserId) {
                restaurantUserService.disableRestaurantUser(ControllerUtils.getCurrentRestaurantUser().getId(),
                                restaurantUserId);
                return new ResponseEntity<>(HttpStatus.OK);
        }

        @Operation(summary = "Change a restaurant user role to chef", description = "Cambia il ruolo di un utente di un ristorante a chef")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Ruolo cambiato a chef con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
        })
        @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MANAGER_WRITE')")
        @PutMapping(value = "/change_role/chef")
        public ResponseEntity<RestaurantUserDTO> changeRestaurantUserRoleToChef(
                        @PathVariable Long restaurantUserId) {
                RestaurantUserDTO updatedUser = restaurantUserService.changeRestaurantUserRole(
                                ControllerUtils.getCurrentRestaurantUser().getId(), restaurantUserId,
                                "ROLE_CHEF");
                return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        }

        @Operation(summary = "Change a restaurant user role to waiter", description = "Cambia il ruolo di un utente di un ristorante a cameriere")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Ruolo cambiato a cameriere con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Utente o ristorante non trovato")
        })
        @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_ROLE_WRITE')")
        @PutMapping(value = "/change_role/waiter")
        public ResponseEntity<RestaurantUserDTO> changeRestaurantUserRoleToWaiter(
                        @PathVariable Long restaurantUserId) {
                RestaurantUserDTO updatedUser = restaurantUserService.changeRestaurantUserRole(
                                ControllerUtils.getCurrentRestaurantUser().getId(), restaurantUserId,
                                "ROLE_WAITER");
                return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        }

}