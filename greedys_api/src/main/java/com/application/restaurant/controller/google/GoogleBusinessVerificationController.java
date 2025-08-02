package com.application.restaurant.controller.google;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.web.ApiResponse;
import com.application.restaurant.service.google.RestaurantGooglePlacesService;
import com.application.restaurant.service.google.after.RestaurantGoogleVerificationService;
import com.application.restaurant.web.dto.google.RestaurantAuthorizationResult;
import com.application.restaurant.web.dto.restaurantGoogleDTO.RestaurantSelectionRequestDTO;
import com.application.restaurant.web.dto.restaurantGoogleDTO.SearchRequestDTO;
import com.application.restaurant.web.dto.restaurantGoogleDTO.UserRestaurantsRequestDTO;
import com.application.restaurant.web.dto.search.RestaurantSearchResult;
import com.application.restaurant.web.dto.verification.RestaurantVerificationResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/restaurant/verification")
@CrossOrigin(origins = "*")
@Tag(name = "Restaurant Verification", description = "API for restaurant verification via Google. " +
     "FLOW: 1) User authorizes scopes (userinfo.email, userinfo.profile, business.manage) " +
     "2) get-restaurants → user's restaurant list " +
     "3) select-restaurant → final choice")
public class GoogleBusinessVerificationController extends BaseController {
    
    private final RestaurantGoogleVerificationService verificationService;
    private final RestaurantGooglePlacesService googlePlacesService;
    
    /**
     * STEP 1: Gets the list of restaurants managed by the user (NEW FLOW)
     * The user must authorize ALL scopes: userinfo.email, userinfo.profile, business.manage
     */
    @Operation(summary = "Get managed restaurants", 
               description = "Gets the list of all restaurants that the user can manage (STEP 1). " +
                           "Requires Access Token with scopes: userinfo.email, userinfo.profile, business.manage")
    @PostMapping("/get-restaurants")
    public ResponseEntity<ApiResponse<RestaurantAuthorizationResult>> getUserRestaurants(
            @RequestBody UserRestaurantsRequestDTO request) {
        return execute("get user restaurants", "Restaurant list retrieved", new OperationSupplier<RestaurantAuthorizationResult>() {
            @Override
            public RestaurantAuthorizationResult get() {
                return verificationService.getUserRestaurants(
                    request.getAccessToken(),
                    request.getEmail()
                );
            }
        });
    }
    
    /**
     * STEP 2: Select a specific restaurant from the list (NEW FLOW)
     * Uses the same Access Token from STEP 1 (already authorized with all scopes)
     */
    @Operation(summary = "Select restaurant", 
               description = "Select and verify a specific restaurant via placeId (STEP 2). " +
                           "Uses the same Access Token from STEP 1")
    @PostMapping("/select-restaurant")
    public ResponseEntity<ApiResponse<RestaurantVerificationResult>> selectRestaurant(
            @RequestBody RestaurantSelectionRequestDTO request) {
        return execute("select restaurant", "Restaurant selected and verified", new OperationSupplier<RestaurantVerificationResult>() {
            @Override
            public RestaurantVerificationResult get() {
                return verificationService.selectRestaurant(
                    request.getAccessToken(),
                    request.getEmail(),
                    request.getPlaceId()
                );
            }
        });
    }

    @Operation(summary = "Search restaurant", 
               description = "Search for a restaurant on Google Maps without OAuth verification (for testing only)")
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<RestaurantSearchResult>> searchRestaurant(
            @RequestBody SearchRequestDTO request) {
        return execute("search restaurant", "Search completed", new OperationSupplier<RestaurantSearchResult>() {
            @Override
            public RestaurantSearchResult get() {
                return googlePlacesService.searchRestaurant(request.getRestaurantName(), request.getAddress());
            }
        });
    }
    

}
