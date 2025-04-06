package com.application.controller.admin;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
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

import com.application.persistence.model.admin.Admin;
import com.application.persistence.model.admin.AdminPrivilege;
import com.application.persistence.model.admin.AdminVerificationToken;
import com.application.registration.AdminOnRegistrationCompleteEvent;
import com.application.service.AdminService;
import com.application.service.EmailService;
import com.application.web.dto.post.admin.NewAdminDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


//TODO: questo non va in public ma in admin un admin è creato di default
@RestController
@RequestMapping("/admin/register")
public class AdminRegistrationController {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private AdminService adminService;

    @Autowired
    private MessageSource messages;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private Environment env;

    @Autowired
    private EmailService mailService;

    public AdminRegistrationController() {
        super();
    }

    private String getAppUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    }

    // Registration
    @Operation(summary = "Register a new admin")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Admin successfully registered",
                     content = { @Content(mediaType = "application/json",
                     schema = @Schema(implementation = NewAdminDTO.class)) }),
        @ApiResponse(responseCode = "400", description = "Invalid request",
                     content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                     content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                     content = @Content)
    })
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


    @Operation(summary = "Confirm admin registration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Registration successfully confirmed",
                     content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request",
                     content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                     content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                     content = @Content)
    })
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

    @Operation(summary = "Resend registration token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token successfully resent",
                     content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request",
                     content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                     content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                     content = @Content)
    })
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
            LOGGER.error("Error while login ", e);
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

        List<AdminPrivilege> privileges = admin.getAdminRoles().stream()
        .map(adminRole -> adminRole.getAdminPrivileges())
        .flatMap(Collection::stream)
        .distinct()
        .collect(Collectors.toList());
        List<GrantedAuthority> authorities = privileges.stream().map(p -> new SimpleGrantedAuthority(p.getName())).collect(Collectors.toList());

        Authentication authentication = new UsernamePasswordAuthenticationToken(admin, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
	



}
