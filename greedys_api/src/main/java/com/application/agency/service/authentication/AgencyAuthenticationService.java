package com.application.agency.service.authentication;

import java.util.List;
import java.util.Locale;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import com.application.agency.persistence.dao.AgencyUserDAO;
import com.application.agency.persistence.dao.AgencyUserHubDAO;
import com.application.agency.persistence.model.user.AgencyUser;
import com.application.agency.persistence.model.user.AgencyUserHub;
import com.application.agency.service.AgencyUserService;
import com.application.common.persistence.mapper.AgencyUserHubMapper;
import com.application.common.persistence.mapper.AgencyUserMapper;
import com.application.common.security.jwt.JwtUtil;
import com.application.common.security.jwt.constants.TokenValidationConstants;
import com.application.common.web.dto.security.AuthResponseDTO;
import com.application.common.web.dto.security.AuthRequestDTO;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AgencyAuthenticationService {

    private final AgencyUserService agencyUserService;
    private final AgencyUserHubDAO agencyUserHubDAO;
    private final AgencyUserDAO agencyUserDAO;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AgencyUserHubMapper agencyUserHubMapper;
    private final AgencyUserMapper agencyUserMapper;

    /**
     * Login per Agency User Hub con supporto per più agenzie
     * Pattern identico a RestaurantAuthenticationService.loginWithHubSupport()
     */
    public AuthResponseDTO loginWithHubSupport(AuthRequestDTO authenticationRequest) {
        // Trova l'AgencyUserHub per email
        AgencyUserHub userHub = agencyUserHubDAO.findByEmail(authenticationRequest.getUsername())
                .orElse(null);
        
        if (userHub == null) {
            log.warn("Agency hub user not found: {}", authenticationRequest.getUsername());
            throw new BadCredentialsException("Invalid username or password.");
        }

        // Verifica la password
        if (!passwordEncoder.matches(authenticationRequest.getPassword(), userHub.getPassword())) {
            log.warn("Invalid password for agency hub user: {}", authenticationRequest.getUsername());
            throw new BadCredentialsException("Invalid username or password.");
        }
        
        log.debug("Password verified successfully for agency hub user: {}", authenticationRequest.getUsername());

        // Recupera tutti gli AgencyUser associati a questo hub
        List<AgencyUser> associatedUsers = agencyUserDAO.findAgencyUsersByEmail(userHub.getEmail());

        log.debug("Found {} associated agencies for hub ID: {}", 
                 associatedUsers != null ? associatedUsers.size() : 0, userHub.getId());

        if (associatedUsers == null || associatedUsers.isEmpty()) {
            log.debug("No agencies associated with user: {}", authenticationRequest.getUsername());
            throw new BadCredentialsException("No agencies associated with this user.");
        }
        
        if (associatedUsers.size() == 1) {
            // Login classico: una sola agenzia
            AgencyUser singleUser = associatedUsers.get(0);
            
            log.debug("Single agency user found - ID: {}, Status: {}, Agency ID: {}", 
                     singleUser.getId(), singleUser.getStatus(), 
                     singleUser.getAgency() != null ? singleUser.getAgency().getId() : "null");
            
            // Verifica se l'utente è abilitato
            if (singleUser.getStatus() != AgencyUser.Status.ENABLED) {
                log.debug("Agency user is not enabled - Status: {}", singleUser.getStatus());
                throw new DisabledException("User account is not enabled.");
            }
            
            // Genera JWT con formato email:agencyId (come RUser)
            final String jwt = jwtUtil.generateToken(singleUser);
            
            // Gestione remember me
            if (authenticationRequest.isRememberMe()) {
                final String refreshToken = jwtUtil.generateRefreshToken(singleUser);
                return AuthResponseDTO.builder()
                        .jwt(jwt)
                        .refreshToken(refreshToken)
                        .user(agencyUserMapper.toDTO(singleUser))
                        .build();
            } else {
                return new AuthResponseDTO(jwt, agencyUserMapper.toDTO(singleUser));
            }
        } else {
            // Login intermedio: più agenzie (HUB)
            // Genera JWT hub con refresh token se remember me
            final String hubJwt = jwtUtil.generateAgencyHubToken(userHub);
            
            log.debug("Hub login with {} agencies - Hub JWT generated", associatedUsers.size());
            
            if (authenticationRequest.isRememberMe()) {
                final String hubRefreshToken = jwtUtil.generateAgencyHubRefreshToken(userHub);
                return AuthResponseDTO.builder()
                        .jwt(hubJwt)
                        .refreshToken(hubRefreshToken)
                        .user(agencyUserHubMapper.toDTO(userHub))
                        .build();
            } else {
                return AuthResponseDTO.builder()
                        .jwt(hubJwt)
                        .user(agencyUserHubMapper.toDTO(userHub))
                        .build();
            }
        }
    }

    /**
     * Refresh token per Agency Hub
     * Pattern identico a RestaurantAuthenticationService.refreshHubToken()
     */
    public AuthResponseDTO refreshHubToken(String refreshToken) {
        log.debug("Agency hub refresh token request received");
        
        try {
            // Verifica che sia un hub refresh token valido
            if (!jwtUtil.isAgencyHubRefreshToken(refreshToken)) {
                throw new BadCredentialsException("Invalid refresh token type");
            }
            
            // Estrae l'email dal refresh token
            String email = jwtUtil.extractUsername(refreshToken);
            
            // Trova l'hub user
            final AgencyUserHub hubUser = agencyUserHubDAO.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
            
            // Genera nuovi token hub
            final String newHubJwt = jwtUtil.generateAgencyHubToken(hubUser);
            final String newHubRefreshToken = jwtUtil.generateAgencyHubRefreshToken(hubUser);
            
            log.debug("New agency hub tokens generated for email: {}", email);
            
            return AuthResponseDTO.builder()
                    .jwt(newHubJwt)
                    .refreshToken(newHubRefreshToken)
                    .user(agencyUserHubMapper.toDTO(hubUser))
                    .build();
                    
        } catch (Exception e) {
            log.error("Agency hub refresh token validation failed: {}", e.getMessage());
            throw new BadCredentialsException("Invalid hub refresh token");
        }
    }

    /**
     * Refresh token per Agency User (singola agenzia)
     * Pattern identico a RestaurantAuthenticationService.refreshRUserToken()
     * Token format: "email:agencyId"
     */
    public AuthResponseDTO refreshAgencyUserToken(String refreshToken) {
        log.debug("Agency user refresh token request received");
        
        try {
            // Verifica che sia un refresh token valido
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                throw new BadCredentialsException("Invalid refresh token type");
            }
            
            // Estrae l'username dal refresh token
            String username = jwtUtil.extractUsername(refreshToken);
            
            // Per AgencyUser, il token contiene email:agencyId
            String[] parts = username.split(":");
            if (parts.length != 2) {
                log.warn("Invalid agency user token format: {}", username);
                throw new BadCredentialsException("Invalid agency user token format");
            }
            
            String email = parts[0];
            Long agencyId = Long.parseLong(parts[1]);
            
            // Trova l'AgencyUser per email + agencyId
            AgencyUser agencyUser = agencyUserDAO.findByEmailAndAgencyId(email, agencyId)
                    .orElseThrow(() -> {
                        log.warn("No agency user found with email: {} and agency: {}", email, agencyId);
                        return new BadCredentialsException("Invalid refresh token");
                    });
            
            // Valida il refresh token
            if (!jwtUtil.validateToken(refreshToken, agencyUser)) {
                throw new BadCredentialsException("Invalid or expired refresh token");
            }
            
            // Genera nuovi token
            final String newJwt = jwtUtil.generateToken(agencyUser);
            final String newRefreshToken = jwtUtil.generateRefreshToken(agencyUser);
            
            log.debug("New agency user tokens generated for: {}:{}", email, agencyId);
            
            return AuthResponseDTO.builder()
                    .jwt(newJwt)
                    .refreshToken(newRefreshToken)
                    .user(agencyUserMapper.toDTO(agencyUser))
                    .build();
                    
        } catch (Exception e) {
            log.error("Agency user refresh token validation failed: {}", e.getMessage());
            throw new BadCredentialsException("Invalid refresh token");
        }
    }

    /**
     * Seleziona un'agenzia per l'AgencyUserHub con multi-agenzia
     * Genera nuovo JWT specifico per quell'agenzia
     * Pattern identico a RestaurantAuthenticationService.selectRestaurant()
     */
    public AuthResponseDTO selectAgency(Long agencyId) {
        String email = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        
        log.debug("Selecting agency {} for user: {}", agencyId, email);
        
        // Trova l'AgencyUser per email + agencyId
        AgencyUser agencyUser = agencyUserDAO.findByEmailAndAgencyId(email, agencyId)
                .orElseThrow(() -> {
                    log.warn("User does not have access to agency: {}", agencyId);
                    return new BadCredentialsException("User does not have access to this agency.");
                });
        
        // Verifica se l'utente è abilitato
        if (agencyUser.getStatus() != AgencyUser.Status.ENABLED) {
            log.warn("Agency user is not enabled for agency: {}", agencyId);
            throw new DisabledException("User is not enabled for this agency.");
        }
        
        // Genera nuovo JWT per quella agenzia specifica
        String jwt = jwtUtil.generateToken(agencyUser);
        
        log.debug("New JWT generated for agency: {}", agencyId);
        
        return new AuthResponseDTO(jwt, agencyUserMapper.toDTO(agencyUser));
    }

    /**
     * Conferma la registrazione di un AgencyUserHub tramite token di verifica
     */
    public String confirmAgencyUserHubRegistration(final HttpServletRequest request, final Model model,
            final String token) {
        Locale locale = request.getLocale();
        final String result = agencyUserService.validateHubVerificationToken(token);

        if (TokenValidationConstants.TOKEN_VALID.equals(result)) {
            // Hub verified successfully - all associated AgencyUsers are now enabled
            if (model != null) {
                model.addAttribute("message", "Agency hub account verified successfully!");
            }
            log.info("Agency hub verification successful for token: {}", token);
            return "redirect:/console.html?lang=" + locale.getLanguage();
        }

        String errorMessage = getErrorMessage(result);
        if (model != null) {
            model.addAttribute("message", errorMessage);
            model.addAttribute("expired", TokenValidationConstants.TOKEN_EXPIRED.equals(result));
            model.addAttribute("token", token);
        }
        log.warn("Agency hub verification failed for token: {} - Result: {}", token, result);
        return "redirect:/public/badUser.html?lang=" + locale.getLanguage();
    }

    private String getErrorMessage(String result) {
        switch (result) {
            case TokenValidationConstants.TOKEN_EXPIRED:
                return "Verification token has expired. Please request a new one.";
            case TokenValidationConstants.TOKEN_INVALID:
                return "Invalid verification token.";
            default:
                return "Verification failed. Please try again.";
        }
    }
}