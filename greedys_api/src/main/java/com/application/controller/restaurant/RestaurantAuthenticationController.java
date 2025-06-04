package com.application.controller.restaurant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.authentication.RestaurantAuthenticationService;
import com.application.web.dto.post.AuthRequestDTO;
import com.application.web.dto.post.AuthResponseDTO;
import com.application.web.dto.post.RestaurantUserSelectRequestDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Restaurant Authentication", description = "Controller for restaurant authentication")
@RequestMapping("/restaurant/auth")
@SecurityRequirement(name = "bearerAuth")
public class RestaurantAuthenticationController {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final RestaurantAuthenticationService restaurantAuthenticationService;

    public RestaurantAuthenticationController(
            RestaurantAuthenticationService restaurantAuthenticationService) {
        this.restaurantAuthenticationService = restaurantAuthenticationService;
    }

    @Operation(summary = "Select a restaurant after intermediate login", description = "Given a hub JWT and a restaurantId, returns a JWT for the selected restaurant user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Selection successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Selection failed", content = @Content(mediaType = "application/json"))
    })
    @PostMapping(value = "/login", produces = "application/json")
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
