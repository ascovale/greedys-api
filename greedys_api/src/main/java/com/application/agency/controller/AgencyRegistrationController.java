package com.application.agency.controller;

import java.io.UnsupportedEncodingException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.agency.service.authentication.AgencyAuthenticationService;
import com.application.common.controller.BaseController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Agency Registration", description = "Controller for agency user registration and verification")
@RequestMapping("/agency/user/register")
@RequiredArgsConstructor
public class AgencyRegistrationController extends BaseController {

    private final AgencyAuthenticationService agencyAuthenticationService;

    @Operation(summary = "Confirm agency user hub registration", description = "Conferma la registrazione dell'Hub utente agency")
    @GetMapping(value = "/confirm-hub")
    public ResponseEntity<String> confirmAgencyUserHubRegistration(final HttpServletRequest request,
            @RequestParam final String token) throws UnsupportedEncodingException {
        return execute("confirm agency hub registration", () -> {
            // Esegue la conferma Hub e aggiorna tutti gli AgencyUser associati
            agencyAuthenticationService.confirmAgencyUserHubRegistration(request, null, token);
            return "Agency hub registration confirmed successfully - all associated users are now enabled";
        });
    }
}