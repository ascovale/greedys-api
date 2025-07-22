package com.application.admin.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.admin.service.authentication.AdminAuthenticationService;
import com.application.common.web.dto.post.AuthRequestDTO;
import com.application.common.web.dto.post.AuthResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Admin Authentication", description = "Controller for managing Admin user authentication")
@RestController
@RequestMapping(value = "/admin/auth", produces = "application/json")
@RequiredArgsConstructor
@Slf4j
public class AdminAuthenticationController {
        
    private final AdminAuthenticationService adminAuthenticationService;

    @Operation(summary = "Generate an authentication token", description = "Authenticates a user and returns a JWT token", responses = {
            @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(mediaType = "application/json"))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Authentication request",
        required = true,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthRequestDTO.class))
    )
    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<?> createAuthenticationToken(
            @RequestBody AuthRequestDTO authenticationRequest) {
        return adminAuthenticationService.login(authenticationRequest);
    }
}