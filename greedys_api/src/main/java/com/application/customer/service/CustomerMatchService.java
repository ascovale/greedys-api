package com.application.customer.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.application.common.service.FuzzyNameMatcher;
import com.application.common.service.PhoneNormalizer;
import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.web.dto.matching.CustomerCandidateDTO;
import com.application.customer.web.dto.matching.CustomerMatchDecisionDTO;
import com.application.customer.web.dto.matching.CustomerMatchInput;
import com.application.customer.web.dto.matching.CustomerMatchResponse;
import com.application.customer.persistence.model.Customer;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for intelligent customer matching using multiple strategies
 */
@Service
@Slf4j
public class CustomerMatchService {

    @Autowired
    private CustomerDAO customerDAO;
    
    @Autowired
    private PhoneNormalizer phoneNormalizer;
    
    @Autowired
    private FuzzyNameMatcher fuzzyNameMatcher;

    // Confidence thresholds for different match types
    private static final double PHONE_EXACT_CONFIDENCE = 1.0;
    private static final double EMAIL_EXACT_CONFIDENCE = 0.95;
    private static final double NAME_PHONE_PARTIAL_CONFIDENCE = 0.85;
    private static final double FULL_NAME_FUZZY_CONFIDENCE = 0.75;
    private static final double NAME_SIMILARITY_THRESHOLD = 0.8;
    private static final double PHONE_PARTIAL_SIMILARITY = 0.7;

    /**
     * Find matching customers based on provided data
     * 
     * @param input Customer data to match against
     * @return Response with candidates and matching decision
     */
    public CustomerMatchResponse findMatches(CustomerMatchInput input) {
        log.info("Finding customer matches for input: phone={}, email={}, name='{}'", 
                input.getPhone(), input.getEmail(), input.getComputedFullName());

        List<CustomerCandidateDTO> candidates = new ArrayList<>();

        try {
            // 1. Phone-based matching (highest priority)
            if (input.hasPhone()) {
                candidates.addAll(findPhoneMatches(input));
            }

            // 2. Email-based matching
            if (input.hasEmail()) {
                candidates.addAll(findEmailMatches(input));
            }

            // 3. Name-based matching (if we don't have strong matches)
            if (candidates.isEmpty() || !hasHighConfidenceMatch(candidates)) {
                candidates.addAll(findNameMatches(input));
            }

            // Remove duplicates and sort by confidence
            candidates = dedupAndSortCandidates(candidates);

            // Make decision based on candidates
            CustomerMatchDecisionDTO decision = makeMatchDecision(candidates);

            return CustomerMatchResponse.builder()
                    .candidates(candidates)
                    .decision(decision)
                    .build();

        } catch (Exception e) {
            log.error("Error finding customer matches", e);
            return CustomerMatchResponse.builder()
                    .decision(CustomerMatchDecisionDTO.createNew("Error during matching process: " + e.getMessage()))
                    .candidates(new ArrayList<>())
                    .build();
        }
    }

    /**
     * Find matches based on phone number (exact and partial)
     */
    private List<CustomerCandidateDTO> findPhoneMatches(CustomerMatchInput input) {
        List<CustomerCandidateDTO> candidates = new ArrayList<>();

        // Normalize input phone
        String normalizedPhone = phoneNormalizer.toE164(input.getPhone());
        if (normalizedPhone == null) {
            log.debug("Could not normalize phone number: {}", input.getPhone());
            return candidates;
        }

        try {
            // Exact phone match
            Customer exactMatch = customerDAO.findByPhoneNumber(normalizedPhone);
            if (exactMatch != null) {
                candidates.add(createCandidateDTO(exactMatch, PHONE_EXACT_CONFIDENCE, 
                    "Exact phone number match"));
            }

            // TODO: Partial phone matching would require additional DAO methods
            // For now, we only do exact matching

        } catch (Exception e) {
            log.error("Error finding phone matches for: {}", normalizedPhone, e);
        }

        return candidates;
    }

    /**
     * Find matches based on email address
     */
    private List<CustomerCandidateDTO> findEmailMatches(CustomerMatchInput input) {
        List<CustomerCandidateDTO> candidates = new ArrayList<>();

        try {
            String email = input.getEmail().trim().toLowerCase();
            Customer emailMatch = customerDAO.findByEmail(email);
            
            if (emailMatch != null) {
                candidates.add(createCandidateDTO(emailMatch, EMAIL_EXACT_CONFIDENCE, 
                    "Exact email match"));
            }

        } catch (Exception e) {
            log.error("Error finding email matches for: {}", input.getEmail(), e);
        }

        return candidates;
    }

    /**
     * Find matches based on name similarity
     */
    private List<CustomerCandidateDTO> findNameMatches(CustomerMatchInput input) {
        List<CustomerCandidateDTO> candidates = new ArrayList<>();

        String inputFullName = input.getComputedFullName();
        if (inputFullName == null || inputFullName.trim().isEmpty()) {
            return candidates;
        }

        try {
            // Get all customers for name matching (could be optimized with better indexing)
            List<Customer> allCustomers = customerDAO.findAll();
            
            for (Customer customer : allCustomers) {
                String customerFullName = buildFullName(customer.getName(), customer.getSurname());
                
                double similarity = fuzzyNameMatcher.calculateSimilarity(
                    inputFullName, customerFullName);
                
                if (similarity >= NAME_SIMILARITY_THRESHOLD) {
                    double confidence = Math.min(FULL_NAME_FUZZY_CONFIDENCE, similarity);
                    
                    candidates.add(createCandidateDTO(customer, confidence, 
                        String.format("Name similarity: %.2f", similarity)));
                }
            }

        } catch (Exception e) {
            log.error("Error finding name matches", e);
        }

        return candidates;
    }

    /**
     * Create candidate DTO from customer entity
     */
    private CustomerCandidateDTO createCandidateDTO(Customer customer, double confidence, 
                                                   String reason) {
        return CustomerCandidateDTO.builder()
                .id(convertToUUID(customer.getId()))
                .displayName(buildFullName(customer.getName(), customer.getSurname()))
                .email(customer.getEmail())
                .phoneE164(customer.getPhoneNumber())
                .confidence(confidence)
                .reason(reason)
                .build();
    }

    /**
     * Convert Long ID to UUID for compatibility
     */
    private UUID convertToUUID(Long id) {
        if (id == null) {
            return null;
        }
        // Simple conversion - in production you might want a more sophisticated mapping
        return UUID.nameUUIDFromBytes(id.toString().getBytes());
    }

    /**
     * Build full name from components, handling nulls
     */
    private String buildFullName(String firstName, String lastName) {
        StringBuilder fullName = new StringBuilder();
        
        if (firstName != null && !firstName.trim().isEmpty()) {
            fullName.append(firstName.trim());
        }
        
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName.trim());
        }
        
        return fullName.toString();
    }

    /**
     * Check if any candidate has high confidence
     */
    private boolean hasHighConfidenceMatch(List<CustomerCandidateDTO> candidates) {
        return candidates.stream()
                .anyMatch(CustomerCandidateDTO::isHighConfidence);
    }

    /**
     * Remove duplicates and sort candidates by confidence
     */
    private List<CustomerCandidateDTO> dedupAndSortCandidates(List<CustomerCandidateDTO> candidates) {
        // Remove duplicates by customer ID (keep highest confidence)
        return candidates.stream()
                .collect(java.util.stream.Collectors.toMap(
                    CustomerCandidateDTO::getId,
                    c -> c,
                    (existing, replacement) -> existing.getConfidence() > replacement.getConfidence() ? existing : replacement
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(CustomerCandidateDTO::getConfidence).reversed())
                .limit(5) // Limit to top 5 candidates
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Make decision based on candidates
     */
    private CustomerMatchDecisionDTO makeMatchDecision(List<CustomerCandidateDTO> candidates) {
        if (candidates.isEmpty()) {
            return CustomerMatchDecisionDTO.createNew("No matching customers found");
        }

        CustomerCandidateDTO bestMatch = candidates.get(0);
        
        // High confidence matches - auto attach
        if (bestMatch.getConfidence() >= PHONE_EXACT_CONFIDENCE) {
            return CustomerMatchDecisionDTO.autoAttach(
                "Exact phone number match with high confidence");
        }
        
        if (bestMatch.getConfidence() >= EMAIL_EXACT_CONFIDENCE) {
            return CustomerMatchDecisionDTO.autoAttach(
                "Exact email match with high confidence");
        }

        // Medium confidence matches - require confirmation
        if (bestMatch.getConfidence() >= NAME_PHONE_PARTIAL_CONFIDENCE) {
            return CustomerMatchDecisionDTO.confirm(
                "Good match found but requires confirmation: " + bestMatch.getReason());
        }

        // Low confidence matches - suggest but recommend creating new
        if (bestMatch.getConfidence() >= FULL_NAME_FUZZY_CONFIDENCE) {
            return CustomerMatchDecisionDTO.createNew(
                "Possible match found but confidence is low. Consider creating new customer.");
        }

        // Very low confidence - create new
        return CustomerMatchDecisionDTO.createNew(
            "No confident matches found. Recommend creating new customer.");
    }
}