package com.application.controller.restaurantUser;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.controller.utils.ControllerUtils;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.service.RestaurantService;
import com.application.service.RestaurantUserService;
import com.application.web.dto.get.RestaurantUserDTO;
import com.application.web.dto.post.NewRestaurantUserDTO;
import com.application.web.dto.put.UpdatePasswordDTO;
import com.application.web.error.InvalidOldPasswordException;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "RestaurantUser", description = "Controller per la gestione degli utenti di un ristorante")
@RestController
@RequestMapping("/restaurant/user")
@SecurityRequirement(name = "restaurantBearerAuth")
public class RestaurantUserController {
        private final RestaurantUserService restaurantUserService;
        private final MessageSource messages;
        private final RestaurantService restaurantService;

        public RestaurantUserController(RestaurantUserService restaurantUserService, MessageSource messages, RestaurantService restaurantService) {
                this.messages = messages;
                this.restaurantUserService = restaurantUserService;
                this.restaurantService = restaurantService;
        }
        // TODO aggiungere il metodo aggiungi restaurant user che prende anche il ruolo

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

        @Operation(summary = "Generate new token for password change", description = "Changes the user's password after verifying the old password")
        @ApiResponse(responseCode = "200", description = "Password changed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
        @ApiResponse(responseCode = "400", description = "Invalid old password or invalid data")
        @ApiResponse(responseCode = "401", description = "Unauthorized")
        @PostMapping(value = "/password/new_token")
        public GenericResponse changeUserPassword(
                        @Parameter(description = "Locale for response messages") final Locale locale,
                        @Parameter(description = "DTO containing the old and new password", required = true) @Valid UpdatePasswordDTO passwordDto) {
                if (!restaurantUserService.checkIfValidOldPassword(getRestaurantUserId(),
                                passwordDto.getOldPassword())) {
                        throw new InvalidOldPasswordException();
                }
                restaurantUserService.changeRestaurantUserPassword(getRestaurantUserId(), passwordDto.getNewPassword());
                return new GenericResponse(messages.getMessage("message.updatePasswordSuc", null, locale));
        }

        @Operation(summary = "Get restaurant user ID", description = "Retrieves the ID of the current restaurant user", responses = {
                        @ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Long.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Access denied"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        @GetMapping("/id")
        public Long getRestaurantUserId() {
                return getCurrentRestaurantUser().getId();
        }
   
        private RestaurantUser getCurrentRestaurantUser() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof RestaurantUser) {
                        return (RestaurantUser) authentication.getPrincipal();
                }
                return null;
        }

                //TODO: Verificare i preauthorize

	@PostMapping(value = "/new")
	@Operation(summary = "Add a user to a restaurant", description = "Add a new user to a restaurant", tags = {
			"restaurant-api" })
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

                //TODO: Verificare i preauthorize

	@PostMapping(value = "/new_with_role")
	@Operation(summary = "Add a user with a role to a restaurant", description = "Add a new user with a specific role to a restaurant", tags = {
			"restaurant-api" })
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