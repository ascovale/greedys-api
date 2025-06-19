package com.application.service.authentication;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.application.persistence.dao.restaurant.RestaurantUserHubDAO;
import com.application.persistence.dao.restaurant.RestaurantUserPasswordResetTokenDAO;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RestaurantPrivilege;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.persistence.model.restaurant.user.RestaurantUserHub;
import com.application.persistence.model.restaurant.user.RestaurantUserPasswordResetToken;
import com.application.persistence.model.restaurant.user.RestaurantUserVerificationToken;
import com.application.security.google2fa.RestaurantUserAuthenticationDetails;
import com.application.security.jwt.JwtUtil;
import com.application.security.user.ISecurityUserService;
import com.application.service.EmailService;
import com.application.service.RestaurantUserService;
import com.application.web.dto.get.RestaurantDTO;
import com.application.web.dto.get.RestaurantUserDTO;
import com.application.web.dto.post.AuthRequestDTO;
import com.application.web.dto.post.AuthResponseDTO;
import com.application.web.dto.post.RestaurantUserSelectRequestDTO;
import com.application.web.util.GenericResponse;

import io.jsonwebtoken.Claims;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

@Service
@Transactional
public class RestaurantAuthenticationService {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final AuthenticationManager authenticationManager;
    private final RestaurantUserService restaurantUserService;
    private final JwtUtil jwtUtil;
    private final RestaurantUserPasswordResetTokenDAO passwordTokenRepository;
    private final MessageSource messages;
    private final EmailService mailService;
    private final ISecurityUserService securityRestaurantUserService;
    private final RestaurantUserDAO restaurantUserDAO;
    private final RestaurantUserHubDAO restaurantUserHubDAO;
    private final PasswordEncoder passwordEncoder;

    public RestaurantAuthenticationService(
            @Qualifier("restaurantAuthenticationManager") AuthenticationManager authenticationManager,
            RestaurantUserService restaurantUserService,
            RestaurantUserDAO restaurantUserDAO,
            JwtUtil jwtUtil,
            RestaurantUserPasswordResetTokenDAO passwordTokenRepository,
            EmailService mailService,
            @Qualifier("restaurantUserSecurityService") ISecurityUserService securityRestaurantUserService,
            MessageSource messages,
            RestaurantUserHubDAO restaurantUserHubDAO,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.restaurantUserService = restaurantUserService;
        this.jwtUtil = jwtUtil;
        this.passwordTokenRepository = passwordTokenRepository;
        this.messages = messages;
        this.mailService = mailService;
        this.securityRestaurantUserService = securityRestaurantUserService;
        this.restaurantUserDAO = restaurantUserDAO;
        this.restaurantUserHubDAO = restaurantUserHubDAO;
        this.passwordEncoder = passwordEncoder;
    }

    /*
     * public AuthResponseDTO login(AuthRequestDTO authenticationRequest) {
     * RestaurantUser userDetails;
     * if (authenticationRequest.getRestaurantId() == null ||
     * authenticationRequest.getRestaurantId() == 0) {
     * // Cerca il primo RestaurantUser associato al RestaurantUserHub
     * System.out.
     * println("Restaurant ID is null or zero, searching for RestaurantUserHub.");
     * RestaurantUser hubUser =
     * restaurantUserDAO.findByEmail(authenticationRequest.getUsername());
     * if (hubUser == null || hubUser.getRestaurantUserHub() == null) {
     * throw new
     * IllegalArgumentException("Invalid email or no associated RestaurantUserHub found."
     * );
     * }
     * List<RestaurantUser> associatedUsers = restaurantUserDAO
     * .findAllByRestaurantUserHubId(hubUser.getRestaurantUserHub().getId());
     * if (associatedUsers.isEmpty()) {
     * throw new IllegalArgumentException(
     * "No associated RestaurantUser found for the given RestaurantUserHub.");
     * }
     * authenticationManager.authenticate(
     * new UsernamePasswordAuthenticationToken(
     * authenticationRequest.getUsername() + ":" +
     * associatedUsers.get(0).getRestaurant().getId(),
     * authenticationRequest.getPassword()));
     * 
     * userDetails = associatedUsers.get(0); // Prendi il primo utente associato
     * } else {
     * authenticationManager.authenticate(
     * new UsernamePasswordAuthenticationToken(
     * authenticationRequest.getUsername() + ":" +
     * authenticationRequest.getRestaurantId(),
     * authenticationRequest.getPassword()));
     * 
     * // Cerca il RestaurantUser specifico per email e restaurantId
     * userDetails =
     * restaurantUserDAO.findByEmailAndRestaurantId(authenticationRequest.
     * getUsername(),
     * authenticationRequest.getRestaurantId());
     * if (userDetails == null) {
     * throw new IllegalArgumentException(
     * "Restaurant not found or user does not have access to this restaurant.");
     * }
     * }
     * String jwt = jwtUtil.generateToken(userDetails);
     * return new AuthResponseDTO(jwt, new RestaurantUserDTO(userDetails));
     * }
     */
    public AuthResponseDTO adminLoginToRestaurantUser(Long restaurantUserId, HttpServletRequest request) {
        RestaurantUser user = restaurantUserDAO.findById(restaurantUserId)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with ID: " + restaurantUserId));

        // Creazione di un token di autenticazione con bypass della password
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null);
        authToken.setDetails(
                new RestaurantUserAuthenticationDetails(true, user.getRestaurant().getId(), user.getEmail()));

        // Autenticazione senza password
        SecurityContextHolder.getContext().setAuthentication(authToken);

        final String jwt = jwtUtil.generateToken(user);
        return new AuthResponseDTO(jwt, new RestaurantUserDTO(user));
    }

    public ResponseEntity<String> createPasswordResetTokenForRestaurantUser(final RestaurantUser ru,
            final String token) {
        final RestaurantUserPasswordResetToken myToken = new RestaurantUserPasswordResetToken(token, ru);
        passwordTokenRepository.save(myToken);
        try {
            // Logica per mandarlo per email
            // SimpleMailMessage email = constructEmail("Reset Password", "Reset your
            // password", ru);
            return ResponseEntity.ok("Password reset token send successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error sending password reset token");
        }
    }

    public ResponseEntity<String> forgotPassword(final String userEmail, final HttpServletRequest request) {

        final RestaurantUser user = restaurantUserService.findRestaurantUserByEmail(userEmail);
        if (user == null) {
            return ResponseEntity.status(404)
                    .body(messages.getMessage("message.userNotFound", null, request.getLocale()));
        }
        String token = UUID.randomUUID().toString();
        restaurantUserService.createPasswordResetTokenForRestaurantUser(user, token);
        mailService.sendEmail(constructResetTokenEmail(getAppUrl(request), request.getLocale(), token, user));

        return ResponseEntity.ok("Password reset email sent successfully");
    }

    public GenericResponse resendRegistrationToken(final HttpServletRequest request, final String existingToken) {
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

    public String confirmRestaurantUserRegistration(final HttpServletRequest request, final Model model,
            final String token) {
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

    public String confirmPasswordChange(final String token) {
        final String result = securityRestaurantUserService.validatePasswordResetToken(token);
        if (result != null) {
            return "invalidToken";
        }
        return "success";
    }

    public AuthResponseDTO changeRestaurant(Long restaurantId) {
        final RestaurantUser currentUser = (RestaurantUser) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        final RestaurantUser updatedUser = restaurantUserDAO.findByEmailAndRestaurantId(currentUser.getEmail(),
                restaurantId);
        if (updatedUser == null) {
            throw new IllegalArgumentException("Restaurant not found or user does not have access to this restaurant.");
        }
        if (!updatedUser.isEnabled()) {
            throw new IllegalArgumentException("User is not enabled.");
        }
        final String newJwt = jwtUtil.generateToken(updatedUser);
        return new AuthResponseDTO(newJwt, new RestaurantUserDTO(updatedUser));
    }

    public AuthResponseDTO selectRestaurant(String token,RestaurantUserSelectRequestDTO selectRequest) {
        // Decodifica il JWT hub
        Claims claims;
        try {
            claims = jwtUtil.extractAllClaims(token);
        } catch (Exception e) {
            throw new UnsupportedOperationException("Invalid hub token.");
        }
        // Verifica che sia un token di tipo hub
        if (!"hub".equals(claims.get("type"))) {
            throw new UnsupportedOperationException("Token is not a hub token.");
        }
        Long hubId = claims.get("hubId", Long.class);
        String email = claims.get("email", String.class);

        // Trova il RestaurantUser con quell'hubId e restaurantId
        RestaurantUser user = restaurantUserDAO.findByEmailAndRestaurantId(email, selectRequest.getRestaurantId());
        if (user == null || user.getRestaurantUserHub() == null || !user.getRestaurantUserHub().getId().equals(hubId)) {
            throw new UnsupportedOperationException("User does not have access to this restaurant.");
        }
        if (!user.isEnabled()) {
            throw new UnsupportedOperationException("User is not enabled for this restaurant.");
        }
        String jwt = jwtUtil.generateToken(user);
        return new AuthResponseDTO(jwt, new RestaurantUserDTO(user));
    }

    private String getAppUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                + request.getContextPath();
    }

    private SimpleMailMessage constructResetTokenEmail(final String contextPath, final Locale locale,
            final String token, final RestaurantUser user) {
        final String url = contextPath + "/changePassword?id=" + user.getId() + "&token=" + token;
        final String message = messages.getMessage("message.resetPassword", null, locale);
        return constructEmail("Reset Password", message + " \r\n" + url, user);
    }

    private SimpleMailMessage constructResendVerificationTokenEmail(final String contextPath, final Locale locale,
            final RestaurantUserVerificationToken newToken, final RestaurantUser restaurantUser) {
        final String confirmationUrl = contextPath + "/registrationConfirm.html?token=" + newToken.getToken();
        final String message = messages.getMessage("message.resendToken", null, locale);
        return constructEmail("Resend Registration Token", message + " \r\n" + confirmationUrl, restaurantUser);
    }

    private SimpleMailMessage constructEmail(String subject, String body, RestaurantUser user) {
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setSubject(subject);
        email.setText(body);
        email.setTo(user.getEmail());
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

    public void authWithoutPassword(RestaurantUser restaurantUser) {
        List<RestaurantPrivilege> privileges = restaurantUser.getPrivileges().stream().collect(Collectors.toList());
        List<GrantedAuthority> authorities = privileges.stream().map(p -> new SimpleGrantedAuthority(p.getName()))
                .collect(Collectors.toList());

        Authentication authentication = new UsernamePasswordAuthenticationToken(restaurantUser, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public List<RestaurantDTO> getRestaurantsForUserHub(String email) {
        return restaurantUserHubDAO.findAllRestaurantsByHubEmail(email)
        .stream()
        .map(RestaurantDTO::new)
        .collect(Collectors.toList());
    }

    public AuthResponseDTO loginWithHubSupport(AuthRequestDTO authenticationRequest) {
        // Trova il RestaurantUser associato all'email
        RestaurantUserHub user = restaurantUserHubDAO.findByEmail(authenticationRequest.getUsername());
        if (user == null) {
            throw new UnsupportedOperationException("Invalid username or password.");
        }

        // Verifica la password
        if (!passwordEncoder.matches(authenticationRequest.getPassword(), user.getPassword())) {
            throw new UnsupportedOperationException("Invalid username or password.");
        }

        // Recupera tutti i RestaurantUser associati a questo hub
        List<RestaurantUser> associatedUsers = restaurantUserDAO
                .findAllByRestaurantUserHubId(user.getId());

        if (associatedUsers == null || associatedUsers.isEmpty()) {
            throw new UnsupportedOperationException("No restaurants associated with this user.");
        }
        System.out.println("\n\n\n\n\n>>>>Associated users: " + associatedUsers.size());
        if (associatedUsers.size() == 1) {
            // Login classico: un solo ristorante
            RestaurantUser singleUser = associatedUsers.get(0);
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.getUsername() + ":" + singleUser.getRestaurant().getId(),
                            authenticationRequest.getPassword()));
            String jwt = jwtUtil.generateToken(singleUser);
            return new AuthResponseDTO(jwt, new RestaurantUserDTO(singleUser));
        } else {
            // Login intermedio: più ristoranti
            // Genera un JWT "hub" (puoi aggiungere un claim "type":"hub" se vuoi)
            // e restituisci la lista dei ristoranti
            // NB: qui non serve autenticare con restaurantId, basta email/password

            // Genera JWT hub (puoi usare un metodo dedicato, qui esempio semplice)
            String hubJwt = jwtUtil.generateHubToken(user);

            AuthResponseDTO hubResponse = new AuthResponseDTO(hubJwt, user);

            return hubResponse;
        }
    }

    public List<RestaurantUser> getAssociatedUsersByHubId(Long hubId) {
        return restaurantUserDAO.findAllByRestaurantUserHubId(hubId);
    }

    public List<RestaurantDTO> getRestaurantsByUserHubId(Long userHubId) {
        return restaurantUserHubDAO.findAllRestaurantsByHubId(userHubId)
                .stream()
                .map(RestaurantDTO::new)
                .collect(Collectors.toList());
    }

}
