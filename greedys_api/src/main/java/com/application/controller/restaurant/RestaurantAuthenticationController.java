package com.application.controller.restaurant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.security.jwt.JwtUtil;
import com.application.service.authentication.RestaurantAuthenticationService;
import com.application.web.dto.post.AuthResponseDTO;
import com.application.web.dto.post.RestaurantUserSelectRequestDTO;

import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Restaurant Authentication", description = "Controller for restaurant authentication")
@RequestMapping("/restaurant/user/auth")
@SecurityRequirement(name = "restaurantBearerAuth")
public class RestaurantAuthenticationController {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final JwtUtil jwtUtil;

    private final RestaurantAuthenticationService restaurantAuthenticationService;

    public RestaurantAuthenticationController(
            RestaurantAuthenticationService restaurantAuthenticationService, JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        this.restaurantAuthenticationService = restaurantAuthenticationService;
    }

    @Operation(summary = "Get list of restaurants for hub user", description = "Given a hub JWT, returns the list of restaurants associated with the hub user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json"))
    })
    //@PreAuthorize("@securityRestaurantUserService.isHubUser()")
    @PostMapping(value = "/restaurants", produces = "application/json")
    public ResponseEntity<?> restaurants( @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
                throw new IllegalArgumentException("Missing or invalid Authorization header");
            }
            String hubJwt = authHeader.substring(7); // Remove 'Bearer '
            Claims claims = jwtUtil.extractAllClaims(hubJwt);
            String type = claims.get("type", String.class);
            if (!"hub".equals(type)) {
                throw new IllegalArgumentException("JWT does not belong to a hub user");
            }
            Long hubId = claims.get("hubId", Long.class);

            return ResponseEntity.ok(restaurantAuthenticationService.getRestaurantsByUserHubId(hubId));
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve restaurants: {}", e.getMessage());
            return ResponseEntity.status(401).body("Failed to retrieve restaurants: " + e.getMessage());
        }
    }

    @Operation(summary = "Select a restaurant after intermediate login", description = "Given a hub JWT and a restaurantId, returns a JWT for the selected restaurant user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Selection successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Selection failed", content = @Content(mediaType = "application/json"))
    })
    @PreAuthorize("@securityRestaurantUserService.hasPermissionForRestaurant(#selectRequest.restaurantId)")
    @PostMapping(value = "/select-restaurant", produces = "application/json")
    public ResponseEntity<?> selectRestaurant(@RequestBody RestaurantUserSelectRequestDTO selectRequest) {
        try {
            AuthResponseDTO responseDTO = restaurantAuthenticationService.selectRestaurant(selectRequest);
            return ResponseEntity.ok(responseDTO);
        } catch (UnsupportedOperationException e) {
            LOGGER.error("Restaurant selection failed: {}", e.getMessage());
            return ResponseEntity.status(401).body("Restaurant selection failed: " + e.getMessage());
        }
    }

}
