package com.application.admin.controller.restaurant;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.web.util.GenericResponse;
import com.application.restaurant.service.RUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
public class AdminUserController {

	private final RUserService rUserService;

	@PostMapping("/{RUserId}/accept")
	@Operation(summary = "Accept a user", description = "Accept a user for a specific restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
			@ApiResponse(responseCode = "404", description = "Restaurant or user not found")
	})
	public GenericResponse acceptUser(@PathVariable Long RUserId) {
		rUserService.acceptRUser(RUserId);
		return new GenericResponse("success");
	}
}
