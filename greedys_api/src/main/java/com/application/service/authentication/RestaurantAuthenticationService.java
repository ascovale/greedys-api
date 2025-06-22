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

import com.application.persistence.dao.restaurant.RUserDAO;
import com.application.persistence.dao.restaurant.RUserHubDAO;
import com.application.persistence.dao.restaurant.RUserPasswordResetTokenDAO;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RestaurantPrivilege;
import com.application.persistence.model.restaurant.user.RUser;
import com.application.persistence.model.restaurant.user.RUserHub;
import com.application.persistence.model.restaurant.user.RUserPasswordResetToken;
import com.application.persistence.model.restaurant.user.RUserVerificationToken;
import com.application.security.google2fa.RUserAuthenticationDetails;
import com.application.security.jwt.JwtUtil;
import com.application.security.user.ISecurityUserService;
import com.application.service.EmailService;
import com.application.service.RUserService;
import com.application.web.dto.get.RestaurantDTO;
import com.application.web.dto.AuthRequestGoogleDTO;
import com.application.web.dto.get.RUserDTO;
import com.application.web.dto.post.AuthRequestDTO;
import com.application.web.dto.post.AuthResponseDTO;
import com.application.web.dto.post.NewCustomerDTO;
import com.application.web.dto.post.RUserSelectRequestDTO;
import com.application.web.util.GenericResponse;

import io.jsonwebtoken.Claims;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

@Service
@Transactional
public class RestaurantAuthenticationService {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final AuthenticationManager authenticationManager;
    private final GoogleAuthService googleAuthService;
    private final RUserService RUserService;
    private final JwtUtil jwtUtil;
    private final RUserPasswordResetTokenDAO passwordTokenRepository;
    private final MessageSource messages;
    private final EmailService mailService;
    private final ISecurityUserService securityRUserService;
    private final RUserDAO RUserDAO;
    private final RUserHubDAO RUserHubDAO;
    private final PasswordEncoder passwordEncoder;

    public RestaurantAuthenticationService(
            @Qualifier("restaurantAuthenticationManager") AuthenticationManager authenticationManager,
            RUserService RUserService,
            RUserDAO RUserDAO,
            JwtUtil jwtUtil,
            RUserPasswordResetTokenDAO passwordTokenRepository,
            EmailService mailService,
            @Qualifier("RUserSecurityService") ISecurityUserService securityRUserService,
            MessageSource messages,
            RUserHubDAO RUserHubDAO,
            PasswordEncoder passwordEncoder,
            GoogleAuthService googleAuthService) {
        this.authenticationManager = authenticationManager;
        this.RUserService = RUserService;
        this.jwtUtil = jwtUtil;
        this.passwordTokenRepository = passwordTokenRepository;
        this.messages = messages;
        this.mailService = mailService;
        this.securityRUserService = securityRUserService;
        this.RUserDAO = RUserDAO;
        this.RUserHubDAO = RUserHubDAO;
        this.passwordEncoder = passwordEncoder;
        this.googleAuthService = googleAuthService;
    }

    /*
     * public AuthResponseDTO login(AuthRequestDTO authenticationRequest) {
     * RUser userDetails;
     * if (authenticationRequest.getRestaurantId() == null ||
     * authenticationRequest.getRestaurantId() == 0) {
     * // Cerca il primo RUser associato al RUserHub
     * System.out.
     * println("Restaurant ID is null or zero, searching for RUserHub.");
     * RUser hubUser =
     * RUserDAO.findByEmail(authenticationRequest.getUsername());
     * if (hubUser == null || hubUser.getRUserHub() == null) {
     * throw new
     * IllegalArgumentException("Invalid email or no associated RUserHub found."
     * );
     * }
     * List<RUser> associatedUsers = RUserDAO
     * .findAllByRUserHubId(hubUser.getRUserHub().getId());
     * if (associatedUsers.isEmpty()) {
     * throw new IllegalArgumentException(
     * "No associated RUser found for the given RUserHub.");
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
     * // Cerca il RUser specifico per email e restaurantId
     * userDetails =
     * RUserDAO.findByEmailAndRestaurantId(authenticationRequest.
     * getUsername(),
     * authenticationRequest.getRestaurantId());
     * if (userDetails == null) {
     * throw new IllegalArgumentException(
     * "Restaurant not found or user does not have access to this restaurant.");
     * }
     * }
     * String jwt = jwtUtil.generateToken(userDetails);
     * return new AuthResponseDTO(jwt, new RUserDTO(userDetails));
     * }
     */
    public AuthResponseDTO adminLoginToRUser(Long RUserId, HttpServletRequest request) {
        RUser user = RUserDAO.findById(RUserId)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with ID: " + RUserId));

        // Creazione di un token di autenticazione con bypass della password
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null);
        authToken.setDetails(
                new RUserAuthenticationDetails(true, user.getRestaurant().getId(), user.getEmail()));

        // Autenticazione senza password
        SecurityContextHolder.getContext().setAuthentication(authToken);

        final String jwt = jwtUtil.generateToken(user);
        return new AuthResponseDTO(jwt, new RUserDTO(user));
    }

    public ResponseEntity<String> createPasswordResetTokenForRUser(final RUser ru,
            final String token) {
        final RUserPasswordResetToken myToken = new RUserPasswordResetToken(token, ru);
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

        final RUser user = RUserService.findRUserByEmail(userEmail);
        if (user == null) {
            return ResponseEntity.status(404)
                    .body(messages.getMessage("message.userNotFound", null, request.getLocale()));
        }
        String token = UUID.randomUUID().toString();
        RUserService.createPasswordResetTokenForRUser(user, token);
        mailService.sendEmail(constructResetTokenEmail(getAppUrl(request), request.getLocale(), token, user));

        return ResponseEntity.ok("Password reset email sent successfully");
    }

    public GenericResponse resendRegistrationToken(final HttpServletRequest request, final String existingToken) {
        RUser RUser = RUserService.getRUser(existingToken);
        if (RUser.getRestaurant() != null &&
                RUser.getRestaurant().getStatus().equals(Restaurant.Status.ENABLED)
                && RUser.isEnabled()) {
            final RUserVerificationToken newToken = RUserService
                    .generateNewVerificationToken(existingToken);
            mailService.sendEmail(constructResendVerificationTokenEmail(getAppUrl(request),
                    request.getLocale(), newToken, RUser));
            return new GenericResponse(messages.getMessage("message.resendToken", null,
                    request.getLocale()));
        } else {
            return new GenericResponse(messages.getMessage(
                    "message.restaurantOrRUserNotEnabled", null, request.getLocale()));
        }
    }

    public String confirmRUserRegistration(final HttpServletRequest request, final Model model,
            final String token) {
        Locale locale = request.getLocale();
        final String result = RUserService.validateVerificationToken(token);

        if (result.equals("valid")) {
            final RUser user = RUserService.getRUser(token);
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
        final String result = securityRUserService.validatePasswordResetToken(token);
        if (result != null) {
            return "invalidToken";
        }
        return "success";
    }

    public AuthResponseDTO changeRestaurant(Long restaurantId) {
        final RUser currentUser = (RUser) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        final RUser updatedUser = RUserDAO.findByEmailAndRestaurantId(currentUser.getEmail(),
                restaurantId);
        if (updatedUser == null) {
            throw new IllegalArgumentException("Restaurant not found or user does not have access to this restaurant.");
        }
        if (!updatedUser.isEnabled()) {
            throw new IllegalArgumentException("User is not enabled.");
        }
        final String newJwt = jwtUtil.generateToken(updatedUser);
        return new AuthResponseDTO(newJwt, new RUserDTO(updatedUser));
    }

    public AuthResponseDTO selectRestaurant(Long restaurantId) {
        // Recupera l'email dell'utente autenticato
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // Trova il RUser con quell'hubId e restaurantId
        RUser user = RUserDAO.findByEmailAndRestaurantId(email, restaurantId);
        if (user == null) {
            throw new UnsupportedOperationException("User does not have access to this restaurant.");
        }
        if (!user.isEnabled()) {
            throw new UnsupportedOperationException("User is not enabled for this restaurant.");
        }
        String jwt = jwtUtil.generateToken(user);
        return new AuthResponseDTO(jwt, new RUserDTO(user));
    }

    private String getAppUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                + request.getContextPath();
    }

    private SimpleMailMessage constructResetTokenEmail(final String contextPath, final Locale locale,
            final String token, final RUser user) {
        final String url = contextPath + "/changePassword?id=" + user.getId() + "&token=" + token;
        final String message = messages.getMessage("message.resetPassword", null, locale);
        return constructEmail("Reset Password", message + " \r\n" + url, user);
    }

    private SimpleMailMessage constructResendVerificationTokenEmail(final String contextPath, final Locale locale,
            final RUserVerificationToken newToken, final RUser RUser) {
        final String confirmationUrl = contextPath + "/registrationConfirm.html?token=" + newToken.getToken();
        final String message = messages.getMessage("message.resendToken", null, locale);
        return constructEmail("Resend Registration Token", message + " \r\n" + confirmationUrl, RUser);
    }

    private SimpleMailMessage constructEmail(String subject, String body, RUser user) {
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

    public void authWithoutPassword(RUser RUser) {
        List<RestaurantPrivilege> privileges = RUser.getPrivileges().stream().collect(Collectors.toList());
        List<GrantedAuthority> authorities = privileges.stream().map(p -> new SimpleGrantedAuthority(p.getName()))
                .collect(Collectors.toList());

        Authentication authentication = new UsernamePasswordAuthenticationToken(RUser, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public List<RestaurantDTO> getRestaurantsForUserHub(String email) {
        return RUserHubDAO.findAllRestaurantsByHubEmail(email)
        .stream()
        .map(RestaurantDTO::new)
        .collect(Collectors.toList());
    }

    public AuthResponseDTO loginWithHubSupport(AuthRequestDTO authenticationRequest) {
        // Trova il RUser associato all'email
        RUserHub user = RUserHubDAO.findByEmail(authenticationRequest.getUsername());
        if (user == null) {
            throw new UnsupportedOperationException("Invalid username or password.");
        }

        // Verifica la password
        if (!passwordEncoder.matches(authenticationRequest.getPassword(), user.getPassword())) {
            throw new UnsupportedOperationException("Invalid username or password.");
        }

        // Recupera tutti i RUser associati a questo hub
        List<RUser> associatedUsers = RUserDAO
                .findAllByRUserHubId(user.getId());

        if (associatedUsers == null || associatedUsers.isEmpty()) {
            throw new UnsupportedOperationException("No restaurants associated with this user.");
        }
        System.out.println("\n\n\n\n\n>>>>Associated users: " + associatedUsers.size());
        if (associatedUsers.size() == 1) {
            // Login classico: un solo ristorante
            RUser singleUser = associatedUsers.get(0);
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.getUsername() + ":" + singleUser.getRestaurant().getId(),
                            authenticationRequest.getPassword()));
            String jwt = jwtUtil.generateToken(singleUser);
            return new AuthResponseDTO(jwt, new RUserDTO(singleUser));
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

    public List<RUser> getAssociatedUsersByHubId(Long hubId) {
        return RUserDAO.findAllByRUserHubId(hubId);
    }

    public List<RestaurantDTO> getRestaurantsByUserHubId(Long userHubId) {
        return RUserHubDAO.findAllRestaurantsByHubId(userHubId)
                .stream()
                .map(RestaurantDTO::new)
                .collect(Collectors.toList());
    }

    public AuthResponseDTO loginWithGoogle(AuthRequestGoogleDTO authenticationRequest) {
		try {
			return googleAuthService.authenticateWithGoogle(authenticationRequest, 
					RUserHubDAO::findByEmail,
					(email, idToken) -> {
                        String[] name = idToken.getPayload().get("name").toString().split(" ");
                        RUserHub newUser = new RUserHub();
                        newUser.setEmail(email);
                        newUser.setFirstName(name[0]);
                        newUser.setLastName(name[1]);
                        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                        return RUserHubDAO.save(newUser);
                    },
                    jwtUtil::generateHubToken
					);
		} catch (Exception e) {
			LOGGER.error("Google authentication failed: {}", e.getMessage(), e);
			throw new RuntimeException("Google authentication failed: " + e.getMessage(), e);
		}
	}

}
