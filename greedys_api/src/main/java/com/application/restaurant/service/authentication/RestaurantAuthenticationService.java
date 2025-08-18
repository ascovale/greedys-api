package com.application.restaurant.service.authentication;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import com.application.common.persistence.mapper.RUserMapper;
import com.application.common.security.jwt.JwtUtil;
import com.application.common.service.EmailService;
import com.application.common.service.authentication.GoogleAuthService;
import com.application.common.web.dto.restaurant.RestaurantDTO;
import com.application.common.web.dto.security.AuthRequestDTO;
import com.application.common.web.dto.security.AuthRequestGoogleDTO;
import com.application.common.web.dto.security.AuthResponseDTO;
import com.application.restaurant.persistence.dao.RUserDAO;
import com.application.restaurant.persistence.dao.RUserHubDAO;
import com.application.restaurant.persistence.dao.RUserPasswordResetTokenDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.persistence.model.user.RUserHub;
import com.application.restaurant.persistence.model.user.RUserPasswordResetToken;
import com.application.restaurant.persistence.model.user.RUserVerificationToken;
import com.application.restaurant.persistence.model.user.RestaurantPrivilege;
import com.application.restaurant.service.RUserService;
import com.application.restaurant.service.security.RUserSecurityService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class RestaurantAuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final GoogleAuthService googleAuthService;
    private final RUserService RUserService;
    private final JwtUtil jwtUtil;
    private final RUserPasswordResetTokenDAO passwordTokenRepository;
    private final MessageSource messages;
    private final EmailService mailService;
    private final RUserSecurityService securityRUserService;
    private final RUserDAO RUserDAO;
    private final RUserHubDAO RUserHubDAO;
    private final PasswordEncoder passwordEncoder;
    private final RUserMapper rUserMapper;

    public RestaurantAuthenticationService(
            @Qualifier("restaurantAuthenticationManager") AuthenticationManager authenticationManager,
            RUserService RUserService,
            RUserDAO RUserDAO,
            JwtUtil jwtUtil,
            RUserPasswordResetTokenDAO passwordTokenRepository,
            EmailService mailService,
            RUserSecurityService securityRUserService,
            MessageSource messages,
            RUserHubDAO RUserHubDAO,
            PasswordEncoder passwordEncoder,
            GoogleAuthService googleAuthService,
            RUserMapper rUserMapper) {
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
        this.rUserMapper = rUserMapper;
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

    public void resendRegistrationToken(final HttpServletRequest request, final String existingToken) {
        RUser RUser = RUserService.getRUser(existingToken);
        if (RUser.getRestaurant() != null &&
                RUser.getRestaurant().getStatus().equals(Restaurant.Status.ENABLED)
                && RUser.isEnabled()) {  
                    throw new BadCredentialsException("Invalid token");
            }
        
            final RUserVerificationToken newToken = RUserService
                    .generateNewVerificationToken(existingToken);
            mailService.sendEmail(constructResendVerificationTokenEmail(getAppUrl(request),
                    request.getLocale(), newToken, RUser));
            log.info("Resend verification token for user: {}", RUser.getEmail());
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
            throw new BadCredentialsException("Restaurant not found or user does not have access to this restaurant.");
        }
        if (!updatedUser.isEnabled()) {
            throw new DisabledException("User is not enabled.");
        }
        final String newJwt = jwtUtil.generateToken(updatedUser);
        return new AuthResponseDTO(newJwt, rUserMapper.toDTO(updatedUser));
    }

    public AuthResponseDTO selectRestaurant(Long restaurantId) {
        // Recupera l'email dell'utente autenticato
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // Trova il RUser con quell'hubId e restaurantId
        RUser user = RUserDAO.findByEmailAndRestaurantId(email, restaurantId);
        if (user == null) {
            throw new BadCredentialsException("User does not have access to this restaurant.");
        }
        if (!user.isEnabled()) {
            throw new DisabledException("User is not enabled for this restaurant.");
        }
        String jwt = jwtUtil.generateToken(user);
        return new AuthResponseDTO(jwt, rUserMapper.toDTO(user));
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
            log.error("Error while login ", e);
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
            throw new BadCredentialsException("Invalid username or password.");
        }

        // Verifica la password
        if (!passwordEncoder.matches(authenticationRequest.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password.");
        }
        
        log.debug("Password verified successfully for user: {}", authenticationRequest.getUsername());

        // Recupera tutti i RUser associati a questo hub
        List<RUser> associatedUsers = RUserDAO
                .findAllByRUserHubId(user.getId());

        log.debug("Found {} associated users for hub ID: {}", 
                 associatedUsers != null ? associatedUsers.size() : 0, user.getId());

        if (associatedUsers == null || associatedUsers.isEmpty()) {
            log.debug("No restaurants associated with user: {}", authenticationRequest.getUsername());
            throw new BadCredentialsException("No restaurants associated with this user.");
        }
        
        log.debug("Associated users found: {} for hub: {}", associatedUsers.size(), user.getEmail());
        
        if (associatedUsers.size() == 1) {
            // Login classico: un solo ristorante
            RUser singleUser = associatedUsers.get(0);
            
            log.debug("Single user found - ID: {}, Status: {}, Restaurant ID: {}, Restaurant Status: {}", 
                     singleUser.getId(), singleUser.getStatus(), 
                     singleUser.getRestaurant() != null ? singleUser.getRestaurant().getId() : "null",
                     singleUser.getRestaurant() != null ? singleUser.getRestaurant().getStatus() : "null");
            
            // Check if user is enabled before authentication
            if (!singleUser.isEnabled()) {
                log.debug("User is not enabled - User status: {}, Restaurant status: {}", 
                         singleUser.getStatus(), 
                         singleUser.getRestaurant() != null ? singleUser.getRestaurant().getStatus() : "null");
                throw new DisabledException("User account is not enabled.");
            }
            
            String authUsername = authenticationRequest.getUsername() + ":" + singleUser.getRestaurant().getId();
            log.debug("Attempting authentication with username: {}", authUsername);
            
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authUsername,
                            authenticationRequest.getPassword()));
            
            final String jwt = jwtUtil.generateToken(singleUser);
            
            // Gestione remember me per utente singolo
            if (authenticationRequest.isRememberMe()) {
                final String refreshToken = jwtUtil.generateRefreshToken(singleUser);
                return AuthResponseDTO.builder()
                        .jwt(jwt)
                        .refreshToken(refreshToken)
                        .user(rUserMapper.toDTO(singleUser))
                        .build();
            } else {
                return new AuthResponseDTO(jwt, rUserMapper.toDTO(singleUser));
            }
        } else {
            // Login intermedio: più ristoranti (HUB)
            // Genera JWT hub normale (sempre 1 ora) con refresh token se remember me
            final String hubJwt = jwtUtil.generateHubToken(user);
            
            if (authenticationRequest.isRememberMe()) {
                final String hubRefreshToken = jwtUtil.generateHubRefreshToken(user);
                return AuthResponseDTO.builder()
                        .jwt(hubJwt)            // Token hub normale da 1 ora
                        .refreshToken(hubRefreshToken)  // Refresh token da 7 giorni
                        .user(user)
                        .build();
            } else {
                return new AuthResponseDTO(hubJwt, user);
            }
        }
    }

    public AuthResponseDTO refreshHubToken(String refreshToken) {
        log.debug("Hub refresh token request received");
        
        try {
            // Verifica che sia un hub refresh token valido
            if (!jwtUtil.isHubRefreshToken(refreshToken)) {
                throw new BadCredentialsException("Invalid refresh token type");
            }
            
            // Estrae l'email dal refresh token
            String email = jwtUtil.extractUsername(refreshToken);
            
            // Trova l'hub user
            final RUserHub hubUser = RUserHubDAO.findByEmail(email);
            if (hubUser == null) {
                log.warn("No hub user found with email from refresh token: {}", email);
                throw new BadCredentialsException("Invalid refresh token");
            }
            
            // Genera nuovi token hub
            final String newHubJwt = jwtUtil.generateHubToken(hubUser);
            final String newHubRefreshToken = jwtUtil.generateHubRefreshToken(hubUser);
            
            log.debug("New hub tokens generated for email: {}", email);
            
            return AuthResponseDTO.builder()
                    .jwt(newHubJwt)
                    .refreshToken(newHubRefreshToken)
                    .user(hubUser)
                    .build();
                    
        } catch (Exception e) {
            log.error("Hub refresh token validation failed: {}", e.getMessage());
            throw new BadCredentialsException("Invalid hub refresh token");
        }
    }

    public AuthResponseDTO refreshRUserToken(String refreshToken) {
        log.debug("RUser refresh token request received");
        
        try {
            // Verifica che sia un refresh token valido
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                throw new BadCredentialsException("Invalid refresh token type");
            }
            
            // Estrae l'username dal refresh token
            String username = jwtUtil.extractUsername(refreshToken);
            
            // Per RUser, il token contiene email:restaurantId
            String[] parts = username.split(":");
            if (parts.length != 2) {
                throw new BadCredentialsException("Invalid RUser token format");
            }
            
            String email = parts[0];
            Long restaurantId = Long.parseLong(parts[1]);
            
            // Trova il RUser
            final RUser rUser = RUserDAO.findByEmailAndRestaurantId(email, restaurantId);
            if (rUser == null) {
                log.warn("No RUser found with email: {} and restaurant: {}", email, restaurantId);
                throw new BadCredentialsException("Invalid refresh token");
            }
            
            // Verifica il refresh token
            if (!jwtUtil.validateToken(refreshToken, rUser)) {
                throw new BadCredentialsException("Invalid or expired refresh token");
            }
            
            // Genera nuovi token
            final String newJwt = jwtUtil.generateToken(rUser);
            final String newRefreshToken = jwtUtil.generateRefreshToken(rUser);
            
            log.debug("New RUser tokens generated for: {}:{}", email, restaurantId);
            
            return AuthResponseDTO.builder()
                    .jwt(newJwt)
                    .refreshToken(newRefreshToken)
                    .user(rUserMapper.toDTO(rUser))
                    .build();
                    
        } catch (Exception e) {
            log.error("RUser refresh token validation failed: {}", e.getMessage());
            throw new BadCredentialsException("Invalid refresh token");
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
			log.error("Google authentication failed: {}", e.getMessage(), e);
			throw new BadCredentialsException("Google authentication failed: " + e.getMessage());
		}
	}

}
