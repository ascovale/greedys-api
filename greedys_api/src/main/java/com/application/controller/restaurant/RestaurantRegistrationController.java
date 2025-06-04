package com.application.controller.restaurant;

import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.RestaurantService;
import com.application.service.authentication.RestaurantAuthenticationService;
import com.application.web.dto.post.AuthResponseDTO;
import com.application.web.dto.post.NewRestaurantDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@Tag(name = "Resttaurant Registration", description = "Controller for restaurant registration")
@RequestMapping("/restaurant/register")
@SecurityRequirement(name = "bearerAuth")
public class RestaurantRegistrationController {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private RestaurantService restaurantService;
    private final RestaurantAuthenticationService restaurantAuthenticationService;

    public RestaurantRegistrationController(RestaurantService restaurantService,
            RestaurantAuthenticationService restaurantAuthenticationService) {
        this.restaurantService = restaurantService;
        this.restaurantAuthenticationService = restaurantAuthenticationService;
    }

    @Bean
    public AuthenticationManager noOpAuthenticationManager() {
        return authentication -> {
            throw new UnsupportedOperationException("No global AuthenticationManager configured");
        };
    }

    // Public API methods
    @Operation(summary = "Request to register a new restaurant", description = "Request to register a new restaurant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Restaurant registered successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = NewRestaurantDTO.class)) }),
    })
    @PostMapping(value = "/new")
    public GenericResponse registerRestaurant(@RequestBody NewRestaurantDTO restaurantDto) {
        LOGGER.debug("Registering restaurant with information:", restaurantDto);
        System.out.println("Registering restaurant with information:" + restaurantDto.getName());
        restaurantService.registerRestaurant(restaurantDto);
        return new GenericResponse("success");
    }

    @GetMapping(value = "/resend_token")
    @ResponseBody
    public GenericResponse resendRegistrationToken(final HttpServletRequest request,
            @RequestParam("token") final String existingToken) {
        return restaurantAuthenticationService.resendRegistrationToken(request, existingToken);
    }

    @Operation(summary = "Request password reset", description = "Sends a password reset token to the restaurant user's email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset token sent successfully", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json"))
    })
    @PostMapping(value = "/password/forgot")
    public ResponseEntity<String> forgotPassword(@RequestParam("email") final String userEmail,
            final HttpServletRequest request) {
        return restaurantAuthenticationService.forgotPassword(userEmail, request);
    }

    @Operation(summary = "Change restaurant and get a new JWT", description = "Switches the restaurant and returns a new JWT for the specified restaurant ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "JWT generated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Restaurant not found", content = @Content(mediaType = "application/json"))
    })
    @PreAuthorize("@securityRestaurantUserService.hasPermissionForRestaurant(#restaurantId)")
    @PostMapping(value = "/change-restaurant", produces = "application/json")
    public AuthResponseDTO changeRestaurant(@RequestParam Long restaurantId) {
        return restaurantAuthenticationService.changeRestaurant(restaurantId);
    }

    @Operation(summary = "Confirm restaurant user registration", description = "Conferma la registrazione")
    @GetMapping(value = "/confirm")
    public String confirmRestaurantUserRegistration(final HttpServletRequest request, final Model model,
            @RequestParam final String token) throws UnsupportedEncodingException {
        return restaurantAuthenticationService.confirmRestaurantUserRegistration(request, model, token);
    }

    @Operation(summary = "Confirm password change with token", description = "Confirms the password change using a token")
    @ApiResponse(responseCode = "200", description = "Password changed successfully or invalid token", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PutMapping(value = "/password/confirm")
    public String confirmPasswordChange(
            @Parameter(description = "Password reset token") @RequestParam final String token) {
        return restaurantAuthenticationService.confirmPasswordChange(token);
    }
    /*
     * 
     * public void authWithHttpServletRequest(HttpServletRequest request, String
     * username, String password) {
     * try {
     * request.login(username, password);
     * } catch (ServletException e) {
     * LOGGER.error("Error while login ", e);
     * }
     * }
     * 
     * public void authWithoutPassword(RestaurantUser restaurantUser) {
     * List<RestaurantPrivilege> privileges =
     * restaurantUser.getPrivileges().stream().collect(Collectors.toList());
     * List<GrantedAuthority> authorities = privileges.stream().map(p -> new
     * SimpleGrantedAuthority(p.getName()))
     * .collect(Collectors.toList());
     * 
     * Authentication authentication = new
     * UsernamePasswordAuthenticationToken(restaurantUser, null, authorities);
     * 
     * SecurityContextHolder.getContext().setAuthentication(authentication);
     * }
     */
}
