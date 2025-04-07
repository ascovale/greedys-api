package com.application.controller.pub;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RestaurantPrivilege;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.persistence.model.restaurant.user.RestaurantUserVerificationToken;
import com.application.security.jwt.JwtUtil;
import com.application.security.user.ISecurityUserService;
import com.application.service.EmailService;
import com.application.service.RestaurantService;
import com.application.service.RestaurantUserService;
import com.application.web.dto.RestaurantUserAuthResponseDTO;
import com.application.web.dto.get.RestaurantUserDTO;
import com.application.web.dto.post.AuthRequestDTO;
import com.application.web.dto.post.AuthResponseDTO;
import com.application.web.dto.post.NewRestaurantDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@Tag(name = "Restaurant Authentication Controller", description = "Controller for restaurant creation and user authentication")
@RequestMapping("/public/restaurant")
@SecurityRequirement(name = "bearerAuth")
public class RestaurantAuthenticationController {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private RestaurantService restaurantService;
    private RestaurantUserService restaurantUserService;
    private MessageSource messages;
    private Environment env;
    private JwtUtil jwtUtil;
    private AuthenticationManager authenticationManager;
    private final ISecurityUserService securityRestaurantUserService;
    private final EmailService mailService;

    public RestaurantAuthenticationController(RestaurantService restaurantService,
            @Qualifier("restaurantAuthenticationManager") AuthenticationManager authenticationManager,
            RestaurantUserService restaurantUserService,
            MessageSource messages,
            Environment env,
            JwtUtil jwtUtil,
            @Qualifier("restaurantUserSecurityService") ISecurityUserService securityRestaurantUserService,
            EmailService mailService) {
        this.mailService = mailService;
        this.restaurantService = restaurantService;
        this.restaurantUserService = restaurantUserService;
        this.messages = messages;
        this.env = env;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.securityRestaurantUserService = securityRestaurantUserService;
    }

    // Public API methods
    @Operation(summary = "Request to register a new restaurant", description = "Request to register a new restaurant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Restaurant registered successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = NewRestaurantDTO.class)) }),
    })
    @PostMapping(value = "/new_restaurant")
    public GenericResponse registerRestaurant(@RequestBody NewRestaurantDTO restaurantDto) {
        LOGGER.debug("Registering restaurant with information:", restaurantDto);
        System.out.println("Registering restaurant with information:" + restaurantDto.getName());
        restaurantService.registerRestaurant(restaurantDto);
        return new GenericResponse("success");
    }

    @GetMapping(value = "/user/resend_token")
    @ResponseBody
    public GenericResponse resendRegistrationToken(final HttpServletRequest request,
            @RequestParam("token") final String existingToken) {
        RestaurantUser restaurantUser = restaurantUserService.getRestaurantUser(existingToken);
        if (restaurantUser.getRestaurant() != null &&
                restaurantUser.getRestaurant().getStatus().equals(Restaurant.Status.ENABLED)
                && restaurantUser.isEnabled()) {
            final RestaurantUserVerificationToken newToken = restaurantUserService
                    .generateNewVerificationToken(existingToken);
            mailService.sendEmail(constructResendVerificationTokenEmail(getAppUrl(request),
                    request.getLocale(), newToken, restaurantUser));
            return new GenericResponse(messages.getMessage("message.resendToken", null,
                    request.getLocale()));
        } else {
            return new GenericResponse(messages.getMessage(
                    "message.restaurantOrRestaurantUserNotEnabled", null, request.getLocale()));
        }
    }

    @Operation(summary = "Request password reset", description = "Sends a password reset token to the restaurant user's email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset token sent successfully", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json"))
    })
    @PostMapping(value = "/user/password/forgot")
    public ResponseEntity<String> forgotPassword(@RequestParam("email") final String userEmail,
            final HttpServletRequest request) {
        final RestaurantUser user = restaurantUserService.findRestaurantUserByEmail(userEmail);
        if (user == null) {
            return ResponseEntity.status(404).body(messages.getMessage("message.userNotFound", null, request.getLocale()));
        }
        String token = UUID.randomUUID().toString();
        restaurantUserService.createPasswordResetTokenForRestaurantUser(user, token);
        mailService.sendEmail(constructResetTokenEmail(getAppUrl(request), request.getLocale(), token, user));

        return ResponseEntity.ok("Password reset email sent successfully");
    }

    @Operation(summary = "Generate an authentication token", description = "Authenticates a user and returns a JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(mediaType = "application/json"))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Authentication request", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthRequestDTO.class)))
    @PostMapping(value = "/user/login", produces = "application/json")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequestDTO authenticationRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(),
                        authenticationRequest.getPassword()));

        final RestaurantUser userDetails = restaurantUserService
                .findRestaurantUserByEmail(authenticationRequest.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);
        final RestaurantUserAuthResponseDTO responseDTO = new RestaurantUserAuthResponseDTO(jwt,
                new RestaurantUserDTO(userDetails));

        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Confirm restaurant user registration", description = "Conferma la registrazione")
    @GetMapping(value = "/user/confirm_restaurant_user")
    public String confirmRestaurantUserRegistration(final HttpServletRequest request, final Model model,
            @RequestParam final String token) throws UnsupportedEncodingException {
        Locale locale = request.getLocale();
        final String result = restaurantUserService.validateVerificationToken(token);
        if (result.equals("valid")) {
            final RestaurantUser user = restaurantUserService.getRestaurantUser(token);
            authWithoutPassword(user);
            model.addAttribute("message", messages.getMessage("message.accountVerified", null, locale));
            return "redirect:/console.html?lang=" + locale.getLanguage();
        }

        model.addAttribute("message", messages.getMessage("auth.message." + result, null, locale));
        model.addAttribute("expired", "expired".equals(result));
        model.addAttribute("token", token);
        return "redirect:/public/badUser.html?lang=" + locale.getLanguage();
    }

    @Operation(summary = "Confirm password change with token", description = "Confirms the password change using a token")
    @ApiResponse(responseCode = "200", description = "Password changed successfully or invalid token", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PutMapping(value = "/user/password/confirm")
    public String confirmPasswordChange(
            @Parameter(description = "Password reset token") @RequestParam final String token) {
        final String result = securityRestaurantUserService.validatePasswordResetToken(token);
        if (result != null) {
            return "invalidToken";
        }
        return "success";
    }

    // Private helper methods
    private SimpleMailMessage constructResendVerificationTokenEmail(final String contextPath, final Locale locale,
            final RestaurantUserVerificationToken newToken, final RestaurantUser user) {
        final String confirmationUrl = contextPath + "/registrationConfirm.html?token=" + newToken.getToken();
        final String message = messages.getMessage("message.resendToken", null, locale);
        return constructEmail("Resend Registration Token", message + " \r\n" + confirmationUrl, user);
    }

    private SimpleMailMessage constructResetTokenEmail(final String contextPath, final Locale locale,
            final String token, final RestaurantUser user) {
        final String url = contextPath + "/user/changePassword?id=" + user.getId() + "&token=" + token;
        final String message = messages.getMessage("message.resetPassword", null, locale);
        return constructEmail("Reset Password", message + " \r\n" + url, user);
    }

    private SimpleMailMessage constructEmail(String subject, String body, RestaurantUser user) {
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setSubject(subject);
        email.setText(body);
        email.setTo(user.getEmail());
        email.setFrom("reservation@greedys.it");
        return email;
    }

    private String getAppUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                + request.getContextPath();
    }

    public void authWithHttpServletRequest(HttpServletRequest request, String username, String password) {
        try {
            request.login(username, password);
        } catch (ServletException e) {
            LOGGER.error("Error while login ", e);
        }
    }

    public void authWithoutPassword(RestaurantUser restaurantUser) {
        List<RestaurantPrivilege> privileges = restaurantUser.getPrivileges().stream().collect(Collectors.toList());
        List<GrantedAuthority> authorities = privileges.stream().map(p -> new SimpleGrantedAuthority(p.getName()))
                .collect(Collectors.toList());

        Authentication authentication = new UsernamePasswordAuthenticationToken(restaurantUser, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
