package com.application.controller.restaurantUser;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.RestaurantUserService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "MultiRestaurantUser", description = "Controller per la gestione dei multi user di ristoranti")
@RestController
@RequestMapping("/restaurant_user/")
@SecurityRequirement(name = "bearerAuth")
public class MultiRestaurantUserController {
        private final RestaurantUserService restaurantUserService;

        public MultiRestaurantUserController(RestaurantUserService restaurantUserService) {
                this.restaurantUserService = restaurantUserService;
        }

        @PreAuthorize("hasAuthority('PRIVILEGE_SWITCH_TO_RESTAURANT_USER') and @securityRestaurantUserService.hasRestaurantUserId(authentication, #restaurantUserId)")
        @PostMapping("/switch_to_restaurant_user")
        public String switchUser(@RequestParam Long restaurantUserId) {
                restaurantUserService.switchToRestaurantUser(restaurantUserId);
                return "User switched to: " + restaurantUserId;
        }

        @PreAuthorize("hasAuthority('PRIVILEGE_SWITCH_TO_RESTAURANT_USER') and @securityRestaurantUserService.hasRestaurantUserId(authentication, #restaurantUserId)")
        @PostMapping("/disconnect_restaurant_user")
        public String removeUser(@RequestParam Long restaurantUserId) {
                restaurantUserService.disconnectRestaurantUser(restaurantUserId);
                return "User removed from restaurant user ID: " + restaurantUserId;
        }
}