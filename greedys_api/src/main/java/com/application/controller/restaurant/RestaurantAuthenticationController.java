package com.application.controller.restaurant;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.security.jwt.JwtUtil;
import com.application.service.authentication.RestaurantAuthenticationService;
import com.application.web.dto.get.RestaurantDTO;
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
            @ApiResponse(responseCode = "200", description = "List retrieved successfully", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantDTO.class, type = "array"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json"))
    })
    @GetMapping(value = "/restaurants", produces = "application/json")
    public ResponseEntity<?> restaurants(@Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        try {
            LOGGER.info("[DEBUG] Entered /restaurants endpoint");
            Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
            LOGGER.info("[DEBUG] Authentication object: {}", authentication);
            if (authentication == null || !authentication.isAuthenticated()) {
                LOGGER.warn("[DEBUG] No authenticated principal found");
                return ResponseEntity.status(401).body("Unauthorized: No authenticated principal found");
            }
            String hubEmail = null;
            Object principal = authentication.getPrincipal();
            LOGGER.info("[DEBUG] Principal class: {}", principal != null ? principal.getClass().getName() : "null");
            if (principal instanceof RestaurantUser) {
                RestaurantUser restaurantUser = (RestaurantUser) principal;
                hubEmail = restaurantUser.getEmail();
                LOGGER.info("[DEBUG] Extracted hubEmail from RestaurantUser: {}", hubEmail);
            } else {
                LOGGER.info("[DEBUG] Principal is not RestaurantUser, checking Authorization header");
                if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
                    LOGGER.warn("[DEBUG] Missing or invalid Authorization header");
                    return ResponseEntity.status(400).body("Bad Request: Missing or invalid Authorization header");
                }
                String hubJwt = authHeader.substring(7); // Remove 'Bearer '
                LOGGER.info("[DEBUG] Extracted JWT from header");
                Claims claims;
                try {
                    claims = jwtUtil.extractAllClaims(hubJwt);
                } catch (Exception ex) {
                    LOGGER.warn("[DEBUG] Invalid JWT: {}", ex.getMessage());
                    return ResponseEntity.status(401).body("Unauthorized: Invalid JWT");
                }
                String type = claims.get("type", String.class);
                LOGGER.info("[DEBUG] JWT type claim: {}", type);
                if (!"hub".equals(type)) {
                    LOGGER.warn("[DEBUG] JWT does not belong to a hub user");
                    return ResponseEntity.status(403).body("Forbidden: JWT does not belong to a hub user");
                }
                hubEmail = claims.get("email", String.class);
            }
            if (hubEmail == null) {
                LOGGER.error("[DEBUG] Hub ID not found in JWT or authentication principal");
                return ResponseEntity.status(400).body("Bad Request: Hub ID not found in JWT or authentication principal");
            }
            return ResponseEntity.ok(restaurantAuthenticationService.getRestaurantsForUserHub(hubEmail));
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve restaurants: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Internal Server Error: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_HUB')")
    @Operation(summary = "Select a restaurant after intermediate login", description = "Given a hub JWT and a restaurantId, returns a JWT for the selected restaurant user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Selection successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json"))
    })
    @GetMapping(value = "/select-restaurant", produces = "application/json")
    public ResponseEntity<?> selectRestaurant(@RequestParam Long restaurantId) {
        try {
            if (restaurantId == null || restaurantId <= 0) {
                LOGGER.warn("Invalid restaurantId: {}", restaurantId);
                return ResponseEntity.status(400).body("Bad Request: Invalid restaurantId");
            }
            AuthResponseDTO responseDTO = restaurantAuthenticationService.selectRestaurant(restaurantId);
            return ResponseEntity.ok(responseDTO);
        } catch (UnsupportedOperationException e) {
            LOGGER.error("Restaurant selection failed: {}", e.getMessage());
            return ResponseEntity.status(403).body("Forbidden: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            LOGGER.error("Bad request: {}", e.getMessage());
            return ResponseEntity.status(400).body("Bad Request: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Internal error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Internal Server Error: " + e.getMessage());
        }
    }

}
