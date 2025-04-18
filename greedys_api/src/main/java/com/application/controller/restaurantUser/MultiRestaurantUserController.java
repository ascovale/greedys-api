package com.application.controller.restaurantUser;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.service.RestaurantUserService;
import com.application.web.dto.get.RestaurantUserDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "3. Multi Restaurant User", description = "Management of multi-restaurant users. Allows operations such as switching users and disconnecting them.")
@RestController
@RequestMapping("/restaurant/multi_user/")
@SecurityRequirement(name = "restaurantBearerAuth")
public class MultiRestaurantUserController {
        private final RestaurantUserService restaurantUserService;

        public MultiRestaurantUserController(RestaurantUserService restaurantUserService) {
                this.restaurantUserService = restaurantUserService;
        }

        @PreAuthorize("hasAuthority('PRIVILEGE_SWITCH_TO_RESTAURANT_USER') and @securityRestaurantUserService.hasRestaurantUserId(authentication, #restaurantUserId)")
        @PostMapping("/switch/{restaurantUserId}")
        @Operation(summary = "Switch restaurant user", description = "Allows switching the active user to the specified restaurant user by its ID.")
        public ResponseEntity<?> loginRestaurantUser(@RequestParam Long restaurantUserId, HttpServletRequest request) {
                return ResponseEntity.ok(restaurantUserService.loginRestaurantUser(restaurantUserId,request));
        }

        @PreAuthorize("hasAuthority('PRIVILEGE_SWITCH_TO_RESTAURANT_USER')")
        @GetMapping("/current_user")
        @Operation(summary = "Get current restaurant user", description = "Returns the details of the currently logged-in restaurant user.")
        public RestaurantUserDTO getCurrentUser() {
                RestaurantUserDTO restaurantUserDTO = new RestaurantUserDTO(getCurrentRestaurantUser());
                return restaurantUserDTO;
        }

        private RestaurantUser getCurrentRestaurantUser() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof RestaurantUser) {
                        return (RestaurantUser) authentication.getPrincipal();
                }
                return null;
        }
}