package com.application.agency.service;

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
import com.application.agency.persistence.dao.AgencyUserHubDAO;
import com.application.agency.persistence.model.Agency;
import com.application.agency.persistence.model.user.AgencyUserHub;
import com.application.common.service.EmailService;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class AgencyService {

    @Autowired
    private AgencyDAO agencyDAO;

    @Autowired
    private AgencyUserHubDAO agencyUserHubDAO;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Agency CRUD Operations

    /**
     * Create a new agency with admin user
     */
    public Agency createAgency(String agencyName, String agencyDescription, 
                              String adminEmail, String adminFirstName, String adminLastName,
                              String adminPhoneNumber) {
        
        // Check if agency name already exists
        if (agencyDAO.existsByName(agencyName)) {
            throw new IllegalArgumentException("Agency with name '" + agencyName + "' already exists");
        }

        // Check if admin email already exists
        if (agencyUserHubDAO.existsByEmail(adminEmail)) {
            throw new IllegalArgumentException("User with email '" + adminEmail + "' already exists");
        }

        // Create agency
        Agency agency = new Agency();
        agency.setName(agencyName);
        agency.setDescription(agencyDescription);
        agency.setStatus(Agency.Status.ACTIVE);
        
        Agency savedAgency = agencyDAO.save(agency);

        // Create admin user hub
        AgencyUserHub adminHub = new AgencyUserHub();
        adminHub.setEmail(adminEmail);
        adminHub.setFirstName(adminFirstName);
        adminHub.setLastName(adminLastName);
        adminHub.setPhoneNumber(adminPhoneNumber);
        adminHub.setStatus(AgencyUserHub.Status.ENABLED);
        adminHub.setAccepted(true);

        // Generate temporary password
        String tempPassword = generateRandomPassword();
        adminHub.setPassword(passwordEncoder.encode(tempPassword));

        agencyUserHubDAO.save(adminHub);

        // Send welcome email with credentials
        try {
            sendWelcomeEmail(adminHub, tempPassword, savedAgency);
        } catch (Exception e) {
            // Log error but don't fail agency creation
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }

        return savedAgency;
    }

    /**
     * Find agency by ID
     */
    public Agency findAgencyById(Long agencyId) {
        return agencyDAO.findById(agencyId)
            .orElseThrow(() -> new EntityNotFoundException("Agency not found with id: " + agencyId));
    }

    /**
     * Find agency by name
     */
    public Optional<Agency> findAgencyByName(String name) {
        return agencyDAO.findByName(name);
    }

    /**
     * Get all agencies
     */
    public List<Agency> getAllAgencies() {
        return agencyDAO.findAll();
    }

    /**
     * Get agencies by status
     */
    public List<Agency> getAgenciesByStatus(Agency.Status status) {
        return agencyDAO.findByStatus(status);
    }

    /**
     * Get active agencies
     */
    public List<Agency> getActiveAgencies() {
        return agencyDAO.findByStatus(Agency.Status.ACTIVE);
    }

    /**
     * Search agencies by name
     */
    public List<Agency> searchAgencies(String searchTerm) {
        return agencyDAO.searchAgencies(searchTerm, null).getContent();
    }

    /**
     * Get paginated agencies
     */
    public Page<Agency> getAgenciesPage(int page, int size, String sortBy) {
        Sort sort = Sort.by(Sort.Direction.ASC, sortBy != null ? sortBy : "name");
        Pageable pageable = PageRequest.of(page, size, sort);
        return agencyDAO.findAll(pageable);
    }

    /**
     * Update agency
     */
    public Agency updateAgency(Long agencyId, String name, String description, Agency.Status status) {
        Agency agency = findAgencyById(agencyId);

        if (name != null && !name.trim().isEmpty()) {
            // Check if new name conflicts with existing agency
            Optional<Agency> existingAgency = agencyDAO.findByName(name);
            if (existingAgency.isPresent() && !existingAgency.get().getId().equals(agencyId)) {
                throw new IllegalArgumentException("Agency with name '" + name + "' already exists");
            }
            agency.setName(name);
        }

        if (description != null) {
            agency.setDescription(description);
        }

        if (status != null) {
            agency.setStatus(status);
        }

        return agencyDAO.save(agency);
    }

    /**
     * Activate agency
     */
    public Agency activateAgency(Long agencyId) {
        Agency agency = findAgencyById(agencyId);
        agency.setStatus(Agency.Status.ACTIVE);
        return agencyDAO.save(agency);
    }

    /**
     * Suspend agency
     */
    public Agency suspendAgency(Long agencyId) {
        Agency agency = findAgencyById(agencyId);
        agency.setStatus(Agency.Status.SUSPENDED);
        return agencyDAO.save(agency);
    }

    /**
     * Delete agency (soft delete by marking as inactive)
     */
    public void deleteAgency(Long agencyId) {
        Agency agency = findAgencyById(agencyId);
        agency.setStatus(Agency.Status.INACTIVE);
        agencyDAO.save(agency);
    }

    // Statistics

    /**
     * Get agency statistics
     */
    public AgencyStats getAgencyStatistics(Long agencyId) {
        Agency agency = findAgencyById(agencyId);
        
        Long totalUsers = agencyUserHubDAO.countByStatus(AgencyUserHub.Status.ENABLED);
        Long pendingUsers = 0L; // No PENDING status in AgencyUserHub
        Long disabledUsers = agencyUserHubDAO.countByStatus(AgencyUserHub.Status.DISABLED);
        
        return new AgencyStats(agency, totalUsers, pendingUsers, disabledUsers);
    }

    // Private helper methods

    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private void sendWelcomeEmail(AgencyUserHub adminHub, String tempPassword, Agency agency) {
        String subject = "Welcome to Greedys - Agency Account Created";
        String body = String.format(
            "Dear %s %s,\n\n" +
            "Your agency account has been successfully created!\n\n" +
            "Agency: %s\n" +
            "Email: %s\n" +
            "Temporary Password: %s\n\n" +
            "Please log in and change your password as soon as possible.\n\n" +
            "Best regards,\n" +
            "The Greedys Team",
            adminHub.getFirstName(),
            adminHub.getLastName(),
            agency.getName(),
            adminHub.getEmail(),
            tempPassword
        );

        emailService.sendEmail(adminHub.getEmail(), subject, body);
    }

    // Inner class for statistics
    public static class AgencyStats {
        private final Agency agency;
        private final Long totalUsers;
        private final Long pendingUsers;
        private final Long disabledUsers;

        public AgencyStats(Agency agency, Long totalUsers, Long pendingUsers, Long disabledUsers) {
            this.agency = agency;
            this.totalUsers = totalUsers;
            this.pendingUsers = pendingUsers;
            this.disabledUsers = disabledUsers;
        }

        // Getters
        public Agency getAgency() { return agency; }
        public Long getTotalUsers() { return totalUsers; }
        public Long getPendingUsers() { return pendingUsers; }
        public Long getDisabledUsers() { return disabledUsers; }
    }
}