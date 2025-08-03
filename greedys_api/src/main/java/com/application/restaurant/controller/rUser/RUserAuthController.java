package com.application.restaurant.controller.rUser;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.security.AuthRequestDTO;
import com.application.common.web.dto.security.AuthRequestGoogleDTO;
import com.application.common.web.dto.security.AuthResponseDTO;
import com.application.restaurant.service.authentication.RestaurantAuthenticationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Tag(name = "Restaurant User Authentication", description = "Controller for restaurant user authentication")
@RequestMapping("/restaurant/user/auth")
@RequiredArgsConstructor
@Slf4j
public class RUserAuthController extends BaseController {

    private final RestaurantAuthenticationService restaurantAuthenticationService;

    @Operation(summary = "Generate an authentication token", description = "Authenticates a user and returns a JWT token or a selection token if multiple restaurants are available")
    @PostMapping(value = "/login", produces = "application/json")
    public ResponseWrapper<AuthResponseDTO> createAuthenticationToken(@RequestBody AuthRequestDTO authenticationRequest) {
        return execute("authenticate user", () -> {
            try {
                return restaurantAuthenticationService.loginWithHubSupport(authenticationRequest);
            } catch (UnsupportedOperationException e) {
                throw new SecurityException("Authentication failed: Invalid username or password.");
            }
        });
    }

    @Operation(summary = "Authenticate with Google", description = "Authenticates a restaurant user hub using Google OAuth2")
    @PostMapping("/google")
    public ResponseWrapper<AuthResponseDTO> authenticateWithGoogle(@RequestBody AuthRequestGoogleDTO authRequest) {
        return execute("google authentication", () -> 
            restaurantAuthenticationService.loginWithGoogle(authRequest));
    }
}
