package com.application.admin.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.application.admin.service.AdminRegistrationService;
import com.application.admin.web.post.NewAdminDTO;
import com.application.common.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/admin/register")
@Tag(name = "Admin Registration", description = "Admin Registration Management")
@RequiredArgsConstructor
@Slf4j
public class AdminRegistrationController {

    private final AdminRegistrationService adminRegistrationService;

    @Operation(summary = "Register a new admin")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Admin registered successfully",
                     content = { @Content(mediaType = "application/json",
                     schema = @Schema(implementation = NewAdminDTO.class)) }),
        @ApiResponse(responseCode = "400", description = "Invalid request",
                     content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                     content = @Content) })
    @PostMapping("/")
    public ResponseEntity<String> registerUserAccount(@Valid @RequestBody NewAdminDTO accountDto, HttpServletRequest request) {
        try {
            adminRegistrationService.registerNewAdmin(accountDto, request);
            return ResponseEntity.ok("Admin registered successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Confirm admin registration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Registration confirmed successfully",
                     content = { @Content(mediaType = "application/json",
                     schema = @Schema(implementation = GenericResponse.class)) }),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token",
                     content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                     content = @Content) })
    @RequestMapping(value = "/confirm", method = RequestMethod.GET)
    public ResponseEntity<GenericResponse> confirmRegistration(final HttpServletRequest request, @RequestParam final String token) {
        try {
            GenericResponse response = adminRegistrationService.confirmRegistrationAndAuthenticate(token, request.getLocale());
            if (response.getMessage().contains("successfully")) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Error confirming registration with token: {}", token, e);
            return ResponseEntity.status(500).body(new GenericResponse("Internal server error"));
        }
    }

    @Operation(summary = "Resend verification token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verification token resent successfully",
                     content = { @Content(mediaType = "application/json",
                     schema = @Schema(implementation = GenericResponse.class)) }),
        @ApiResponse(responseCode = "400", description = "Invalid token",
                     content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                     content = @Content) })
    @RequestMapping(value = "/resend_token", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<GenericResponse> resendRegistrationToken(final HttpServletRequest request, @RequestParam("token") final String existingToken) {
        try {
            GenericResponse response = adminRegistrationService.resendVerificationToken(existingToken, request, request.getLocale());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error resending verification token: {}", existingToken, e);
            return ResponseEntity.badRequest().body(new GenericResponse("Failed to resend verification token: " + e.getMessage()));
        }
    }

}
