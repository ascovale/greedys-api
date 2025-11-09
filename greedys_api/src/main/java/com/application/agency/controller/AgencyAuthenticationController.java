package com.application.agency.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.security.jwt.JwtUtil;
import com.application.common.web.dto.security.AuthResponseDTO;
import com.application.common.web.dto.security.RefreshTokenRequestDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Tag(name = "Agency Authentication", description = "Controller for agency authentication")
@RequestMapping("/agency/user/auth")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class AgencyAuthenticationController extends BaseController {

    private final JwtUtil jwtUtil;
    // TODO: Add AgencyAuthenticationService when implemented

    @Operation(summary = "Login to agency", description = "Authenticate agency user with email, password, and agency ID")
    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AgencyLoginRequestDTO request) {
        return execute("agency login", () -> {
            // TODO: Implement authentication logic
            throw new UnsupportedOperationException("Agency authentication not yet implemented");
        });
    }

    @Operation(summary = "Refresh JWT token", description = "Refresh an expired JWT token")
    @PostMapping(value = "/refresh", produces = "application/json")
    public ResponseEntity<AuthResponseDTO> refresh(@RequestBody RefreshTokenRequestDTO request) {
        return execute("refresh agency token", () -> {
            // TODO: Implement refresh logic
            throw new UnsupportedOperationException("Agency token refresh not yet implemented");
        });
    }

    @Operation(summary = "Refresh Agency Hub JWT token", description = "Refresh an expired Agency Hub JWT token")
    @PostMapping(value = "/refresh/hub", produces = "application/json")
    public ResponseEntity<AuthResponseDTO> refreshHub(@RequestBody RefreshTokenRequestDTO request) {
        return execute("refresh agency hub token", () -> {
            // TODO: Implement hub refresh logic
            throw new UnsupportedOperationException("Agency hub token refresh not yet implemented");
        });
    }

    @Operation(summary = "Logout", description = "Logout and invalidate tokens")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return execute("agency logout", () -> {
            SecurityContextHolder.clearContext();
            return null;
        });
    }

    // Inner DTOs for requests
    public static class AgencyLoginRequestDTO {
        private String email;
        private String password;
        private Long agencyId;

        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public Long getAgencyId() { return agencyId; }
        public void setAgencyId(Long agencyId) { this.agencyId = agencyId; }
    }

    public static class AgencySwitchRequestDTO {
        private Long agencyId;

        public Long getAgencyId() { return agencyId; }
        public void setAgencyId(Long agencyId) { this.agencyId = agencyId; }
    }
}