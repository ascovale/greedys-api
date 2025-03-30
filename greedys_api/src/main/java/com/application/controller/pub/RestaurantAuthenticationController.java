package com.application.controller.pub;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.restaurant.user.RestaurantPrivilege;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.security.jwt.JwtUtil;
import com.application.service.RestaurantService;
import com.application.service.RestaurantUserService;
import com.application.web.dto.AuthRequestDTO;
import com.application.web.dto.AuthResponseDTO;
import com.application.web.dto.RestaurantUserAuthResponseDTO;
import com.application.web.dto.get.RestaurantUserDTO;
import com.application.web.dto.post.NewRestaurantDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@Tag(name = "Restaurant Authentication Controller", description = "Controller per la creazione dei ristoranti e dell'autenticazione degli utenti del ristornate")
@RequestMapping("/public/restaurant")
@SecurityRequirement(name = "bearerAuth")
public class RestaurantAuthenticationController {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private RestaurantService restaurantService;

    private RestaurantUserService restaurantUserService;

    private MessageSource messages;

    private Environment env;

    //private EmailService mailService;

    private JwtUtil jwtUtil;

    private AuthenticationManager authenticationManager;

    public RestaurantAuthenticationController(RestaurantService restaurantService,
            @Qualifier("restaurantAuthenticationManager") AuthenticationManager authenticationManager,
            RestaurantUserService restaurantUserService,
            MessageSource messages,
            Environment env,
            //EmailService mailService,
            JwtUtil jwtUtil) {
        this.restaurantService = restaurantService;
        this.restaurantUserService = restaurantUserService;
        this.messages = messages;
        this.env = env;
        //this.mailService = mailService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }
    // Restaurant Registration

    @Operation(summary = "Request to register a new restaurant", description = "Richiesta di registrazione di un nuovo ristorante")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utente registrato con successo", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = NewRestaurantDTO.class)) }),
    })
    @PostMapping(value = "/new_restaurant")
    public GenericResponse registerRestaurant(@RequestBody NewRestaurantDTO restaurantDto) {
        LOGGER.debug("Registering restaurant with information:", restaurantDto);
        System.out.println("Registering restaurant with information:" + restaurantDto.getName());
        restaurantService.registerRestaurant(restaurantDto);
        return new GenericResponse("success");
    }
/* 
    @Operation(summary = "Apply for a restaurant")
    @PostMapping("/user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registrazione ", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = NewRestaurantDTO.class)) }),
    })
    public GenericResponse applyForRestaurant(@RequestBody NewRestaurantUserDTO userDTO) {

        restaurantUserService.registerRestaurantUser(userDTO);

        return new GenericResponse("success");
    }

    @RequestMapping(value = "/user/resend_token", method = RequestMethod.GET)
    @ResponseBody
    public GenericResponse resendRegistrationToken(final HttpServletRequest request,
            @RequestParam("token") final String existingToken) {
        RestaurantUser restaurantUser = restaurantUserService.getRestaurantUser(existingToken);
        if (restaurantUser.getRestaurant() != null && restaurantUser.getRestaurant().getStatus().equals(Restaurant.Status.ENABLED) && restaurantUser.isEnabled()) {
            final RestaurantUserVerificationToken newToken = restaurantUserService
                .generateNewVerificationToken(existingToken);
            mailService.sendEmail(constructResendVerificationTokenEmail(getAppUrl(request), request.getLocale(), newToken,
                restaurantUser));
            return new GenericResponse(messages.getMessage("message.resendToken", null, request.getLocale()));
        } else {
            return new GenericResponse(messages.getMessage("message.restaurantOrRestaurantUserNotEnabled", null, request.getLocale()));
        }
    }

    private String getAppUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                + request.getContextPath();
    }
*/
    /*
     * @RequestMapping(value = "/user/update/2fa", method = RequestMethod.POST)
     * 
     * @ResponseBody
     * public GenericResponse modifyUser2FA(@RequestParam("use2FA") final boolean
     * use2FA) throws UnsupportedEncodingException {
     * final User user = userService.updateUser2FA(use2FA);
     * if (use2FA) {
     * return new GenericResponse(userService.generateQRUrl(user));
     * }
     * return null;
     * }
     */

    // ============== NON-API ============
/* 
    private SimpleMailMessage constructResendVerificationTokenEmail(final String contextPath, final Locale locale,
            final RestaurantUserVerificationToken newToken, final RestaurantUser user) {
        final String confirmationUrl = contextPath + "/registrationConfirm.html?token=" + newToken.getToken();
        final String message = messages.getMessage("message.resendToken", null, locale);
        return constructEmail("Resend Registration Token", message + " \r\n" + confirmationUrl, user);
    }
*/
    @SuppressWarnings("unused")

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
        email.setFrom(env.getProperty("support.email"));
        return email;
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

    @Operation(summary = "Crea un token di autenticazione", description = "Autentica un utente e restituisce un token JWT", responses = {
            @ApiResponse(responseCode = "200", description = "Autenticazione riuscita", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Autenticazione fallita", content = @Content(mediaType = "application/json"))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Richiesta di autenticazione", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthRequestDTO.class)))
    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequestDTO authenticationRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(),
                            authenticationRequest.getPassword()));

            final RestaurantUser userDetails = restaurantUserService
                    .findRestaurantUserByEmail(authenticationRequest.getUsername());
            final String jwt = jwtUtil.generateToken(userDetails);
            final RestaurantUserAuthResponseDTO responseDTO = new RestaurantUserAuthResponseDTO(jwt,
                    new RestaurantUserDTO(userDetails));

            return ResponseEntity.ok(responseDTO);
        } catch (DisabledException e) {
            LOGGER.error("Authentication failed: User is disabled", e);
            return ResponseEntity.status(403).body(new GenericResponse("User is disabled"));
        } catch (Exception e) {
            LOGGER.error("Authentication failed", e);
            return ResponseEntity.status(401).body(new GenericResponse("Authentication failed"));
        }
    }
/* 
    @Operation(summary = "Confirm restaurant user registration", description = "Conferma la registrazione")
    @RequestMapping(value = "/user/confirm_restaurant_user", method = RequestMethod.GET)
    public String confirmRestaurantUserRegistration(final HttpServletRequest request, final Model model,
            @RequestParam final String token) throws UnsupportedEncodingException {
        Locale locale = request.getLocale();
        final String result = restaurantUserService.validateVerificationToken(token);
        if (result.equals("valid")) {
            final RestaurantUser user = restaurantUserService.getRestaurantUser(token);
            // if (user.isUsing2FA()) {
            // model.addAttribute("qr", userService.generateQRUrl(user));
            // return "redirect:/qrcode.html?lang=" + locale.getLanguage();
            // }
            authWithoutPassword(user);
            model.addAttribute("message", messages.getMessage("message.accountVerified", null, locale));
            return "redirect:/console.html?lang=" + locale.getLanguage();
        }

        model.addAttribute("message", messages.getMessage("auth.message." + result, null, locale));
        model.addAttribute("expired", "expired".equals(result));
        model.addAttribute("token", token);
        return "redirect:/badUser.html?lang=" + locale.getLanguage();
    }
*/
}
