package com.application.admin.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.AdminOnRegistrationCompleteEvent;
import com.application.admin.persistence.dao.AdminDAO;
import com.application.admin.persistence.dao.AdminRoleDAO;
import com.application.admin.persistence.dao.AdminVerificationTokenDAO;
import com.application.admin.persistence.model.Admin;
import com.application.admin.persistence.model.AdminPrivilege;
import com.application.admin.persistence.model.AdminRole;
import com.application.admin.persistence.model.AdminVerificationToken;
import com.application.admin.web.dto.admin.AdminDTO;
import com.application.admin.web.dto.admin.NewAdminDTO;
import com.application.common.persistence.mapper.AdminMapper;
import com.application.common.security.jwt.constants.TokenValidationConstants;
import com.application.common.service.EmailService;
import com.application.common.web.error.UserAlreadyExistException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for handling admin registration operations including
 * account creation, email verification, and token management.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdminRegistrationService {

    private final AdminService adminService;
    private final MessageSource messageSource;
    private final EmailService emailService;
    private final AdminDAO adminDAO;
    private final AdminVerificationTokenDAO tokenDAO;
    private final AdminRoleDAO adminRoleDAO;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final AdminMapper adminMapper;

    /**
     * Constructs the application URL from the HTTP request
     * 
     * @param request The HTTP servlet request
     * @return The complete application URL
     */
    private String getAppUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    }

    /**
     * Registers a new admin account and sends verification email
     * 
     * @param accountDto The admin account data
     * @param request The HTTP servlet request to extract app URL
     * @return The created Admin entity
     * @throws Exception if registration fails
     */
    public AdminDTO registerNewAdmin(NewAdminDTO accountDto, HttpServletRequest request) {
        log.info("Registering new admin with email: {}", accountDto.getEmail());
        
        if (emailExists(accountDto.getEmail())) {
            throw new UserAlreadyExistException("There is an account with that email adress: " + accountDto.getEmail());
        }
        if (accountDto.getPassword() == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        
        final Admin admin = Admin.builder()
            .name(accountDto.getFirstName())
            .surname(accountDto.getLastName())
            .password(passwordEncoder.encode(accountDto.getPassword()))
            .email(accountDto.getEmail())
            .build();

        AdminRole adminRole = adminRoleDAO.findByName("ROLE_SUPER_ADMIN");
        admin.addAdminRole(adminRole);
        Admin savedAdmin = adminDAO.save(admin);
        
        // Pubblica l'evento per l'invio dell'email di conferma
        String appUrl = getAppUrl(request);
        eventPublisher.publishEvent(new AdminOnRegistrationCompleteEvent(savedAdmin, Locale.ITALIAN, appUrl));
        
        log.info("Admin registered successfully with ID: {}", savedAdmin.getId());
        return adminMapper.toDTO(savedAdmin);
    }

    /**
     * Check if an admin with the given email already exists
     * 
     * @param email The email to check
     * @return true if email exists, false otherwise
     */
    private boolean emailExists(String email) {
        return adminService.findAdminByEmail(email) != null;
    }

    /**
     * Validates a verification token and activates the admin account
     * 
     * @param token The verification token
     * @param locale The user's locale for messages
     * @return Result of validation ("valid", "expired", "invalidToken")
     */
    public String confirmRegistration(String token, Locale locale) {
        log.info("Confirming registration with token: {}", token);
        
        String result = validateVerificationToken(token);
        
        if (TokenValidationConstants.TOKEN_VALID.equals(result)) {
            log.info("Token validated successfully: {}", token);
        } else {
            log.warn("Token validation failed with result: {} for token: {}", result, token);
        }
        
        return result;
    }

    /**
     * Confirms registration and automatically authenticates the admin
     * 
     * @param token The verification token
     * @param locale The user's locale for messages
     * @return String with result message
     */
    public String confirmRegistrationAndAuthenticate(String token, Locale locale) {
        log.info("Confirming registration and authenticating with token: {}", token);
        
        String result = validateVerificationToken(token);
        
        if (TokenValidationConstants.TOKEN_VALID.equals(result)) {
            Admin admin = getAdminByToken(token);
            if (admin != null) {
                authenticateAdminWithoutPassword(admin);
            }
            String message = getValidationMessage(result, locale);
            log.info("Registration confirmed and admin authenticated successfully");
            return message + " - Authentication successful";
        }

        String message = getValidationMessage(result, locale);
        boolean expired = isTokenExpired(result);
        
        if (expired) {
            return message + " - Token expired";
        } else {
            return message + " - Invalid token";
        }
    }

    /**
     * Validates a verification token
     * 
     * @param token The verification token
     * @return Result of validation
     */
    private String validateVerificationToken(String token) {
        final AdminVerificationToken verificationToken = tokenDAO.findByToken(token);
        if (verificationToken == null) {
            return TokenValidationConstants.TOKEN_INVALID;
        }

        final Admin user = verificationToken.getAdmin();
        final LocalDateTime now = LocalDateTime.now();
        if (verificationToken.getExpiryDate().isBefore(now)) {
            tokenDAO.delete(verificationToken);
            return TokenValidationConstants.TOKEN_EXPIRED;
        }
        if (user.getStatus() != Admin.Status.VERIFY_TOKEN) {
            return TokenValidationConstants.TOKEN_INVALID;
        }

        user.setStatus(Admin.Status.ENABLED);
        tokenDAO.delete(verificationToken);
        adminDAO.save(user);
        return TokenValidationConstants.TOKEN_VALID;
    }

    /**
     * Gets the admin associated with a verification token
     * 
     * @param token The verification token
     * @return The Admin entity
     */
    public Admin getAdminByToken(String token) {
        AdminVerificationToken verificationToken = tokenDAO.findByToken(token);
        if (verificationToken != null) {
            return verificationToken.getAdmin();
        }
        return null;
    }

    /**
     * Resends verification token email to admin
     * 
     * @param existingToken The existing verification token
     * @param request The HTTP servlet request to extract app URL
     * @param locale The user's locale for messages
     * @return String with success message
     */
    public String resendVerificationToken(String existingToken, HttpServletRequest request, Locale locale) {
        log.info("Resending verification token for existing token: {}", existingToken);
        
        try {
            AdminVerificationToken newToken = generateNewVerificationToken(existingToken);
            Admin admin = getAdminByToken(newToken.getToken());
            
            String appUrl = getAppUrl(request);
            SimpleMailMessage email = constructResendVerificationTokenEmail(appUrl, locale, newToken, admin);
            emailService.sendEmail(email);
            
            String message = messageSource.getMessage("message.resendToken", null, locale);
            log.info("Verification token resent successfully for admin: {}", admin.getEmail());
            
            return message;
        } catch (Exception e) {
            log.error("Failed to resend verification token for token: {}", existingToken, e);
            throw e;
        }
    }

    /**
     * Generates a new verification token for an existing token
     * 
     * @param existingVerificationToken The existing token
     * @return The new verification token
     */
    private AdminVerificationToken generateNewVerificationToken(String existingVerificationToken) {
        AdminVerificationToken vToken = tokenDAO.findByToken(existingVerificationToken);
        vToken.updateToken(UUID.randomUUID().toString());
        vToken = tokenDAO.save(vToken);
        return vToken;
    }

    /**
     * Gets a localized message for the validation result
     * 
     * @param result The validation result
     * @param locale The user's locale
     * @return The localized message
     */
    public String getValidationMessage(String result, Locale locale) {
        if (TokenValidationConstants.TOKEN_VALID.equals(result)) {
            return messageSource.getMessage("message.accountVerified", null, locale);
        } else {
            return messageSource.getMessage("auth.message." + result, null, locale);
        }
    }

    /**
     * Checks if a validation result indicates an expired token
     * 
     * @param result The validation result
     * @return true if token is expired
     */
    public boolean isTokenExpired(String result) {
        return "expired".equals(result);
    }

    /**
     * Constructs email for resending verification token
     * 
     * @param contextPath The application context path
     * @param locale The user's locale
     * @param newToken The new verification token
     * @param admin The admin user
     * @return SimpleMailMessage ready to send
     */
    private SimpleMailMessage constructResendVerificationTokenEmail(String contextPath, Locale locale, 
                                                                   AdminVerificationToken newToken, Admin admin) {
        String confirmationUrl = contextPath + "/registrationConfirm.html?token=" + newToken.getToken();
        String message = messageSource.getMessage("message.resendToken", null, locale);
        return constructEmail("Resend Registration Token", message + " \r\n" + confirmationUrl, admin);
    }

    /**
     * Constructs a simple mail message
     * 
     * @param subject Email subject
     * @param body Email body
     * @param admin The admin recipient
     * @return SimpleMailMessage ready to send
     */
    private SimpleMailMessage constructEmail(String subject, String body, Admin admin) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setSubject(subject);
        email.setText(body);
        email.setTo(admin.getEmail());
        email.setFrom("reservation@greedys.it");
        return email;
    }

    /**
     * Authenticates an admin without password by setting up the security context
     * 
     * @param admin The admin to authenticate
     */
    private void authenticateAdminWithoutPassword(Admin admin) {
        List<AdminPrivilege> privileges = admin.getRoles().stream()
            .map(adminRole -> adminRole.getPrivileges())
            .flatMap(Collection::stream)
            .distinct()
            .collect(Collectors.toList());
        
        List<GrantedAuthority> authorities = privileges.stream()
            .map(p -> new SimpleGrantedAuthority(p.getName()))
            .collect(Collectors.toList());

        Authentication authentication = new UsernamePasswordAuthenticationToken(admin, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        log.info("Admin {} authenticated successfully without password", admin.getEmail());
    }
}
