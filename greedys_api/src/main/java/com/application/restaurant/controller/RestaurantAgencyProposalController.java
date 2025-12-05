package com.application.restaurant.controller;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.persistence.model.group.AgencyProposal;
import com.application.common.persistence.model.group.ProposalRevision;
import com.application.common.persistence.model.group.enums.ProposalStatus;
import com.application.common.service.group.AgencyProposalService;
import com.application.restaurant.persistence.model.user.RUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller per la gestione delle Proposte Personalizzate per Agency.
 * Permette al ristoratore di creare offerte personalizzate per le agenzie partner.
 * Supporta negoziazione bidirezionale con audit trail completo.
 */
@RestController
@RequestMapping("/restaurant/group/proposals")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Restaurant Agency Proposals", description = "Gestione proposte personalizzate per agenzie")
@RequiredArgsConstructor
@Slf4j
public class RestaurantAgencyProposalController extends BaseController {

    private final AgencyProposalService agencyProposalService;
    private final ObjectMapper objectMapper;

    // ==================== CREATE ====================

    @Operation(summary = "Crea una nuova proposta per un'agenzia")
    @CreateApiResponses
    @PostMapping("/agency/{agencyId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_PROPOSAL_WRITE')")
    public ResponseEntity<AgencyProposal> createProposal(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long agencyId,
            @RequestBody CreateProposalRequest request) {
        
        return executeCreate("create proposal", () -> {
            AgencyProposal proposal = AgencyProposal.builder()
                .title(request.title())
                .description(request.description())
                .proposedPrice(request.proposedPrice())
                .originalPrice(request.originalPrice())
                .childrenPrice(request.childrenPrice())
                .discountPercentage(request.discountPercentage())
                .minPax(request.minPax())
                .maxPax(request.maxPax())
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .isExclusive(request.isExclusive())
                .termsAndConditions(request.termsAndConditions())
                .notesForAgency(request.notesForAgency())
                .customContent(request.customContent() != null ? objectMapper.valueToTree(request.customContent()) : null)
                .build();
            
            return agencyProposalService.createProposal(user.getRestaurant().getId(), agencyId, proposal);
        });
    }

    @Operation(summary = "Crea proposta basata su un menù esistente")
    @CreateApiResponses
    @PostMapping("/from-menu/{menuId}/agency/{agencyId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_PROPOSAL_WRITE')")
    public ResponseEntity<AgencyProposal> createFromMenu(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long menuId,
            @PathVariable Long agencyId,
            @RequestParam(required = false) BigDecimal proposedPrice,
            @RequestParam(required = false) String title) {
        
        return executeCreate("create proposal from menu", () ->
            agencyProposalService.createFromMenu(user.getRestaurant().getId(), agencyId, menuId, proposedPrice, title));
    }

    // ==================== READ ====================

    @Operation(summary = "Ottieni tutte le proposte")
    @ReadApiResponses
    @GetMapping
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_PROPOSAL_READ')")
    public ResponseEntity<Page<AgencyProposal>> getProposals(
            @AuthenticationPrincipal RUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return executePaginated("get proposals", () ->
            agencyProposalService.getByRestaurant(user.getRestaurant().getId(), PageRequest.of(page, size)));
    }

    @Operation(summary = "Ottieni proposte attive")
    @ReadApiResponses
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_PROPOSAL_READ')")
    public ResponseEntity<List<AgencyProposal>> getActiveProposals(@AuthenticationPrincipal RUser user) {
        return executeList("get active proposals", () ->
            agencyProposalService.getActiveByRestaurant(user.getRestaurant().getId()));
    }

    @Operation(summary = "Ottieni proposte in attesa di risposta dal ristorante")
    @ReadApiResponses
    @GetMapping("/pending-response")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_PROPOSAL_READ')")
    public ResponseEntity<List<AgencyProposal>> getPendingResponse(@AuthenticationPrincipal RUser user) {
        return executeList("get pending response", () ->
            agencyProposalService.getPendingRestaurantResponse(user.getRestaurant().getId()));
    }

    @Operation(summary = "Ottieni proposte per una specifica agenzia")
    @ReadApiResponses
    @GetMapping("/agency/{agencyId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_PROPOSAL_READ')")
    public ResponseEntity<List<AgencyProposal>> getByAgency(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long agencyId) {
        
        return executeList("get proposals by agency", () ->
            agencyProposalService.getByRestaurantAndAgency(user.getRestaurant().getId(), agencyId));
    }

    @Operation(summary = "Ottieni proposte esclusive per un'agenzia")
    @ReadApiResponses
    @GetMapping("/agency/{agencyId}/exclusive")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_PROPOSAL_READ')")
    public ResponseEntity<List<AgencyProposal>> getExclusiveProposals(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long agencyId) {
        
        return executeList("get exclusive proposals", () ->
            agencyProposalService.getExclusiveProposals(user.getRestaurant().getId(), agencyId));
    }

    @Operation(summary = "Ottieni una proposta specifica")
    @ReadApiResponses
    @GetMapping("/{proposalId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_PROPOSAL_READ')")
    public ResponseEntity<AgencyProposal> getProposal(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long proposalId) {
        
        return execute("get proposal", () -> {
            AgencyProposal proposal = agencyProposalService.getById(proposalId);
            validateOwnership(user, proposal);
            return proposal;
        });
    }

    @Operation(summary = "Cerca proposta per codice")
    @ReadApiResponses
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_PROPOSAL_READ')")
    public ResponseEntity<AgencyProposal> getByCode(
            @AuthenticationPrincipal RUser user,
            @PathVariable String code) {
        
        return execute("get proposal by code", () ->
            agencyProposalService.getByCode(code)
                .filter(p -> p.getRestaurant().getId().equals(user.getRestaurant().getId()))
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found")));
    }

    // ==================== UPDATE ====================

    @Operation(summary = "Aggiorna una proposta (dal ristorante)")
    @ReadApiResponses
    @PutMapping("/{proposalId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_PROPOSAL_WRITE')")
    public ResponseEntity<AgencyProposal> updateProposal(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long proposalId,
            @RequestBody UpdateProposalRequest request) {
        
        return execute("update proposal", () -> {
            AgencyProposal proposal = agencyProposalService.getById(proposalId);
            validateOwnership(user, proposal);
            
            AgencyProposal updates = AgencyProposal.builder()
                .title(request.title())
                .description(request.description())
                .proposedPrice(request.proposedPrice())
                .childrenPrice(request.childrenPrice())
                .discountPercentage(request.discountPercentage())
                .minPax(request.minPax())
                .maxPax(request.maxPax())
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .termsAndConditions(request.termsAndConditions())
                .notesForAgency(request.notesForAgency())
                .customContent(request.customContent() != null ? objectMapper.valueToTree(request.customContent()) : null)
                .build();
            
            return agencyProposalService.updateByRestaurant(proposalId, updates, user.getId(), request.note());
        });
    }

    // ==================== STATUS MANAGEMENT ====================

    @Operation(summary = "Invia la proposta all'agenzia")
    @ReadApiResponses
    @PostMapping("/{proposalId}/send")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_PROPOSAL_WRITE')")
    public ResponseEntity<AgencyProposal> sendProposal(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long proposalId) {
        
        return execute("send proposal", () -> {
            AgencyProposal proposal = agencyProposalService.getById(proposalId);
            validateOwnership(user, proposal);
            return agencyProposalService.send(proposalId, user.getId());
        });
    }

    @Operation(summary = "Attiva la proposta (dopo accettazione)")
    @ReadApiResponses
    @PostMapping("/{proposalId}/activate")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_PROPOSAL_WRITE')")
    public ResponseEntity<AgencyProposal> activateProposal(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long proposalId) {
        
        return execute("activate proposal", () -> {
            AgencyProposal proposal = agencyProposalService.getById(proposalId);
            validateOwnership(user, proposal);
            return agencyProposalService.activate(proposalId, user.getId());
        });
    }

    @Operation(summary = "Rifiuta una controproposta dell'agenzia")
    @ReadApiResponses
    @PostMapping("/{proposalId}/reject")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_PROPOSAL_WRITE')")
    public ResponseEntity<AgencyProposal> rejectProposal(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long proposalId,
            @RequestBody(required = false) RejectRequest request) {
        
        return execute("reject proposal", () -> {
            AgencyProposal proposal = agencyProposalService.getById(proposalId);
            validateOwnership(user, proposal);
            String reason = request != null ? request.reason() : "Rifiutato dal ristorante";
            return agencyProposalService.reject(proposalId, reason, "RESTAURANT", user.getId());
        });
    }

    @Operation(summary = "Cancella una proposta")
    @ReadApiResponses
    @PostMapping("/{proposalId}/cancel")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_PROPOSAL_WRITE')")
    public ResponseEntity<AgencyProposal> cancelProposal(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long proposalId) {
        
        return execute("cancel proposal", () -> {
            AgencyProposal proposal = agencyProposalService.getById(proposalId);
            validateOwnership(user, proposal);
            return agencyProposalService.cancel(proposalId, "RESTAURANT", user.getId());
        });
    }

    // ==================== AUDIT TRAIL ====================

    @Operation(summary = "Ottieni lo storico delle revisioni")
    @ReadApiResponses
    @GetMapping("/{proposalId}/revisions")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_PROPOSAL_READ')")
    public ResponseEntity<List<ProposalRevision>> getRevisions(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long proposalId) {
        
        return executeList("get revisions", () -> {
            AgencyProposal proposal = agencyProposalService.getById(proposalId);
            validateOwnership(user, proposal);
            return agencyProposalService.getRevisions(proposalId);
        });
    }

    @Operation(summary = "Ottieni una specifica revisione")
    @ReadApiResponses
    @GetMapping("/{proposalId}/revisions/{version}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_PROPOSAL_READ')")
    public ResponseEntity<ProposalRevision> getRevision(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long proposalId,
            @PathVariable Integer version) {
        
        return execute("get revision", () -> {
            AgencyProposal proposal = agencyProposalService.getById(proposalId);
            validateOwnership(user, proposal);
            return agencyProposalService.getRevision(proposalId, version)
                .orElseThrow(() -> new IllegalArgumentException("Revision not found"));
        });
    }

    // ==================== STATISTICS ====================

    @Operation(summary = "Conta proposte per stato")
    @ReadApiResponses
    @GetMapping("/count/{status}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_PROPOSAL_READ')")
    public ResponseEntity<Long> countByStatus(
            @AuthenticationPrincipal RUser user,
            @PathVariable ProposalStatus status) {
        
        return execute("count by status", () ->
            agencyProposalService.countByStatus(user.getRestaurant().getId(), status));
    }

    // ==================== HELPER ====================

    private void validateOwnership(RUser user, AgencyProposal proposal) {
        if (!proposal.getRestaurant().getId().equals(user.getRestaurant().getId())) {
            throw new IllegalArgumentException("Proposal does not belong to this restaurant");
        }
    }

    // ==================== DTOs ====================

    public record CreateProposalRequest(
        String title,
        String description,
        BigDecimal proposedPrice,
        BigDecimal originalPrice,
        BigDecimal childrenPrice,
        BigDecimal discountPercentage,
        Integer minPax,
        Integer maxPax,
        LocalDate validFrom,
        LocalDate validUntil,
        Boolean isExclusive,
        String termsAndConditions,
        String notesForAgency,
        Map<String, Object> customContent
    ) {}

    public record UpdateProposalRequest(
        String title,
        String description,
        BigDecimal proposedPrice,
        BigDecimal childrenPrice,
        BigDecimal discountPercentage,
        Integer minPax,
        Integer maxPax,
        LocalDate validFrom,
        LocalDate validUntil,
        String termsAndConditions,
        String notesForAgency,
        Map<String, Object> customContent,
        String note
    ) {}

    public record RejectRequest(String reason) {}
}
