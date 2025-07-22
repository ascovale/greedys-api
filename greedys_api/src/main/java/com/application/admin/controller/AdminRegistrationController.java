package com.application.admin.controller;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.application.admin.AdminOnRegistrationCompleteEvent;
import com.application.admin.model.Admin;
import com.application.admin.model.AdminPrivilege;
import com.application.admin.model.AdminVerificationToken;
import com.application.admin.service.AdminService;
import com.application.admin.web.post.NewAdminDTO;
import com.application.common.service.EmailService;
import com.application.common.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


//TODO: bisogna implementare che qualche utente può modificare le password degli admin
// creare un permesso specifico per questo
@RestController
@RequestMapping("/admin/register")
@Tag(name = "Admin Registration", description = "Admin Registration Management")
@RequiredArgsConstructor
@Slf4j
public class AdminRegistrationController {

    private final AdminService adminService;
    private final MessageSource messages;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailService mailService;

    private String getAppUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    }

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
            Admin admin = adminService.registerNewAdminAccount(accountDto);
            //verifica la mail mandiamo una mail di conferma
            eventPublisher.publishEvent(new AdminOnRegistrationCompleteEvent(admin, Locale.ITALIAN, getAppUrl(request)));
            return ResponseEntity.ok("User registered successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @RequestMapping(value = "/confirm", method = RequestMethod.GET)
    public String confirmRegistration(final HttpServletRequest request, final Model model, @RequestParam final String token) throws UnsupportedEncodingException {
        Locale locale = request.getLocale();
        final String result = adminService.validateVerificationToken(token);
        if (result.equals("valid")) {
            final Admin admin = adminService.getAdmin(token);
            // if (user.isUsing2FA()) {
            // model.addAttribute("qr", userService.generateQRUrl(user));
            // return "redirect:/qrcode.html?lang=" + locale.getLanguage();
            // }
            authWithoutPassword(admin);
            model.addAttribute("message", messages.getMessage("message.accountVerified", null, locale));
            return "redirect:/console.html?lang=" + locale.getLanguage();
        }

        model.addAttribute("message", messages.getMessage("auth.message." + result, null, locale));
        model.addAttribute("expired", "expired".equals(result));
        model.addAttribute("token", token);
        return "redirect:/badUser.html?lang=" + locale.getLanguage();
    }

    // user activation - verification

    @RequestMapping(value = "/resend_token", method = RequestMethod.GET)
    @ResponseBody
    public GenericResponse resendRegistrationToken(final HttpServletRequest request, @RequestParam("token") final String existingToken) {
        final AdminVerificationToken newToken = adminService.generateNewVerificationToken(existingToken);
		Admin admin = adminService.getAdmin(newToken.getToken());
        mailService.sendEmail(constructResendVerificationTokenEmail(getAppUrl(request), request.getLocale(), newToken, admin));
        return new GenericResponse(messages.getMessage("message.resendToken", null, request.getLocale()));
    }


/*
    @RequestMapping(value = "/user/update/2fa", method = RequestMethod.POST)
    @ResponseBody
    public GenericResponse modifyUser2FA(@RequestParam("use2FA") final boolean use2FA) throws UnsupportedEncodingException {
        final User user = userService.updateUser2FA(use2FA);
        if (use2FA) {
            return new GenericResponse(userService.generateQRUrl(user));
        }
        return null;
    }*/

	private SimpleMailMessage constructResendVerificationTokenEmail(final String contextPath, final Locale locale, final AdminVerificationToken newToken, final Admin admin) {
        final String confirmationUrl = contextPath + "/registrationConfirm.html?token=" + newToken.getToken();
        final String message = messages.getMessage("message.resendToken", null, locale);
        return constructEmail("Resend Registration Token", message + " \r\n" + confirmationUrl, admin);
    }

    @SuppressWarnings("unused")
	private SimpleMailMessage constructResetTokenEmail(final String contextPath, final Locale locale, final String token, final Admin admin) {
        final String url = contextPath + "/admin/changePassword?id=" + admin.getId() + "&token=" + token;
        final String message = messages.getMessage("message.resetPassword", null, locale);
        return constructEmail("Reset Password", message + " \r\n" + url, admin);
    }

    private SimpleMailMessage constructEmail(String subject, String body, Admin admin) {
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setSubject(subject);
        email.setText(body);
        email.setTo(admin.getEmail());
        email.setFrom("reservation@greedys.it");
        return email;
    }

    public void authWithHttpServletRequest(HttpServletRequest request, String username, String password) {
        try {
            request.login(username, password);
        } catch (ServletException e) {
            log.error("Error while login ", e);
        }
    }

    /*

    public void authWithAuthManager(HttpServletRequest request, String username, String password) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);
        authToken.setDetails(new WebAuthenticationDetails(request));
        Authentication authentication = authenticationManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
    }
        */

        
    public void authWithoutPassword(Admin admin) {

        List<AdminPrivilege> privileges = admin.getRoles().stream()
        .map(adminRole -> adminRole.getPrivileges())
        .flatMap(Collection::stream)
        .distinct()
        .collect(Collectors.toList());
        List<GrantedAuthority> authorities = privileges.stream().map(p -> new SimpleGrantedAuthority(p.getName())).collect(Collectors.toList());

        Authentication authentication = new UsernamePasswordAuthenticationToken(admin, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
	



}
