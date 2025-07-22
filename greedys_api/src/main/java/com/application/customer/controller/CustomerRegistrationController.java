package com.application.customer.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.jwt.JwtUtil;
import com.application.common.security.user.ISecurityUserService;
import com.application.common.service.EmailService;
import com.application.common.web.dto.AuthRequestGoogleDTO;
import com.application.common.web.dto.get.CustomerDTO;
import com.application.common.web.dto.post.AuthResponseDTO;
import com.application.common.web.util.GenericResponse;
import com.application.customer.CustomerOnRegistrationCompleteEvent;
import com.application.customer.model.Customer;
import com.application.customer.model.Privilege;
import com.application.customer.model.VerificationToken;
import com.application.customer.service.CustomerService;
import com.application.customer.service.authentication.CustomerAuthenticationService;
import com.application.customer.web.post.NewCustomerDTO;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@RestController
@Tag(name = "Customer Registration", description = "Controller for managing customer registration")
@RequestMapping("/customer/auth")
@RequiredArgsConstructor
@Slf4j
public class CustomerRegistrationController {

    private final CustomerService customerService;
    private final MessageSource messages;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailService mailService;
    private final ISecurityUserService securityCustomerService;
    private final CustomerAuthenticationService customerAuthenticationService;
    private final JwtUtil jwtUtil;

    // ------------------- API Methods ----------------------------- //

    // 1. Authentication and Registration methods
    @Operation(summary = "Register a new customer", description = "Registers a new customer account and sends a verification email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer successfully registered", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = NewCustomerDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @PostMapping("/new")
    public ResponseEntity<String> registerCustomerAccount(@Valid @RequestBody NewCustomerDTO accountDto,
            HttpServletRequest request) {
        try {
            Customer customer = customerAuthenticationService.registerNewCustomerAccount(accountDto);
            eventPublisher
                    .publishEvent(
                            new CustomerOnRegistrationCompleteEvent(customer, Locale.ITALIAN, getAppUrl(request)));
            return ResponseEntity.ok("Customer registered successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 2. Password Management methods
    @Operation(summary = "Send password reset email", description = "Sends an email with a password reset token to the specified customer.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset email sent successfully", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid email address", content = @Content),
        @ApiResponse(responseCode = "404", description = "Customer not found", content = @Content)
    })
    @PostMapping("/password/forgot")
    public ResponseEntity<String> sendPasswordResetEmail(@RequestParam String email, HttpServletRequest request) {
        Customer customer = customerService.findCustomerByEmail(email);
        if (customer == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer not found");
        }

        String token = UUID.randomUUID().toString();
        customerAuthenticationService.createPasswordResetTokenForCustomer(customer, token);
        mailService.sendEmail(constructResetTokenEmail(getAppUrl(request), request.getLocale(), token, customer));

        return ResponseEntity.ok("Password reset email sent successfully");
    }

    @Operation(summary = "Confirm password change with token", description = "Confirms the password change using a token")
    @ApiResponse(responseCode = "200", description = "Password changed successfully or invalid token", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PutMapping(value = "/password/confirm")
    public String confirmPasswordChange(
            @Parameter(description = "Password reset token") @RequestParam final String token) {
        final String result = securityCustomerService.validatePasswordResetToken(token);
        if (result != null) {
            return "invalidToken";
        }
        return "success";
    }

    // 3. Token Management methods
    @Operation(summary = "Confirm customer registration", description = "Validates the registration token and activates the customer account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account successfully verified", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token", content = @Content)
    })
    @RequestMapping(value = "/confirm", method = RequestMethod.GET)
    @ResponseBody
    public GenericResponse confirmRegistrationResponse(final HttpServletRequest request,
            @RequestParam final String token) throws UnsupportedEncodingException {
        Locale locale = request.getLocale();
        final String result = customerAuthenticationService.validateVerificationToken(token);
        if (result.equals("valid")) {
            final Customer customer = customerAuthenticationService.getCustomer(token);
            authWithoutPassword(customer);
            return new GenericResponse(messages.getMessage("message.accountVerified", null, locale));
        }

        return new GenericResponse(
                messages.getMessage("auth.message." + result, null, locale) + "expired".equals(result));
    }

    @Operation(summary = "Resend registration token", description = "Generates and sends a new registration token to the customer.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token successfully resent", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid token", content = @Content)
    })
    @RequestMapping(value = "/resend_token", method = RequestMethod.GET)
    @ResponseBody
    public GenericResponse resendRegistrationToken(final HttpServletRequest request,
            @RequestParam("token") final String existingToken) {
        final VerificationToken newToken = customerAuthenticationService.generateNewVerificationToken(existingToken);
        Customer customer = customerAuthenticationService.getCustomer(newToken.getToken());
        mailService.sendEmail(
                constructResendVerificationTokenEmail(getAppUrl(request), request.getLocale(), newToken, customer));
        return new GenericResponse(messages.getMessage("message.resendToken", null, request.getLocale()));
    }

    
    //TODO  Registration with facebook verify the restaurant email if it is facebook verified

    //TODO Registration with apple verify the restaurant email if it is apple verified

    // 4. Google Authentication
    @Operation(summary = "Authenticate with Google", description = "Authenticates or if not exist register a customer using a Google token and returns a JWT token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(mediaType = "application/json"))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Google authentication request", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthRequestGoogleDTO.class)))
    @PostMapping("/google")
    public ResponseEntity<AuthResponseDTO> authenticateWithGoogle(@RequestBody AuthRequestGoogleDTO authRequest)
            throws Exception {
        log.warn("Received Google authentication request: {}", authRequest.getToken());
        GoogleIdToken idToken = verifyGoogleToken(authRequest.getToken());

        if (idToken != null) {
            String email = idToken.getPayload().getEmail();
            String name = (String) idToken.getPayload().get("name");
            log.warn("Google token verified. Email: {}, Name: {}", email, name);
            // quali dati vogliamo prendere da google?
            Customer customer = customerService.findCustomerByEmail(email);
            if (customer == null) {
                String[] nameParts = name != null ? name.split(" ", 2) : new String[]{"", ""};
                NewCustomerDTO accountDto = NewCustomerDTO.builder()
                        .firstName(nameParts[0])
                        .lastName(nameParts.length > 1 ? nameParts[1] : "")
                        .email(email)
                        .password(generateRandomPassword())
                        .build();
                customer = customerAuthenticationService.registerNewCustomerAccount(accountDto);
            }
            String jwt = jwtUtil.generateToken(customer);
            return ResponseEntity.ok(new AuthResponseDTO(jwt, new CustomerDTO(customer)));
        } else {
            log.warn("Google token verification failed.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    // ------------------- Private Methods ----------------------------- //

    private String getAppUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                + request.getContextPath();
    }

    private SimpleMailMessage constructResendVerificationTokenEmail(final String contextPath, final Locale locale,
            final VerificationToken newToken, final Customer customer) {
        final String confirmationUrl = contextPath + "/registrationConfirm.html?token=" + newToken.getToken();
        final String message = messages.getMessage("message.resendToken", null, locale);
        return constructEmail("Resend Registration Token", message + " \r\n" + confirmationUrl, customer);
    }

    private SimpleMailMessage constructResetTokenEmail(final String contextPath, final Locale locale,
            final String token, final Customer customer) {
        final String url = contextPath + "/customer/changePassword?id=" + customer.getId() + "&token=" + token;
        final String message = messages.getMessage("message.resetPassword", null, locale);
        return constructEmail("Reset Password", message + " \r\n" + url, customer);
    }

    private SimpleMailMessage constructEmail(String subject, String body, Customer customer) {
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setSubject(subject);
        email.setText(body);
        email.setTo(customer.getEmail());
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

    public void authWithoutPassword(Customer customer) {
        List<Privilege> privileges = customer.getRoles().stream().map(role -> role.getPrivileges())
                .flatMap(list -> list.stream()).distinct().collect(Collectors.toList());
        List<GrantedAuthority> authorities = privileges.stream().map(p -> new SimpleGrantedAuthority(p.getName()))
                .collect(Collectors.toList());

        Authentication authentication = new UsernamePasswordAuthenticationToken(customer, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private GoogleIdToken verifyGoogleToken(String token) throws Exception {
        try {
            log.debug("Verifying Google token... {}", token);
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Arrays.asList(
                            "982346813437-3s1uepb5ic7ib5r4mfegdsbrkjjvtl7b.apps.googleusercontent.com", // Web client ID
                                                                                                        // (API)
                            "982346813437-d0kerhe6h2km0veqs563avsgtv6vb7p5.apps.googleusercontent.com", // Flutter Web
                            "982346813437-e1vsuujvorosiaamfdc3honrrbur17ri.apps.googleusercontent.com", // Android
                            "982346813437-iosclientid.apps.googleusercontent.com" // TODO: Inserire il token per Ios
                    ))
                    .build();

            GoogleIdToken idToken = verifier.verify(token);
            if (idToken != null) {
                log.debug("Google token verified successfully.");
            } else {
                log.warn("Google token verification failed: Invalid token.");
            }
            return idToken;
        } catch (GeneralSecurityException e) {
            log.error("Google token verification failed: GeneralSecurityException - {}", e.getMessage(), e);
        } catch (IOException e) {
            log.error("Google token verification failed: IOException - {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Google token verification failed: Exception - {}", e.getMessage(), e);
        }
        return null;
    }

    private String generateRandomPassword() {
        // Implement a method to generate a random password
        return UUID.randomUUID().toString();
    }
}
