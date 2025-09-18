package com.application.restaurant.controller;

import java.io.UnsupportedEncodingException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.service.RestaurantService;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.restaurant.RestaurantDTO;
import com.application.common.web.dto.security.AuthResponseDTO;
import com.application.restaurant.service.authentication.RestaurantAuthenticationService;
import com.application.restaurant.web.dto.restaurant.NewRestaurantDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Tag(name = "Restaurant Registration", description = "Controller for restaurant registration")
@RequestMapping("/restaurant/register")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class RestaurantRegistrationController extends BaseController {

    private final RestaurantService restaurantService;
    private final RestaurantAuthenticationService restaurantAuthenticationService;

    @Operation(summary = "Request to register a new restaurant", description = "Request to register a new restaurant")
    @PostMapping(value = "/new")
    public ResponseEntity<ResponseWrapper<RestaurantDTO>> registerRestaurant(@RequestBody NewRestaurantDTO restaurantDto) {
        return executeCreate("register restaurant", "Restaurant registered successfully", () -> {
            log.debug("Registering restaurant with information:", restaurantDto);
            return restaurantService.registerRestaurant(restaurantDto);
            
        });
    }
    //TODO richiesta verifica email
    
    @Operation(summary = "Resend registration token", description = "Resends the registration token")
    @GetMapping(value = "/resend_token")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<String>> resendRegistrationToken(final HttpServletRequest request,
            @RequestParam("token") final String existingToken) {
        return executeVoid("resend registration token", () -> 
            restaurantAuthenticationService.resendRegistrationToken(request, existingToken));
    }

    @Operation(summary = "Request password reset", description = "Sends a password reset token to the restaurant user's email")
    @PostMapping(value = "/password/forgot")
    public ResponseEntity<ResponseWrapper<String>> forgotPassword(@RequestParam("email") final String userEmail,
            final HttpServletRequest request) {
        return execute("forgot password", () -> {
            ResponseEntity<String> response = restaurantAuthenticationService.forgotPassword(userEmail, request);
            return response.getBody();
        });
    }

    @Operation(summary = "Change restaurant and get a new JWT", description = "Switches the restaurant and returns a new JWT for the specified restaurant ID")
    @PreAuthorize("@securityRUserService.hasPermissionForRestaurant(#restaurantId)")
    @PostMapping(value = "/change-restaurant", produces = "application/json")
    public ResponseEntity<ResponseWrapper<AuthResponseDTO>> changeRestaurant(@RequestParam Long restaurantId) {
        return execute("change restaurant", () -> restaurantAuthenticationService.changeRestaurant(restaurantId));
    }

    @Operation(summary = "Confirm restaurant user registration", description = "Conferma la registrazione")
    @GetMapping(value = "/confirm")
    public ResponseEntity<ResponseWrapper<String>> confirmRUserRegistration(final HttpServletRequest request,
            @RequestParam final String token) throws UnsupportedEncodingException {
        return execute("confirm registration", () -> {
            // Esegue la conferma e ritorna un messaggio di successo
            restaurantAuthenticationService.confirmRUserRegistration(request, null, token);
            return "Registration confirmed successfully";
        });
    }

    @Operation(summary = "Confirm password change with token", description = "Confirms the password change using a token")
    @PutMapping(value = "/password/confirm")
    public ResponseEntity<ResponseWrapper<String>> confirmPasswordChange(
            @Parameter(description = "Password reset token") @RequestParam final String token) {
        return execute("confirm password change", () -> restaurantAuthenticationService.confirmPasswordChange(token));
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
     * public void authWithoutPassword(RUser RUser) {
     * List<RestaurantPrivilege> privileges =
     * RUser.getPrivileges().stream().collect(Collectors.toList());
     * List<GrantedAuthority> authorities = privileges.stream().map(p -> new
     * SimpleGrantedAuthority(p.getName()))
     * .collect(Collectors.toList());
     * 
     * Authentication authentication = new
     * UsernamePasswordAuthenticationToken(RUser, null, authorities);
     * 
     * SecurityContextHolder.getContext().setAuthentication(authentication);
     * }
     */
}
