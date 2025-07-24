package com.application.admin.controller.restaurant;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.admin.service.authentication.AdminRUserAuthenticationService;
import com.application.common.controller.BaseController;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.service.RUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/admin/restaurant/user")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Restaurant User", description = "Admin management APIs for the RUser")
@RequiredArgsConstructor
@Slf4j
public class AdminRUserController extends BaseController {

    private final RUserService RUserService;
    private final AdminRUserAuthenticationService adminRUserAuthenticationService;

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_USER_WRITE')")
    @Operation(summary = "Block restaurant user", description = "Blocks a restaurant user by their ID")
    @PutMapping("/{RUserId}/block")
    public ResponseEntity<com.application.common.web.dto.ApiResponse<String>> blockRUser(@PathVariable Long RUserId) {
        return executeVoid("block restaurant user", "Restaurant user blocked successfully", () -> {
            RUserService.updateRUserStatus(RUserId, RUser.Status.BLOCKED);
        });
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_USER_WRITE')")
    @Operation(summary = "Enable restaurant user", description = "Enables a restaurant user by their ID")
    @PutMapping("/{RUserId}/enable")
    public ResponseEntity<com.application.common.web.dto.ApiResponse<String>> enableRUser(@PathVariable Long RUserId) {
        return executeVoid("enable restaurant user", "Restaurant user enabled successfully", () -> {
            RUserService.updateRUserStatus(RUserId, RUser.Status.ENABLED);
        });
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_USER_WRITE')")
    @Operation(summary = "Change restaurant owner", description = "Changes the owner of a restaurant")
    @PutMapping("/{restaurantId}/changeOwner/{idOldOwner}/{idNewOwner}")
    public ResponseEntity<com.application.common.web.dto.ApiResponse<String>> changeRestaurantOwner(@PathVariable Long restaurantId, @PathVariable Long idOldOwner,
            @PathVariable Long idNewOwner) {
        return executeVoid("change restaurant owner", "Restaurant owner changed successfully", () -> {
            RUserService.changeRestaurantOwner(restaurantId, idOldOwner, idNewOwner);
        });
    }

    //@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_SWITCH_TO_RESTAURANT_USER')")
    @GetMapping("/login/{RUserId}")
    @Operation(summary = "Get JWT Token of a restaurant user", description = "Returns the JWT token of a restaurant user")
    public ResponseEntity<com.application.common.web.dto.ApiResponse<Object>> loginHasRUser(@PathVariable Long RUserId, HttpServletRequest request) {
        return execute("get restaurant user token", () -> adminRUserAuthenticationService.adminLoginToRUser(RUserId, request));
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_USER_READ')")
    @Operation(summary = "Get restaurant users", description = "Retrieves the list of users for a specific restaurant")
    @GetMapping("/{restaurantId}/users")
    public ResponseEntity<com.application.common.web.dto.ApiResponse<Object>> getRUsers(@PathVariable Long restaurantId) {
        return execute("get restaurant users", () -> RUserService.getRUsersByRestaurantId(restaurantId));
    }

}
