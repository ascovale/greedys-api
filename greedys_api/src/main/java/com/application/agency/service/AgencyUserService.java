package com.application.agency.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.agency.persistence.dao.AgencyDAO;
import com.application.agency.persistence.dao.AgencyUserDAO;
import com.application.agency.persistence.dao.AgencyUserHubDAO;
import com.application.agency.persistence.dao.AgencyUserHubVerificationTokenDAO;
import com.application.agency.persistence.model.Agency;
import com.application.agency.persistence.model.user.AgencyUser;
import com.application.agency.persistence.model.user.AgencyUserHub;
import com.application.agency.persistence.model.user.AgencyUserHubVerificationToken;
import com.application.agency.persistence.model.user.AgencyUserOptions;
import com.application.common.service.EmailService;
import com.application.common.security.jwt.constants.TokenValidationConstants;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class AgencyUserService {

    @Autowired
    private AgencyUserDAO agencyUserDAO;

    @Autowired
    private AgencyUserHubDAO agencyUserHubDAO;

    @Autowired
    private AgencyUserHubVerificationTokenDAO hubTokenDAO;

    @Autowired
    private AgencyDAO agencyDAO;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Agency User Management

    /**
     * Register a new agency user
     */
    public AgencyUser registerAgencyUser(String email, String firstName, String lastName, 
                                       String phoneNumber, Long agencyId, String roleName) {
        
        // Verify agency exists and is active
        Agency agency = agencyDAO.findById(agencyId)
            .orElseThrow(() -> new EntityNotFoundException("Agency not found with id: " + agencyId));
        
        if (!agency.isActive()) {
            throw new IllegalStateException("Cannot add users to inactive agency");
        }

        // Check if user already exists for this agency
        List<AgencyUser> existingUsers = agencyUserDAO.findAgencyUsersByEmail(email);
        boolean userExistsInAgency = existingUsers.stream()
            .anyMatch(user -> user.getAgency().getId().equals(agencyId));
        
        if (userExistsInAgency) {
            throw new IllegalArgumentException("User already exists for this agency");
        }

        // Check if a user hub with this email already exists
        AgencyUserHub existingUserHub = agencyUserHubDAO.findByEmail(email).orElse(null);
        if (existingUserHub == null) {
            // Create new user hub with encoded password - starts with VERIFY_TOKEN
            String tempPassword = generateRandomPassword();
            existingUserHub = AgencyUserHub.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .password(passwordEncoder.encode(tempPassword))
                .status(AgencyUserHub.Status.VERIFY_TOKEN) // Richiede verifica email
                .accepted(true)
                .credentialsExpirationDate(LocalDate.now().plusDays(90))
                .build();

            // Create user options
            AgencyUserOptions options = new AgencyUserOptions();
            existingUserHub.setOptions(options);
            
            existingUserHub = agencyUserHubDAO.save(existingUserHub);

            // Generate and send verification token for new Hub
            String hubToken = UUID.randomUUID().toString();
            createVerificationTokenForHub(existingUserHub, hubToken);
            
            // TODO: Send hub verification email instead of welcome email
            // sendHubVerificationEmail(existingUserHub, hubToken, agency);
        }

        // Create agency user - inherit status from Hub
        AgencyUser.Status agencyUserStatus = mapHubStatusToUserStatus(existingUserHub.getStatus());
        
        AgencyUser agencyUser = new AgencyUser();
        agencyUser.setEmail(email);
        agencyUser.setName(firstName);
        agencyUser.setSurname(lastName);
        agencyUser.setPhoneNumber(phoneNumber);
        agencyUser.setAgency(agency);
        agencyUser.setStatus(agencyUserStatus); // Inherit status from Hub

        return agencyUserDAO.save(agencyUser);
    }

    /**
     * Find agency user by email and agency ID
     */
    public AgencyUser findByEmailAndAgencyId(String email, Long agencyId) {
        List<AgencyUser> users = agencyUserDAO.findAgencyUsersByEmail(email);
        return users.stream()
            .filter(user -> user.getAgency().getId().equals(agencyId))
            .findFirst()
            .orElse(null);
    }

    /**
     * Find agency user by ID
     */
    public AgencyUser findAgencyUserById(Long userId) {
        return agencyUserDAO.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Agency user not found with id: " + userId));
    }

    /**
     * Get all users for an agency
     */
    public List<AgencyUser> getAgencyUsers(Long agencyId) {
        return agencyUserDAO.findByAgencyId(agencyId);
    }

    /**
     * Get enabled users for an agency
     */
    public List<AgencyUser> getEnabledAgencyUsers(Long agencyId) {
        return agencyUserDAO.findByAgencyIdAndStatus(agencyId, AgencyUser.Status.ENABLED);
    }

    /**
     * Get paginated users for an agency
     */
    public Page<AgencyUser> getAgencyUsersPage(Long agencyId, int page, int size, String sortBy) {
        Sort sort = Sort.by(Sort.Direction.ASC, sortBy != null ? sortBy : "firstName");
        Pageable pageable = PageRequest.of(page, size, sort);
        return agencyUserDAO.findByAgencyId(agencyId, pageable);
    }

    /**
     * Search users within an agency
     */
    public List<AgencyUser> searchAgencyUsers(Long agencyId, String searchTerm) {
        // Use pagination with large size to get all results for now
        return agencyUserDAO.searchAgencyUsers(agencyId, searchTerm, Pageable.unpaged()).getContent();
    }

    /**
     * Update agency user
     */
    public AgencyUser updateAgencyUser(Long userId, String firstName, String lastName, 
                                     String phoneNumber, AgencyUser.Status status) {
        AgencyUser user = findAgencyUserById(userId);

        if (firstName != null && !firstName.trim().isEmpty()) {
            user.setName(firstName);
        }

        if (lastName != null && !lastName.trim().isEmpty()) {
            user.setSurname(lastName);
        }

        if (phoneNumber != null) {
            user.setPhoneNumber(phoneNumber);
        }

        if (status != null) {
            user.setStatus(status);
        }

        return agencyUserDAO.save(user);
    }

    /**
     * Enable agency user
     */
    public AgencyUser enableAgencyUser(Long userId) {
        AgencyUser user = findAgencyUserById(userId);
        user.setStatus(AgencyUser.Status.ENABLED);
        return agencyUserDAO.save(user);
    }

    /**
     * Disable agency user
     */
    public AgencyUser disableAgencyUser(Long userId) {
        AgencyUser user = findAgencyUserById(userId);
        user.setStatus(AgencyUser.Status.DISABLED);
        return agencyUserDAO.save(user);
    }

    /**
     * Delete agency user (soft delete)
     */
    public void deleteAgencyUser(Long userId) {
        AgencyUser user = findAgencyUserById(userId);
        user.setStatus(AgencyUser.Status.DISABLED);
        agencyUserDAO.save(user);
    }

    /**
     * Change user password
     */
    public void changeAgencyUserPassword(Long userId, String newPassword) {
        AgencyUser user = findAgencyUserById(userId);
        // For AgencyUser, the password would be in the AgencyUserHub
        // Since AgencyUser doesn't have direct access, we look up by email
        Optional<AgencyUserHub> userHub = agencyUserHubDAO.findByEmail(user.getEmail());
        if (userHub.isPresent()) {
            userHub.get().setPassword(passwordEncoder.encode(newPassword));
            agencyUserHubDAO.save(userHub.get());
        }
    }

    /**
     * Get user statistics for an agency
     */
    public AgencyUserStats getAgencyUserStatistics(Long agencyId) {
        Long totalUsers = agencyUserDAO.countByAgencyId(agencyId);
        Long enabledUsers = agencyUserDAO.countActiveByAgencyId(agencyId);
        Long disabledUsers = totalUsers - enabledUsers; // Approximation

        return new AgencyUserStats(totalUsers, enabledUsers, disabledUsers);
    }

    // User Hub Management

    /**
     * Find agency user hub by email
     */
    public Optional<AgencyUserHub> findAgencyUserHubByEmail(String email) {
        return agencyUserHubDAO.findByEmail(email);
    }

    /**
     * Get all agencies for a user
     */
    public List<Agency> getUserAgencies(String userEmail) {
        return agencyDAO.findAgenciesByUserEmail(userEmail);
    }

    /**
     * Check if user has access to agency
     */
    public boolean hasUserAccessToAgency(String userEmail, Long agencyId) {
        List<AgencyUser> users = agencyUserDAO.findAgencyUsersByEmail(userEmail);
        return users.stream()
            .anyMatch(user -> user.getAgency().getId().equals(agencyId) && 
                             user.getStatus() == AgencyUser.Status.ENABLED);
    }

    // Private helper methods

    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private void sendWelcomeEmail(AgencyUserHub userHub, String tempPassword, Agency agency) {
        String subject = "Welcome to Greedys - Agency User Account Created";
        String body = String.format(
            "Dear %s %s,\n\n" +
            "Your user account for agency '%s' has been created!\n\n" +
            "Email: %s\n" +
            "Temporary Password: %s\n\n" +
            "Please log in and change your password as soon as possible.\n\n" +
            "Best regards,\n" +
            "The Greedys Team",
            userHub.getFirstName(),
            userHub.getLastName(),
            agency.getName(),
            userHub.getEmail(),
            tempPassword
        );

        emailService.sendEmail(userHub.getEmail(), subject, body);
    }

    // Statistics class
    public static class AgencyUserStats {
        private final Long totalUsers;
        private final Long enabledUsers;
        private final Long disabledUsers;

        public AgencyUserStats(Long totalUsers, Long enabledUsers, Long disabledUsers) {
            this.totalUsers = totalUsers;
            this.enabledUsers = enabledUsers;
            this.disabledUsers = disabledUsers;
        }

        public Long getTotalUsers() { return totalUsers; }
        public Long getEnabledUsers() { return enabledUsers; }
        public Long getDisabledUsers() { return disabledUsers; }
    }

    // ========= HUB VERIFICATION METHODS =========

    /**
     * Create verification token for AgencyUserHub
     */
    public void createVerificationTokenForHub(final AgencyUserHub userHub, final String token) {
        final AgencyUserHubVerificationToken hubToken = new AgencyUserHubVerificationToken(token, userHub);
        hubTokenDAO.save(hubToken);
    }

    /**
     * Validate hub verification token
     */
    public String validateHubVerificationToken(String token) {
        final AgencyUserHubVerificationToken verificationToken = hubTokenDAO.findByToken(token);
        if (verificationToken == null) {
            return TokenValidationConstants.TOKEN_INVALID;
        }

        final AgencyUserHub userHub = verificationToken.getAgencyUserHub();
        final java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (verificationToken.getExpiryDate().isBefore(now)) {
            hubTokenDAO.delete(verificationToken);
            return TokenValidationConstants.TOKEN_EXPIRED;
        }
        if (userHub.getStatus() != AgencyUserHub.Status.VERIFY_TOKEN) {
            return TokenValidationConstants.TOKEN_INVALID;
        }
        
        // Update Hub status to ENABLED
        userHub.setStatus(AgencyUserHub.Status.ENABLED);
        agencyUserHubDAO.save(userHub);
        
        // Update all AgencyUsers associated with this Hub
        updateAllAgencyUserStatusByHub(userHub);
        
        // Delete used token
        hubTokenDAO.delete(verificationToken);
        return TokenValidationConstants.TOKEN_VALID;
    }

    /**
     * Map Hub status to AgencyUser status
     */
    private AgencyUser.Status mapHubStatusToUserStatus(AgencyUserHub.Status hubStatus) {
        switch (hubStatus) {
            case VERIFY_TOKEN:
                return AgencyUser.Status.VERIFY_TOKEN;
            case ENABLED:
                return AgencyUser.Status.ENABLED;
            case BLOCKED:
                return AgencyUser.Status.BLOCKED;
            case DELETED:
                return AgencyUser.Status.DELETED;
            case DISABLED:
                return AgencyUser.Status.DISABLED;
            default:
                return AgencyUser.Status.VERIFY_TOKEN;
        }
    }

    /**
     * Update status of all AgencyUsers associated with a Hub
     */
    private void updateAllAgencyUserStatusByHub(AgencyUserHub userHub) {
        List<AgencyUser> associatedUsers = agencyUserDAO.findAgencyUsersByEmail(userHub.getEmail());
        AgencyUser.Status newStatus = mapHubStatusToUserStatus(userHub.getStatus());
        
        for (AgencyUser user : associatedUsers) {
            user.setStatus(newStatus);
            agencyUserDAO.save(user);
        }
    }
}