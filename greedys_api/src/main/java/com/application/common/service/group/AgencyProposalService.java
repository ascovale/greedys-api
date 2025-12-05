package com.application.common.service.group;

import com.application.agency.persistence.dao.AgencyDAO;
import com.application.agency.persistence.model.Agency;
import com.application.common.persistence.dao.group.AgencyProposalDAO;
import com.application.common.persistence.dao.group.FixedPriceMenuDAO;
import com.application.common.persistence.dao.group.ProposalRevisionDAO;
import com.application.common.persistence.dao.group.RestaurantAgencyDAO;
import com.application.common.persistence.model.group.AgencyProposal;
import com.application.common.persistence.model.group.FixedPriceMenu;
import com.application.common.persistence.model.group.ProposalRevision;
import com.application.common.persistence.model.group.RestaurantAgency;
import com.application.common.persistence.model.group.enums.ProposalStatus;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service per la gestione delle Proposte Personalizzate per Agency.
 * Supporta negoziazione bidirezionale con audit trail completo.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AgencyProposalService {

    private final AgencyProposalDAO agencyProposalDAO;
    private final ProposalRevisionDAO proposalRevisionDAO;
    private final RestaurantAgencyDAO restaurantAgencyDAO;
    private final FixedPriceMenuDAO fixedPriceMenuDAO;
    private final RestaurantDAO restaurantDAO;
    private final AgencyDAO agencyDAO;
    private final ObjectMapper objectMapper;

    // ==================== CREATE ====================

    /**
     * Crea una nuova proposta (draft)
     */
    public AgencyProposal createProposal(Long restaurantId, Long agencyId, AgencyProposal proposal) {
        Restaurant restaurant = restaurantDAO.findById(restaurantId)
            .orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));
        Agency agency = agencyDAO.findById(agencyId)
            .orElseThrow(() -> new IllegalArgumentException("Agency not found: " + agencyId));

        // Verifica relazione B2B (opzionale ma consigliata)
        Optional<RestaurantAgency> relation = restaurantAgencyDAO.findByRestaurantIdAndAgencyId(restaurantId, agencyId);
        
        proposal.setRestaurant(restaurant);
        proposal.setAgency(agency);
        relation.ifPresent(proposal::setRestaurantAgency);
        proposal.setStatus(ProposalStatus.DRAFT);
        proposal.setVersion(1);
        proposal.setIsExclusive(proposal.getIsExclusive() != null ? proposal.getIsExclusive() : true);
        
        AgencyProposal saved = agencyProposalDAO.save(proposal);
        
        // Crea prima revisione
        createRevision(saved, "RESTAURANT", null, "Creazione proposta", "CREATION");
        
        log.info("Created proposal {} for restaurant {} -> agency {}", saved.getProposalCode(), restaurantId, agencyId);
        return saved;
    }

    /**
     * Crea proposta basata su un menu esistente
     */
    public AgencyProposal createFromMenu(Long restaurantId, Long agencyId, Long menuId, 
                                          BigDecimal proposedPrice, String title) {
        FixedPriceMenu menu = fixedPriceMenuDAO.findById(menuId)
            .orElseThrow(() -> new IllegalArgumentException("Menu not found: " + menuId));
        
        AgencyProposal proposal = AgencyProposal.builder()
            .title(title != null ? title : "Proposta: " + menu.getName())
            .description(menu.getDescription())
            .baseMenu(menu)
            .proposedPrice(proposedPrice != null ? proposedPrice : menu.getPriceForAgency())
            .originalPrice(menu.getBasePrice())
            .minPax(menu.getMinimumPax())
            .maxPax(menu.getMaximumPax())
            .build();
        
        return createProposal(restaurantId, agencyId, proposal);
    }

    // ==================== READ ====================

    public AgencyProposal getById(Long proposalId) {
        return agencyProposalDAO.findById(proposalId)
            .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));
    }

    public Optional<AgencyProposal> getByCode(String code) {
        return agencyProposalDAO.findByProposalCode(code);
    }

    public Page<AgencyProposal> getByRestaurant(Long restaurantId, Pageable pageable) {
        return agencyProposalDAO.findByRestaurantId(restaurantId, pageable);
    }

    public List<AgencyProposal> getActiveByRestaurant(Long restaurantId) {
        return agencyProposalDAO.findActiveByRestaurant(restaurantId);
    }

    public Page<AgencyProposal> getByAgency(Long agencyId, Pageable pageable) {
        return agencyProposalDAO.findByAgencyId(agencyId, pageable);
    }

    public List<AgencyProposal> getActiveByAgency(Long agencyId) {
        return agencyProposalDAO.findActiveByAgency(agencyId);
    }

    public List<AgencyProposal> getByRestaurantAndAgency(Long restaurantId, Long agencyId) {
        return agencyProposalDAO.findByRestaurantAndAgency(restaurantId, agencyId);
    }

    public List<AgencyProposal> getExclusiveProposals(Long restaurantId, Long agencyId) {
        return agencyProposalDAO.findExclusiveProposals(restaurantId, agencyId);
    }

    public List<AgencyProposal> getValidProposals(Long agencyId) {
        return agencyProposalDAO.findValidByAgency(agencyId, LocalDate.now());
    }

    public List<AgencyProposal> getPendingRestaurantResponse(Long restaurantId) {
        return agencyProposalDAO.findPendingRestaurantResponse(restaurantId);
    }

    public List<AgencyProposal> getPendingAgencyResponse(Long agencyId) {
        return agencyProposalDAO.findPendingAgencyResponse(agencyId);
    }

    // ==================== UPDATE ====================

    /**
     * Aggiorna la proposta (dal ristorante)
     */
    public AgencyProposal updateByRestaurant(Long proposalId, AgencyProposal updates, Long userId, String note) {
        AgencyProposal proposal = getById(proposalId);
        
        if (!proposal.canEdit()) {
            throw new IllegalStateException("Proposal cannot be edited in status: " + proposal.getStatus());
        }
        
        // Traccia campi modificati
        ArrayNode changedFields = objectMapper.createArrayNode();
        
        if (updates.getTitle() != null && !updates.getTitle().equals(proposal.getTitle())) {
            changedFields.add("title");
            proposal.setTitle(updates.getTitle());
        }
        if (updates.getDescription() != null) {
            changedFields.add("description");
            proposal.setDescription(updates.getDescription());
        }
        if (updates.getProposedPrice() != null && !updates.getProposedPrice().equals(proposal.getProposedPrice())) {
            changedFields.add("proposedPrice");
            proposal.setProposedPrice(updates.getProposedPrice());
        }
        if (updates.getChildrenPrice() != null) {
            changedFields.add("childrenPrice");
            proposal.setChildrenPrice(updates.getChildrenPrice());
        }
        if (updates.getDiscountPercentage() != null) {
            changedFields.add("discountPercentage");
            proposal.setDiscountPercentage(updates.getDiscountPercentage());
        }
        if (updates.getMinPax() != null) {
            changedFields.add("minPax");
            proposal.setMinPax(updates.getMinPax());
        }
        if (updates.getMaxPax() != null) {
            changedFields.add("maxPax");
            proposal.setMaxPax(updates.getMaxPax());
        }
        if (updates.getValidFrom() != null) {
            changedFields.add("validFrom");
            proposal.setValidFrom(updates.getValidFrom());
        }
        if (updates.getValidUntil() != null) {
            changedFields.add("validUntil");
            proposal.setValidUntil(updates.getValidUntil());
        }
        if (updates.getCustomContent() != null) {
            changedFields.add("customContent");
            proposal.setCustomContent(updates.getCustomContent());
        }
        if (updates.getTermsAndConditions() != null) {
            changedFields.add("termsAndConditions");
            proposal.setTermsAndConditions(updates.getTermsAndConditions());
        }
        if (updates.getNotesForAgency() != null) {
            changedFields.add("notesForAgency");
            proposal.setNotesForAgency(updates.getNotesForAgency());
        }
        
        proposal.setLastModifiedByType("RESTAURANT");
        proposal.setUpdatedBy(userId);
        
        AgencyProposal saved = agencyProposalDAO.save(proposal);
        
        // Crea revisione
        createRevision(saved, "RESTAURANT", userId, note, "UPDATE", changedFields);
        
        log.info("Proposal {} updated by restaurant", proposalId);
        return saved;
    }

    /**
     * Controproposta dall'agency
     */
    public AgencyProposal counterProposal(Long proposalId, BigDecimal newPrice, String note, Long userId) {
        AgencyProposal proposal = getById(proposalId);
        
        if (!proposal.canEdit()) {
            throw new IllegalStateException("Proposal cannot be modified in status: " + proposal.getStatus());
        }
        
        ArrayNode changedFields = objectMapper.createArrayNode();
        changedFields.add("proposedPrice");
        
        proposal.setProposedPrice(newPrice);
        proposal.setStatus(ProposalStatus.COUNTER_PROPOSAL);
        proposal.setLastModifiedByType("AGENCY");
        proposal.setUpdatedBy(userId);
        
        AgencyProposal saved = agencyProposalDAO.save(proposal);
        
        createRevision(saved, "AGENCY", userId, note, "COUNTER_PROPOSAL", changedFields);
        
        log.info("Counter-proposal from agency for proposal {}", proposalId);
        return saved;
    }

    // ==================== STATUS MANAGEMENT ====================

    /**
     * Invia la proposta all'agency
     */
    public AgencyProposal send(Long proposalId, Long userId) {
        AgencyProposal proposal = getById(proposalId);
        
        if (proposal.getStatus() != ProposalStatus.DRAFT) {
            throw new IllegalStateException("Only draft proposals can be sent");
        }
        
        proposal.setStatus(ProposalStatus.SENT);
        proposal.setSentAt(LocalDateTime.now());
        proposal.setLastModifiedByType("RESTAURANT");
        
        AgencyProposal saved = agencyProposalDAO.save(proposal);
        createRevision(saved, "RESTAURANT", userId, "Proposta inviata all'agenzia", "SENT");
        
        log.info("Proposal {} sent to agency", proposalId);
        return saved;
    }

    /**
     * Agency accetta la proposta
     */
    public AgencyProposal accept(Long proposalId, Long userId) {
        AgencyProposal proposal = getById(proposalId);
        
        if (!proposal.canAccept()) {
            throw new IllegalStateException("Proposal cannot be accepted in status: " + proposal.getStatus());
        }
        
        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposal.setAcceptedAt(LocalDateTime.now());
        proposal.setLastModifiedByType("AGENCY");
        
        AgencyProposal saved = agencyProposalDAO.save(proposal);
        createRevision(saved, "AGENCY", userId, "Proposta accettata", "ACCEPTED");
        
        log.info("Proposal {} accepted by agency", proposalId);
        return saved;
    }

    /**
     * Rifiuta la proposta
     */
    public AgencyProposal reject(Long proposalId, String reason, String rejectedByType, Long userId) {
        AgencyProposal proposal = getById(proposalId);
        
        proposal.setStatus(ProposalStatus.REJECTED);
        proposal.setRejectedAt(LocalDateTime.now());
        proposal.setRejectionReason(reason);
        proposal.setLastModifiedByType(rejectedByType);
        
        AgencyProposal saved = agencyProposalDAO.save(proposal);
        createRevision(saved, rejectedByType, userId, "Proposta rifiutata: " + reason, "REJECTED");
        
        log.info("Proposal {} rejected by {}", proposalId, rejectedByType);
        return saved;
    }

    /**
     * Attiva la proposta (rendendola utilizzabile per prenotazioni)
     */
    public AgencyProposal activate(Long proposalId, Long userId) {
        AgencyProposal proposal = getById(proposalId);
        
        if (proposal.getStatus() != ProposalStatus.ACCEPTED) {
            throw new IllegalStateException("Only accepted proposals can be activated");
        }
        
        proposal.setStatus(ProposalStatus.ACTIVE);
        
        AgencyProposal saved = agencyProposalDAO.save(proposal);
        createRevision(saved, "RESTAURANT", userId, "Proposta attivata", "ACTIVATED");
        
        log.info("Proposal {} activated", proposalId);
        return saved;
    }

    /**
     * Cancella la proposta
     */
    public AgencyProposal cancel(Long proposalId, String cancelledByType, Long userId) {
        AgencyProposal proposal = getById(proposalId);
        
        proposal.setStatus(ProposalStatus.CANCELLED);
        proposal.setLastModifiedByType(cancelledByType);
        
        AgencyProposal saved = agencyProposalDAO.save(proposal);
        createRevision(saved, cancelledByType, userId, "Proposta cancellata", "CANCELLED");
        
        log.info("Proposal {} cancelled by {}", proposalId, cancelledByType);
        return saved;
    }

    // ==================== AUDIT TRAIL ====================

    /**
     * Ottiene lo storico delle revisioni
     */
    public List<ProposalRevision> getRevisions(Long proposalId) {
        return proposalRevisionDAO.findByProposalIdOrderByVersionDesc(proposalId);
    }

    public Optional<ProposalRevision> getRevision(Long proposalId, Integer version) {
        return proposalRevisionDAO.findByProposalIdAndVersion(proposalId, version);
    }

    private void createRevision(AgencyProposal proposal, String modifiedByType, Long userId, 
                                String description, String changeType) {
        createRevision(proposal, modifiedByType, userId, description, changeType, null);
    }

    private void createRevision(AgencyProposal proposal, String modifiedByType, Long userId, 
                                String description, String changeType, JsonNode changedFields) {
        ProposalRevision revision = ProposalRevision.builder()
            .proposal(proposal)
            .version(proposal.getVersion())
            .modifiedByType(modifiedByType)
            .modifiedByUserId(userId)
            .status(proposal.getStatus())
            .proposedPrice(proposal.getProposedPrice())
            .childrenPrice(proposal.getChildrenPrice())
            .discountPercentage(proposal.getDiscountPercentage())
            .customContent(proposal.getCustomContent())
            .description(proposal.getDescription())
            .termsAndConditions(proposal.getTermsAndConditions())
            .changeType(changeType)
            .changeDescription(description)
            .changedFields(changedFields)
            .build();
        
        proposalRevisionDAO.save(revision);
        
        // Incrementa versione
        proposal.setVersion(proposal.getVersion() + 1);
        agencyProposalDAO.save(proposal);
    }

    // ==================== EXPIRATION ====================

    /**
     * Marca le proposte scadute
     */
    public int expireProposals() {
        List<AgencyProposal> expired = agencyProposalDAO.findExpiredProposals(LocalDate.now());
        
        for (AgencyProposal proposal : expired) {
            proposal.setStatus(ProposalStatus.EXPIRED);
            agencyProposalDAO.save(proposal);
            log.info("Proposal {} expired", proposal.getProposalCode());
        }
        
        return expired.size();
    }

    // ==================== STATISTICS ====================

    public Long countByStatus(Long restaurantId, ProposalStatus status) {
        return agencyProposalDAO.countByRestaurantAndStatus(restaurantId, status);
    }

    public Long countAcceptedByAgency(Long agencyId) {
        return agencyProposalDAO.countAcceptedByAgency(agencyId);
    }
}
