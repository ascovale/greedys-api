package com.application.agency.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.agency.service.authentication.AgencyAuthenticationService;
import com.application.common.controller.BaseController;
import com.application.common.web.dto.security.AuthResponseDTO;
import com.application.common.web.dto.security.RefreshTokenRequestDTO;
import com.application.common.web.dto.security.AuthRequestDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Tag(name = "Agency Authentication", description = "Controller for agency authentication")
@RequestMapping("/api/v1/agency/auth")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class AgencyAuthenticationController extends BaseController {

    private final AgencyAuthenticationService agencyAuthenticationService;

    @Operation(summary = "Login to agency", description = "Authenticate agency user with email, password, and remember me option")
    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO request) {
        return execute("agency login", () -> {
            return agencyAuthenticationService.loginWithHubSupport(request);
        });
    }

    @Operation(summary = "Refresh Agency User JWT token", description = "Refresh an expired JWT token for agency user (single agency)")
    @PostMapping(value = "/refresh", produces = "application/json")
    public ResponseEntity<AuthResponseDTO> refresh(@RequestBody RefreshTokenRequestDTO request) {
        return execute("refresh agency user token", () -> {
            return agencyAuthenticationService.refreshAgencyUserToken(request.getRefreshToken());
        });
    }

    @Operation(summary = "Refresh Agency Hub JWT token", description = "Refresh an expired JWT token for agency hub (multi-agency)")
    @PostMapping(value = "/refresh/hub", produces = "application/json")
    public ResponseEntity<AuthResponseDTO> refreshHub(@RequestBody RefreshTokenRequestDTO request) {
        return execute("refresh agency hub token", () -> {
            return agencyAuthenticationService.refreshHubToken(request.getRefreshToken());
        });
    }

    @Operation(summary = "Select agency", description = "Hub user selects a specific agency and gets a new JWT for that agency")
    @PostMapping(value = "/select-agency", produces = "application/json")
    public ResponseEntity<AuthResponseDTO> selectAgency(@RequestBody SelectAgencyRequestDTO request) {
        return execute("select agency", () -> {
            return agencyAuthenticationService.selectAgency(request.getAgencyId());
        });
    }

    @Operation(summary = "Change agency", description = "Hub user changes to a different agency (alias for select-agency)")
    @PostMapping(value = "/change-agency", produces = "application/json")
    public ResponseEntity<AuthResponseDTO> changeAgency(@RequestBody SelectAgencyRequestDTO request) {
        return execute("change agency", () -> {
            return agencyAuthenticationService.selectAgency(request.getAgencyId());
        });
    }

    @Operation(summary = "Logout", description = "Logout and invalidate tokens")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return execute("agency logout", () -> {
            SecurityContextHolder.clearContext();
            log.debug("Agency user logged out");
            return null;
        });
    }

    // DTOs for requests
    public static class SelectAgencyRequestDTO {
        private Long agencyId;

        public Long getAgencyId() { return agencyId; }
        public void setAgencyId(Long agencyId) { this.agencyId = agencyId; }
    }
}