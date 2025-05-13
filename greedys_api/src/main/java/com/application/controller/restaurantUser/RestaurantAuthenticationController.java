package com.application.controller.restaurantUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.authentication.RestaurantAuthenticationService;
import com.application.web.dto.RestaurantUserAuthResponseDTO;
import com.application.web.dto.post.RestaurantUserAuthRequestDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Restaurant Authentication", description = "Controller for restaurant creation and user authentication")
@RequestMapping("/restaurant/user/auth")
@SecurityRequirement(name = "bearerAuth")
public class RestaurantAuthenticationController {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final RestaurantAuthenticationService restaurantAuthenticationService;

    public RestaurantAuthenticationController(
            RestaurantAuthenticationService restaurantAuthenticationService) {
        this.restaurantAuthenticationService = restaurantAuthenticationService;
    }


    @Operation(summary = "Generate an authentication token", description = "Authenticates a user and returns a JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserAuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(mediaType = "application/json"))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Authentication request", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantUserAuthRequestDTO.class)))
    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody RestaurantUserAuthRequestDTO authenticationRequest) {
        try {
            RestaurantUserAuthResponseDTO responseDTO = restaurantAuthenticationService.login(authenticationRequest);
            return ResponseEntity.ok(responseDTO);
        } catch (UnsupportedOperationException e) {
            LOGGER.error("Authentication failed: {}", e.getMessage());
            return ResponseEntity.status(401).body("Authentication failed: Invalid username or password.");
        }
    }
}
