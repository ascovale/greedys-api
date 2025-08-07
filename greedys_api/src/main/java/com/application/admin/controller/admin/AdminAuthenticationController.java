package com.application.admin.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.admin.service.authentication.AdminAuthenticationService;
import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.WrapperDataType;
import com.application.common.controller.annotation.WrapperType;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.security.AuthRequestDTO;
import com.application.common.web.dto.security.AuthResponseDTO;
import com.application.common.web.dto.security.RefreshTokenRequestDTO;

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
    @WrapperType(dataClass = AuthResponseDTO.class, type = WrapperDataType.DTO)
    public ResponseEntity<ResponseWrapper<AuthResponseDTO>> createAuthenticationToken(
            @RequestBody AuthRequestDTO authenticationRequest) {
        return execute("admin login", () -> adminAuthenticationService.login(authenticationRequest));
    }

    @Operation(summary = "Refresh authentication token", description = "Refresh an admin JWT token using a refresh token")
    @PostMapping(value = "/refresh", produces = "application/json")
    @PreAuthorize("hasAuthority('PRIVILEGE_REFRESH_ONLY')")
    @WrapperType(dataClass = AuthResponseDTO.class, type = WrapperDataType.DTO)
    public ResponseEntity<ResponseWrapper<AuthResponseDTO>> refreshToken(@RequestBody RefreshTokenRequestDTO refreshRequest) {
        return execute("admin refresh token", () -> 
            adminAuthenticationService.refreshToken(refreshRequest.getRefreshToken())
        );
    }
}
