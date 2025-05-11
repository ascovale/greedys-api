package com.application.controller.admin;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.admin.Admin;
import com.application.security.jwt.JwtUtil;
import com.application.service.AdminService;
import com.application.web.dto.AdminAuthResponseDTO;
import com.application.web.dto.get.AdminDTO;
import com.application.web.dto.post.AuthRequestDTO;
import com.application.web.dto.post.AuthResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "1. Admin Authentication", description = "Controller for managing Admin user authentication")
@RestController
@RequestMapping(value = "/admin/auth", produces = "application/json")
public class AdminAuthenticationController {
        
    private AuthenticationManager authenticationManager;
    private JwtUtil jwtUtil;
    private AdminService adminService;

    public AdminAuthenticationController(
            @Qualifier("adminAuthenticationManager") AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            AdminService adminService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.adminService = adminService;
    }

    @Operation(summary = "Generate an authentication token", description = "Authenticates a user and returns a JWT token", responses = {
            @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(mediaType = "application/json"))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Authentication request", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthRequestDTO.class)))
    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<?> createAuthenticationToken(
            @RequestBody AuthRequestDTO authenticationRequest) {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(),
                            authenticationRequest.getPassword()));

            final Admin userDetails = adminService.findAdminByEmail(authenticationRequest.getUsername());
            final String jwt = jwtUtil.generateToken(userDetails);
            final AdminAuthResponseDTO responseDTO = new AdminAuthResponseDTO(jwt, new AdminDTO(userDetails));
            return ResponseEntity.ok(responseDTO);
    }
}