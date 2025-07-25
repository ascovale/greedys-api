package com.application.customer.controller.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.web.dto.ApiResponse;
import com.application.common.web.dto.post.AuthRequestDTO;
import com.application.common.web.dto.post.AuthResponseDTO;
import com.application.customer.service.authentication.CustomerAuthenticationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Customer Authentication", description = "Controller for managing customer authentication")
@RestController
@RequestMapping(value = "/customer/auth", produces = "application/json")
@RequiredArgsConstructor
@Slf4j
public class CustomerAuthenticationController extends BaseController {

    private final CustomerAuthenticationService customerAuthenticationService;

    @Operation(summary = "Generate an authentication token", description = "Authenticates a customer and returns a JWT token")
    @PostMapping(value = "/login", produces = "application/json")
    @CreateApiResponses
    public ResponseEntity<ApiResponse<AuthResponseDTO>> createAuthenticationToken(
            @RequestBody AuthRequestDTO authenticationRequest) {
        return execute("customer login", () -> customerAuthenticationService.login(authenticationRequest));
    }
}
