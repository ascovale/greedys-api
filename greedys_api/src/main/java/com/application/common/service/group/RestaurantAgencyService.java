package com.application.common.service.group;

import com.application.agency.persistence.dao.AgencyDAO;
import com.application.agency.persistence.model.Agency;
import com.application.common.persistence.dao.group.RestaurantAgencyDAO;
import com.application.common.persistence.model.group.RestaurantAgency;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.model.Restaurant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service per la gestione delle relazioni B2B tra Ristoranti e Agenzie.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RestaurantAgencyService {

    private final RestaurantAgencyDAO restaurantAgencyDAO;
    private final RestaurantDAO restaurantDAO;
    private final AgencyDAO agencyDAO;

    // ==================== CREATE ====================

    /**
     * Crea una nuova relazione B2B (pending approval)
     */
    public RestaurantAgency createRelationship(Long restaurantId, Long agencyId) {
        // Verifica che non esista già
        if (restaurantAgencyDAO.existsByRestaurantIdAndAgencyId(restaurantId, agencyId)) {
            throw new IllegalStateException("Relationship already exists between restaurant " + restaurantId + " and agency " + agencyId);
        }

        Restaurant restaurant = restaurantDAO.findById(restaurantId)
            .orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));
        Agency agency = agencyDAO.findById(agencyId)
            .orElseThrow(() -> new IllegalArgumentException("Agency not found: " + agencyId));

        RestaurantAgency relation = RestaurantAgency.builder()
            .restaurant(restaurant)
            .agency(agency)
            .isActive(true)
            .isApproved(false)
            .isTrusted(false)
            .build();

        log.info("Created B2B relationship between restaurant {} and agency {}", restaurantId, agencyId);
        return restaurantAgencyDAO.save(relation);
    }

    /**
     * Crea relazione già approvata (usato da admin)
     */
    public RestaurantAgency createApprovedRelationship(Long restaurantId, Long agencyId, Long approvedBy) {
        RestaurantAgency relation = createRelationship(restaurantId, agencyId);
        return approve(relation.getId(), approvedBy);
    }

    // ==================== READ ====================

    public RestaurantAgency getById(Long relationId) {
        return restaurantAgencyDAO.findById(relationId)
            .orElseThrow(() -> new IllegalArgumentException("Relationship not found: " + relationId));
    }

    public Optional<RestaurantAgency> getByRestaurantAndAgency(Long restaurantId, Long agencyId) {
        return restaurantAgencyDAO.findByRestaurantIdAndAgencyId(restaurantId, agencyId);
    }

    public Optional<RestaurantAgency> getActiveRelationship(Long restaurantId, Long agencyId) {
        return restaurantAgencyDAO.findActiveRelationship(restaurantId, agencyId);
    }

    public Page<RestaurantAgency> getByRestaurant(Long restaurantId, Pageable pageable) {
        return restaurantAgencyDAO.findByRestaurantId(restaurantId, pageable);
    }

    public List<RestaurantAgency> getActiveByRestaurant(Long restaurantId) {
        return restaurantAgencyDAO.findActiveByRestaurant(restaurantId);
    }

    public List<RestaurantAgency> getPendingByRestaurant(Long restaurantId) {
        return restaurantAgencyDAO.findPendingByRestaurant(restaurantId);
    }

    public Page<RestaurantAgency> getByAgency(Long agencyId, Pageable pageable) {
        return restaurantAgencyDAO.findByAgencyId(agencyId, pageable);
    }

    public List<RestaurantAgency> getActiveByAgency(Long agencyId) {
        return restaurantAgencyDAO.findActiveByAgency(agencyId);
    }

    public List<RestaurantAgency> getPendingByAgency(Long agencyId) {
        return restaurantAgencyDAO.findPendingByAgency(agencyId);
    }

    public List<RestaurantAgency> getTrustedByRestaurant(Long restaurantId) {
        return restaurantAgencyDAO.findTrustedByRestaurant(restaurantId);
    }

    public List<RestaurantAgency> getTopAgencies(Long restaurantId, int limit) {
        return restaurantAgencyDAO.findTopAgenciesByRestaurant(restaurantId, Pageable.ofSize(limit));
    }

    // ==================== UPDATE ====================

    /**
     * Approva una relazione B2B
     */
    public RestaurantAgency approve(Long relationId, Long approvedBy) {
        RestaurantAgency relation = getById(relationId);
        
        relation.setIsApproved(true);
        relation.setApprovedAt(LocalDateTime.now());
        relation.setApprovedBy(approvedBy);
        
        log.info("Approved B2B relationship {}", relationId);
        return restaurantAgencyDAO.save(relation);
    }

    /**
     * Rifiuta/revoca una relazione B2B
     */
    public RestaurantAgency reject(Long relationId) {
        RestaurantAgency relation = getById(relationId);
        
        relation.setIsApproved(false);
        relation.setIsActive(false);
        
        log.info("Rejected B2B relationship {}", relationId);
        return restaurantAgencyDAO.save(relation);
    }

    /**
     * Imposta l'agency come trusted
     */
    public RestaurantAgency setTrusted(Long relationId, boolean trusted) {
        RestaurantAgency relation = getById(relationId);
        
        relation.setIsTrusted(trusted);
        
        log.info("Set B2B relationship {} trusted={}", relationId, trusted);
        return restaurantAgencyDAO.save(relation);
    }

    /**
     * Aggiorna i termini commerciali
     */
    public RestaurantAgency updateCommercialTerms(Long relationId, 
                                                   BigDecimal discountPercentage,
                                                   BigDecimal commissionPercentage,
                                                   Integer minPax,
                                                   Integer maxPax,
                                                   BigDecimal depositPercentage,
                                                   Integer advanceBookingDays,
                                                   Integer paymentTermsDays) {
        RestaurantAgency relation = getById(relationId);
        
        if (discountPercentage != null) relation.setDefaultDiscountPercentage(discountPercentage);
        if (commissionPercentage != null) relation.setCommissionPercentage(commissionPercentage);
        if (minPax != null) relation.setMinPax(minPax);
        if (maxPax != null) relation.setMaxPax(maxPax);
        if (depositPercentage != null) relation.setDepositPercentage(depositPercentage);
        if (advanceBookingDays != null) relation.setAdvanceBookingDays(advanceBookingDays);
        if (paymentTermsDays != null) relation.setPaymentTermsDays(paymentTermsDays);
        
        log.info("Updated commercial terms for relationship {}", relationId);
        return restaurantAgencyDAO.save(relation);
    }

    /**
     * Aggiorna i contatti
     */
    public RestaurantAgency updateContacts(Long relationId,
                                           String restaurantContactName,
                                           String restaurantContactEmail,
                                           String restaurantContactPhone,
                                           String agencyContactName,
                                           String agencyContactEmail,
                                           String agencyContactPhone) {
        RestaurantAgency relation = getById(relationId);
        
        if (restaurantContactName != null) relation.setRestaurantContactName(restaurantContactName);
        if (restaurantContactEmail != null) relation.setRestaurantContactEmail(restaurantContactEmail);
        if (restaurantContactPhone != null) relation.setRestaurantContactPhone(restaurantContactPhone);
        if (agencyContactName != null) relation.setAgencyContactName(agencyContactName);
        if (agencyContactEmail != null) relation.setAgencyContactEmail(agencyContactEmail);
        if (agencyContactPhone != null) relation.setAgencyContactPhone(agencyContactPhone);
        
        return restaurantAgencyDAO.save(relation);
    }

    /**
     * Aggiorna le note
     */
    public RestaurantAgency updateNotes(Long relationId, String notesForAgency, String internalNotes) {
        RestaurantAgency relation = getById(relationId);
        
        if (notesForAgency != null) relation.setNotesForAgency(notesForAgency);
        if (internalNotes != null) relation.setInternalNotes(internalNotes);
        
        return restaurantAgencyDAO.save(relation);
    }

    // ==================== DELETE ====================

    /**
     * Disattiva la relazione (soft delete)
     */
    public void deactivate(Long relationId) {
        RestaurantAgency relation = getById(relationId);
        relation.setIsActive(false);
        restaurantAgencyDAO.save(relation);
        log.info("Deactivated B2B relationship {}", relationId);
    }

    /**
     * Riattiva la relazione
     */
    public RestaurantAgency reactivate(Long relationId) {
        RestaurantAgency relation = getById(relationId);
        relation.setIsActive(true);
        log.info("Reactivated B2B relationship {}", relationId);
        return restaurantAgencyDAO.save(relation);
    }

    // ==================== VALIDATION ====================

    /**
     * Verifica se l'agency può prenotare presso il ristorante
     */
    public boolean canBook(Long restaurantId, Long agencyId) {
        return restaurantAgencyDAO.findActiveRelationship(restaurantId, agencyId)
            .map(RestaurantAgency::canBook)
            .orElse(false);
    }

    /**
     * Verifica se il PAX è valido per la relazione
     */
    public boolean isValidPax(Long restaurantId, Long agencyId, int pax) {
        return restaurantAgencyDAO.findActiveRelationship(restaurantId, agencyId)
            .map(r -> r.isValidPax(pax))
            .orElse(false);
    }

    // ==================== STATISTICS ====================

    public Long countActiveByRestaurant(Long restaurantId) {
        return restaurantAgencyDAO.countActiveByRestaurant(restaurantId);
    }

    public Long countApprovedByAgency(Long agencyId) {
        return restaurantAgencyDAO.countApprovedByAgency(agencyId);
    }

    /**
     * Registra una prenotazione completata (aggiorna statistiche)
     */
    public void recordBooking(Long relationId, BigDecimal amount) {
        RestaurantAgency relation = getById(relationId);
        relation.incrementBookings(amount);
        restaurantAgencyDAO.save(relation);
    }
}
