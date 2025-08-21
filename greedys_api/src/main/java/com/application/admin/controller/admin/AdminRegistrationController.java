package com.application.admin.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.application.admin.service.AdminRegistrationService;
import com.application.admin.web.dto.admin.AdminDTO;
import com.application.admin.web.dto.admin.NewAdminDTO;
import com.application.common.controller.BaseController;
import com.application.common.web.ResponseWrapper;

import io.swagger.v3.oas.annotations.Operation;
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
public class AdminRegistrationController extends BaseController {

    private final AdminRegistrationService adminRegistrationService;

    @Operation(summary = "Register a new admin")
    
    @PostMapping("/")
    public ResponseEntity<ResponseWrapper<AdminDTO>> registerUserAccount(@Valid @RequestBody NewAdminDTO accountDto, HttpServletRequest request) {
        return executeCreate("register new admin",  () -> {
            return adminRegistrationService.registerNewAdmin(accountDto, request);
        });
    }

    @Operation(summary = "Confirm admin registration")
    @GetMapping(value = "/confirm")
    public ResponseEntity<ResponseWrapper<String>> confirmRegistration(final HttpServletRequest request, @RequestParam final String token) {
        return execute("confirm admin registration", () -> {
            return adminRegistrationService.confirmRegistrationAndAuthenticate(token, request.getLocale());
        });
    }

    @Operation(summary = "Resend verification token")
    @RequestMapping(value = "/resend_token", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ResponseWrapper<String>> resendRegistrationToken(final HttpServletRequest request, @RequestParam("token") final String existingToken) {
        return execute("resend verification token", () -> {
            return adminRegistrationService.resendVerificationToken(existingToken, request, request.getLocale());
        });
    }

}
