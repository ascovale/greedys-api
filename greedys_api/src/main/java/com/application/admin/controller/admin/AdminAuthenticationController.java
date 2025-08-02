package com.application.admin.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.admin.service.authentication.AdminAuthenticationService;
import com.application.common.controller.BaseController;
import com.application.common.web.ApiResponse;
import com.application.common.web.dto.security.AuthRequestDTO;
import com.application.common.web.dto.security.AuthResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Admin Authentication", description = "Controller for managing Admin user authentication")
@RestController
@RequestMapping(value = "/admin/auth", produces = "application/json")
@RequiredArgsConstructor
@Slf4j
public class AdminAuthenticationController extends BaseController {

    private final AdminAuthenticationService adminAuthenticationService;

    @Operation(summary = "Generate an authentication token", description = "Authenticates a user and returns a JWT token")
    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> createAuthenticationToken(
            @RequestBody AuthRequestDTO authenticationRequest) {
        return execute("admin login", () -> adminAuthenticationService.login(authenticationRequest));
    }
}
