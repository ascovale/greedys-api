package com.application.customer.controller.customer;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
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

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.web.ApiResponse;
import com.application.common.web.dto.AuthRequestGoogleDTO;
import com.application.common.web.dto.post.AuthResponseDTO;
import com.application.customer.persistence.model.Customer;
import com.application.customer.persistence.model.Privilege;
import com.application.customer.service.authentication.CustomerAuthenticationService;
import com.application.customer.service.security.CustomerUserSecurityService;
import com.application.customer.web.post.NewCustomerDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
public class CustomerRegistrationController extends BaseController {

    private final MessageSource messages;
    private final CustomerUserSecurityService securityCustomerService;
    private final CustomerAuthenticationService customerAuthenticationService;

    @Operation(summary = "Register a new customer", description = "Registers a new customer account and sends a verification email.")
    @PostMapping("/new")
    @CreateApiResponses
    public ResponseEntity<ApiResponse<String>> registerCustomerAccount(@Valid @RequestBody NewCustomerDTO accountDto,
            HttpServletRequest request) {
        return executeCreate("Register new customer", () -> {
            customerAuthenticationService.registerNewCustomerAccount(accountDto);
            return "Customer registered successfully";
        });
    }

    @Operation(summary = "Send password reset email", description = "Sends an email with a password reset token to the specified customer.")
    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponse<String>> sendPasswordResetEmail(@RequestParam String email, HttpServletRequest request) {
        return executeVoid("Send password reset email", "Password reset email sent successfully", () -> {
            customerAuthenticationService.sendPasswordResetEmail(email, getAppUrl(request), request.getLocale());
        });
    }

    @Operation(summary = "Confirm password change with token", description = "Confirms the password change using a token")
    @PutMapping(value = "/password/confirm")
    public ResponseEntity<ApiResponse<String>> confirmPasswordChange(
            @Parameter(description = "Password reset token") @RequestParam final String token) {
        return execute("Validate password reset token", () -> {
            return securityCustomerService.validatePasswordResetToken(token);
            
        });
    }

    @Operation(summary = "Confirm customer registration", description = "Validates the registration token and activates the customer account.")
    @RequestMapping(value = "/confirm", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> confirmRegistrationResponse(final HttpServletRequest request,
            @RequestParam final String token) throws UnsupportedEncodingException {
        return executeVoid("Confirm customer registration", () -> {
            Locale locale = request.getLocale();
            final String result = customerAuthenticationService.validateVerificationToken(token);
            if (result.equals("valid")) {
                final Customer customer = customerAuthenticationService.getCustomer(token);
                authWithoutPassword(customer);
            } else {
                throw new IllegalArgumentException(messages.getMessage("message.invalidToken", null, locale));
            }
        });
    }

    @Operation(summary = "Resend registration token", description = "Generates and sends a new registration token to the customer.")
    @RequestMapping(value = "/resend_token", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> resendRegistrationToken(final HttpServletRequest request,
            @RequestParam("token") final String existingToken) {
        return executeVoid("Resend registration token", () -> {
            customerAuthenticationService.resendRegistrationToken(existingToken, getAppUrl(request), request.getLocale());
        });
    }

    //TODO Registration with facebook verify the restaurant email if it is facebook verified
    //TODO Registration with apple verify the restaurant email if it is apple verified

    // 4. Google Authentication
    @Operation(summary = "Authenticate with Google", description = "Authenticates or if not exist register a customer using a Google token and returns a JWT token.")
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> authenticateWithGoogle(@RequestBody AuthRequestGoogleDTO authRequest) {
        return execute("Authenticate with Google", () -> customerAuthenticationService.authenticateWithGoogle(authRequest.getToken()));
    }

    // ------------------- Private Methods ----------------------------- //

    private String getAppUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                + request.getContextPath();
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
}

