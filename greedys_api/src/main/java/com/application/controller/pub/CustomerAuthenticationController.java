package com.application.controller.pub;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.customer.Privilege;
import com.application.persistence.model.customer.VerificationToken;
import com.application.registration.CustomerOnRegistrationCompleteEvent;
import com.application.security.jwt.JwtUtil;
import com.application.service.CustomerService;
import com.application.service.EmailService;
import com.application.web.dto.AuthRequestGoogleDTO;
import com.application.web.dto.get.CustomerDTO;
import com.application.web.dto.post.AuthRequestDTO;
import com.application.web.dto.post.AuthResponseDTO;
import com.application.web.dto.post.NewCustomerDTO;
import com.application.web.util.GenericResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@Tag(name = "Customer Authentication Controller", description = "Controller per la gestione dell'autenticazione dei customer")
@RequestMapping("/public/customer")
public class CustomerAuthenticationController {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private CustomerService userService;

    private MessageSource messages;

    private ApplicationEventPublisher eventPublisher;

    private Environment env;

    private EmailService mailService;

    private AuthenticationManager authenticationManager;

    private JwtUtil jwtUtil;

    private CustomerService customerService;

    private static final Logger logger = LoggerFactory.getLogger(CustomerAuthenticationController.class);

    @Autowired
    public CustomerAuthenticationController(CustomerService userService, MessageSource messages,
        ApplicationEventPublisher eventPublisher, Environment env, EmailService mailService,
        @Qualifier("customerAuthenticationManager")AuthenticationManager authenticationManager, 
        JwtUtil jwtUtil, CustomerService customerService) {
        this.userService = userService;
        this.messages = messages;
        this.eventPublisher = eventPublisher;
        this.env = env;
        this.mailService = mailService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.customerService = customerService;
    }
    public CustomerAuthenticationController() {
        super();
    }

    private String getAppUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                + request.getContextPath();
    }

    // Registration
    @Operation(summary = "Registra un nuovo utente customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer registrato con successo", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = NewCustomerDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida", content = @Content),
            @ApiResponse(responseCode = "500", description = "Errore interno del server", content = @Content) })
    @PostMapping("/new_customer")
    public ResponseEntity<String> registerCustomerAccount(@Valid @RequestBody NewCustomerDTO accountDto,
            HttpServletRequest request) {
        try {
            Customer user = userService.registerNewCustomerAccount(accountDto);
            eventPublisher
                    .publishEvent(new CustomerOnRegistrationCompleteEvent(user, Locale.ITALIAN, getAppUrl(request)));
            return ResponseEntity.ok("Customer registered successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @RequestMapping(value = "/confirm", method = RequestMethod.GET)
    @ResponseBody
    public GenericResponse confirmRegistrationResponse(final HttpServletRequest request,
            @RequestParam final String token) throws UnsupportedEncodingException {
        Locale locale = request.getLocale();
        final String result = userService.validateVerificationToken(token);
        if (result.equals("valid")) {
            final Customer user = userService.getCustomer(token);
            authWithoutPassword(user);
            return new GenericResponse(messages.getMessage("message.accountVerified", null, locale));
        }

        return new GenericResponse(
                messages.getMessage("auth.message." + result, null, locale) + "expired".equals(result));
    }

    @RequestMapping(value = "/resend_token", method = RequestMethod.GET)
    @ResponseBody
    public GenericResponse resendRegistrationToken(final HttpServletRequest request,
            @RequestParam("token") final String existingToken) {
        final VerificationToken newToken = userService.generateNewVerificationToken(existingToken);
        Customer customer = userService.getCustomer(newToken.getToken());
        mailService.sendEmail(
                constructResendVerificationTokenEmail(getAppUrl(request), request.getLocale(), newToken, customer));
        return new GenericResponse(messages.getMessage("message.resendToken", null, request.getLocale()));
    }

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

    private SimpleMailMessage constructResendVerificationTokenEmail(final String contextPath, final Locale locale,
            final VerificationToken newToken, final Customer customer) {
        final String confirmationUrl = contextPath + "/registrationConfirm.html?token=" + newToken.getToken();
        final String message = messages.getMessage("message.resendToken", null, locale);
        return constructEmail("Resend Registration Token", message + " \r\n" + confirmationUrl, customer);
    }

    @SuppressWarnings("unused")
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
            LOGGER.error("Error while login ", e);
        }
    }

    /*
     * 
     * public void authWithAuthManager(HttpServletRequest request, String username,
     * String password) {
     * UsernamePasswordAuthenticationToken authToken = new
     * UsernamePasswordAuthenticationToken(username, password);
     * authToken.setDetails(new WebAuthenticationDetails(request));
     * Authentication authentication =
     * authenticationManager.authenticate(authToken);
     * SecurityContextHolder.getContext().setAuthentication(authentication);
     * // request.getSession().setAttribute(HttpSessionSecurityContextRepository.
     * SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
     * }
     */

    public void authWithoutPassword(Customer customer) {
        List<Privilege> privileges = customer.getRoles().stream().map(role -> role.getPrivileges())
                .flatMap(list -> list.stream()).distinct().collect(Collectors.toList());
        List<GrantedAuthority> authorities = privileges.stream().map(p -> new SimpleGrantedAuthority(p.getName()))
                .collect(Collectors.toList());

        Authentication authentication = new UsernamePasswordAuthenticationToken(customer, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Operation(summary = "Crea un token di autenticazione", description = "Autentica un utente e restituisce un token JWT", responses = {
            @ApiResponse(responseCode = "200", description = "Autenticazione riuscita", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Autenticazione fallita", content = @Content(mediaType = "application/json"))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Richiesta di autenticazione", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthRequestDTO.class)))
    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<AuthResponseDTO> createAuthenticationToken(@RequestBody AuthRequestDTO authenticationRequest)
            throws Exception {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(),
                        authenticationRequest.getPassword()));

        final Customer userDetails = customerService.findUserByEmail(authenticationRequest.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);
        final AuthResponseDTO responseDTO = new AuthResponseDTO(jwt, new CustomerDTO(userDetails));

        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Autentica con Google", description = "Autentica un utente utilizzando un token di Google e restituisce un token JWT", responses = {
            @ApiResponse(responseCode = "200", description = "Autenticazione riuscita", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Autenticazione fallita", content = @Content(mediaType = "application/json"))
    })

    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Richiesta di autenticazione con Google", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthRequestGoogleDTO.class)))
    @PostMapping("/google")
    public ResponseEntity<AuthResponseDTO> authenticateWithGoogle(@RequestBody AuthRequestGoogleDTO authRequest)
            throws Exception {
        logger.warn("Received Google authentication request: {}", authRequest.getToken());
        GoogleIdToken idToken = verifyGoogleToken(authRequest.getToken());

        if (idToken != null) {
            String email = idToken.getPayload().getEmail();
            String name = (String) idToken.getPayload().get("name");
            logger.warn("Google token verified. Email: {}, Name: {}", email, name);
            // quali dati vogliamo prendere da google?
            Customer user = customerService.findUserByEmail(email);
            if (user == null) {
                NewCustomerDTO accountDto = new NewCustomerDTO();
                // devo verificare questa cosa
                accountDto.setFirstName(name.split(" ")[0]);
                accountDto.setLastName(name.split(" ")[1]);
                accountDto.setEmail(email);
                accountDto.setPassword(generateRandomPassword()); // Generate and set a random password
                user = customerService.registerNewCustomerAccount(accountDto);
            }
            String jwt = jwtUtil.generateToken(user);
            return ResponseEntity.ok(new AuthResponseDTO(jwt, new CustomerDTO(user)));
        } else {
            logger.warn("Google token verification failed.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private GoogleIdToken verifyGoogleToken(String token) throws Exception {
        try {
            logger.debug("Verifying Google token... {}", token);
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
                logger.debug("Google token verified successfully.");
            } else {
                logger.warn("Google token verification failed: Invalid token.");
            }
            return idToken;
        } catch (GeneralSecurityException e) {
            logger.error("Google token verification failed: GeneralSecurityException - {}", e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Google token verification failed: IOException - {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Google token verification failed: Exception - {}", e.getMessage(), e);
        }
        return null;
    }

    private String generateRandomPassword() {
        // Implement a method to generate a random password
        return UUID.randomUUID().toString();
    }


}
