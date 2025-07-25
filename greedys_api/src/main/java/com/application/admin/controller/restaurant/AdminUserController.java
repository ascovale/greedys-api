package com.application.admin.controller.restaurant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.web.ApiResponse;
import com.application.restaurant.service.RUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/admin/restaurant")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin User", description = "Admin User Management")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController extends BaseController {

	private final RUserService rUserService;

	@PostMapping("/{RUserId}/accept")
	@Operation(summary = "Accept a user", description = "Accept a user for a specific restaurant")
	public ResponseEntity<ApiResponse<String>> acceptUser(@PathVariable Long RUserId) {
		return executeVoid("accept user", "User accepted successfully", () -> {
			rUserService.acceptRUser(RUserId);
		});
	}
}
