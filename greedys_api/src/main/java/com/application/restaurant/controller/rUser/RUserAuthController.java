package com.application.restaurant.controller.rUser;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.web.dto.AuthRequestGoogleDTO;
import com.application.common.web.dto.post.AuthRequestDTO;
import com.application.common.web.dto.post.AuthResponseDTO;
import com.application.restaurant.service.authentication.RestaurantAuthenticationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Tag(name = "Restaurant User Authentication", description = "Controller for restaurant user authentication")
@RequestMapping("/restaurant/user/auth")
@RequiredArgsConstructor
@Slf4j
public class RUserAuthController {

    private final RestaurantAuthenticationService restaurantAuthenticationService;

    @Operation(summary = "Generate an authentication token", description = "Authenticates a user and returns a JWT token or a selection token if multiple restaurants are available")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),

            @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(mediaType = "application/json"))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Authentication request", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthRequestDTO.class)))
    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequestDTO authenticationRequest) {
        try {
            return ResponseEntity.ok(restaurantAuthenticationService.loginWithHubSupport(authenticationRequest));
        } catch (UnsupportedOperationException e) {
            log.error("Authentication failed: {}", e.getMessage());
            return ResponseEntity.status(401).body("Authentication failed: Invalid username or password.");
        }
    }

    @Operation(summary = "Authenticate with Google", description = "Authenticates a restaurant user hub using Google OAuth2")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Google authentication successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Google authentication failed", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/google")
    public ResponseEntity<?> authenticateWithGoogle(@RequestBody AuthRequestGoogleDTO authRequest) {
        try {
            AuthResponseDTO response = restaurantAuthenticationService.loginWithGoogle(authRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Google authentication failed: {}", e.getMessage());
            return ResponseEntity.status(401).body("Google authentication failed: " + e.getMessage());
        }
    }
    
}
