package com.application.restaurant.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.security.jwt.JwtUtil;
import com.application.common.web.ApiResponse;
import com.application.common.web.dto.restaurant.RestaurantDTO;
import com.application.common.web.dto.security.AuthResponseDTO;
import com.application.common.web.dto.security.RefreshTokenRequestDTO;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.service.authentication.RestaurantAuthenticationService;

import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@RestController
@Tag(name = "Restaurant Authentication", description = "Controller for restaurant authentication")
@RequestMapping("/restaurant/user/auth")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class RestaurantAuthenticationController extends BaseController {

    private final JwtUtil jwtUtil;
    private final RestaurantAuthenticationService restaurantAuthenticationService;

    @Operation(summary = "Get list of restaurants for hub user", description = "Given a hub JWT, returns the list of restaurants associated with the hub user")
    @GetMapping(value = "/restaurants", produces = "application/json")
    @ReadApiResponses
    public ResponseEntity<ApiResponse<List<RestaurantDTO>>> restaurants(@Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return execute("get restaurants for hub user", () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new IllegalStateException("No authenticated principal found");
            }
            
            String hubEmail = extractHubEmail(authentication, authHeader);
            return restaurantAuthenticationService.getRestaurantsForUserHub(hubEmail);
        });
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_HUB')")
    @Operation(summary = "Select a restaurant after intermediate login", description = "Given a hub JWT and a restaurantId, returns a JWT for the selected restaurant user")
    @GetMapping(value = "/select-restaurant", produces = "application/json")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> selectRestaurant(@RequestParam Long restaurantId) {
        return execute("select restaurant", () -> {
            if (restaurantId == null || restaurantId <= 0) {
                throw new IllegalArgumentException("Invalid restaurantId: " + restaurantId);
            }
            return restaurantAuthenticationService.selectRestaurant(restaurantId);
        });
    }

    private String extractHubEmail(Authentication authentication, String authHeader) throws Exception {
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof RUser) {
            RUser rUser = (RUser) principal;
            return rUser.getEmail();
        } else {
            if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
                throw new IllegalArgumentException("Missing or invalid Authorization header");
            }
            
            String hubJwt = authHeader.substring(7);
            Claims claims = jwtUtil.extractAllClaims(hubJwt);
            String type = claims.get("type", String.class);
            
            if (!"hub".equals(type)) {
                throw new SecurityException("JWT does not belong to a hub user");
            }
            
            return claims.get("email", String.class);
        }
    }

    @Operation(summary = "Refresh hub token", description = "Refresh a hub JWT token using a hub refresh token")
    @PostMapping(value = "/refresh/hub", produces = "application/json")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refreshHubToken(@RequestBody RefreshTokenRequestDTO refreshRequest) {
        return execute("refresh hub token", () -> 
            restaurantAuthenticationService.refreshHubToken(refreshRequest.getRefreshToken())
        );
    }

    @Operation(summary = "Refresh restaurant user token", description = "Refresh a restaurant user JWT token using a refresh token")
    @PostMapping(value = "/refresh", produces = "application/json")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refreshRUserToken(@RequestBody RefreshTokenRequestDTO refreshRequest) {
        return execute("refresh restaurant user token", () -> 
            restaurantAuthenticationService.refreshRUserToken(refreshRequest.getRefreshToken())
        );
    }
}
