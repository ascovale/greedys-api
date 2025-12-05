package com.application.restaurant.controller;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.persistence.model.group.RestaurantAgency;
import com.application.common.service.group.RestaurantAgencyService;
import com.application.restaurant.persistence.model.user.RUser;
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
import java.util.List;

/**
 * Controller per la gestione delle Relazioni B2B tra Ristorante e Agenzie.
 * Permette al ristoratore di gestire le partnership commerciali con le agenzie di viaggio.
 */
@RestController
@RequestMapping("/restaurant/group/agencies")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Restaurant Agency Relations", description = "Gestione relazioni B2B ristorante-agenzie")
@RequiredArgsConstructor
@Slf4j
public class RestaurantAgencyRelationController extends BaseController {

    private final RestaurantAgencyService restaurantAgencyService;

    // ==================== CRUD ====================

    @Operation(summary = "Crea una nuova relazione B2B con un'agenzia")
    @CreateApiResponses
    @PostMapping("/{agencyId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_WRITE')")
    public ResponseEntity<RestaurantAgency> createRelationship(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long agencyId) {
        
        return executeCreate("create agency relationship", () ->
            restaurantAgencyService.createRelationship(user.getRestaurant().getId(), agencyId));
    }

    @Operation(summary = "Ottieni tutte le relazioni con le agenzie")
    @ReadApiResponses
    @GetMapping
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_READ')")
    public ResponseEntity<Page<RestaurantAgency>> getRelationships(
            @AuthenticationPrincipal RUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return executePaginated("get agency relationships", () ->
            restaurantAgencyService.getByRestaurant(user.getRestaurant().getId(), PageRequest.of(page, size)));
    }

    @Operation(summary = "Ottieni relazioni attive")
    @ReadApiResponses
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_READ')")
    public ResponseEntity<List<RestaurantAgency>> getActiveRelationships(@AuthenticationPrincipal RUser user) {
        return executeList("get active relationships", () ->
            restaurantAgencyService.getActiveByRestaurant(user.getRestaurant().getId()));
    }

    @Operation(summary = "Ottieni relazioni in attesa di approvazione")
    @ReadApiResponses
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_READ')")
    public ResponseEntity<List<RestaurantAgency>> getPendingRelationships(@AuthenticationPrincipal RUser user) {
        return executeList("get pending relationships", () ->
            restaurantAgencyService.getPendingByRestaurant(user.getRestaurant().getId()));
    }

    @Operation(summary = "Ottieni agenzie trusted")
    @ReadApiResponses
    @GetMapping("/trusted")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_READ')")
    public ResponseEntity<List<RestaurantAgency>> getTrustedAgencies(@AuthenticationPrincipal RUser user) {
        return executeList("get trusted agencies", () ->
            restaurantAgencyService.getTrustedByRestaurant(user.getRestaurant().getId()));
    }

    @Operation(summary = "Ottieni top agenzie per volume")
    @ReadApiResponses
    @GetMapping("/top")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_READ')")
    public ResponseEntity<List<RestaurantAgency>> getTopAgencies(
            @AuthenticationPrincipal RUser user,
            @RequestParam(defaultValue = "10") int limit) {
        
        return executeList("get top agencies", () ->
            restaurantAgencyService.getTopAgencies(user.getRestaurant().getId(), limit));
    }

    @Operation(summary = "Ottieni una relazione specifica")
    @ReadApiResponses
    @GetMapping("/{relationId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_READ')")
    public ResponseEntity<RestaurantAgency> getRelationship(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long relationId) {
        
        return execute("get relationship", () -> {
            RestaurantAgency relation = restaurantAgencyService.getById(relationId);
            validateOwnership(user, relation);
            return relation;
        });
    }

    @Operation(summary = "Ottieni relazione con una specifica agenzia")
    @ReadApiResponses
    @GetMapping("/agency/{agencyId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_READ')")
    public ResponseEntity<RestaurantAgency> getRelationshipWithAgency(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long agencyId) {
        
        return execute("get relationship with agency", () ->
            restaurantAgencyService.getByRestaurantAndAgency(user.getRestaurant().getId(), agencyId)
                .orElseThrow(() -> new IllegalArgumentException("No relationship with agency " + agencyId)));
    }

    // ==================== APPROVAL ====================

    @Operation(summary = "Approva una relazione B2B")
    @ReadApiResponses
    @PostMapping("/{relationId}/approve")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_WRITE')")
    public ResponseEntity<RestaurantAgency> approveRelationship(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long relationId) {
        
        return execute("approve relationship", () -> {
            RestaurantAgency relation = restaurantAgencyService.getById(relationId);
            validateOwnership(user, relation);
            return restaurantAgencyService.approve(relationId, user.getId());
        });
    }

    @Operation(summary = "Rifiuta una relazione B2B")
    @ReadApiResponses
    @PostMapping("/{relationId}/reject")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_WRITE')")
    public ResponseEntity<RestaurantAgency> rejectRelationship(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long relationId) {
        
        return execute("reject relationship", () -> {
            RestaurantAgency relation = restaurantAgencyService.getById(relationId);
            validateOwnership(user, relation);
            return restaurantAgencyService.reject(relationId);
        });
    }

    // ==================== TRUST ====================

    @Operation(summary = "Imposta agenzia come trusted")
    @ReadApiResponses
    @PostMapping("/{relationId}/trust")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_WRITE')")
    public ResponseEntity<RestaurantAgency> setTrusted(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long relationId,
            @RequestParam boolean trusted) {
        
        return execute("set trusted", () -> {
            RestaurantAgency relation = restaurantAgencyService.getById(relationId);
            validateOwnership(user, relation);
            return restaurantAgencyService.setTrusted(relationId, trusted);
        });
    }

    // ==================== COMMERCIAL TERMS ====================

    @Operation(summary = "Aggiorna termini commerciali")
    @ReadApiResponses
    @PutMapping("/{relationId}/terms")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_WRITE')")
    public ResponseEntity<RestaurantAgency> updateCommercialTerms(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long relationId,
            @RequestBody UpdateCommercialTermsRequest request) {
        
        return execute("update commercial terms", () -> {
            RestaurantAgency relation = restaurantAgencyService.getById(relationId);
            validateOwnership(user, relation);
            
            return restaurantAgencyService.updateCommercialTerms(
                relationId,
                request.discountPercentage(),
                request.commissionPercentage(),
                request.minPax(),
                request.maxPax(),
                request.depositPercentage(),
                request.advanceBookingDays(),
                request.paymentTermsDays()
            );
        });
    }

    @Operation(summary = "Aggiorna contatti")
    @ReadApiResponses
    @PutMapping("/{relationId}/contacts")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_WRITE')")
    public ResponseEntity<RestaurantAgency> updateContacts(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long relationId,
            @RequestBody UpdateContactsRequest request) {
        
        return execute("update contacts", () -> {
            RestaurantAgency relation = restaurantAgencyService.getById(relationId);
            validateOwnership(user, relation);
            
            return restaurantAgencyService.updateContacts(
                relationId,
                request.restaurantContactName(),
                request.restaurantContactEmail(),
                request.restaurantContactPhone(),
                request.agencyContactName(),
                request.agencyContactEmail(),
                request.agencyContactPhone()
            );
        });
    }

    @Operation(summary = "Aggiorna note")
    @ReadApiResponses
    @PutMapping("/{relationId}/notes")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_WRITE')")
    public ResponseEntity<RestaurantAgency> updateNotes(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long relationId,
            @RequestBody UpdateNotesRequest request) {
        
        return execute("update notes", () -> {
            RestaurantAgency relation = restaurantAgencyService.getById(relationId);
            validateOwnership(user, relation);
            
            return restaurantAgencyService.updateNotes(
                relationId,
                request.notesForAgency(),
                request.internalNotes()
            );
        });
    }

    // ==================== DEACTIVATE / REACTIVATE ====================

    @Operation(summary = "Disattiva relazione (soft delete)")
    @ReadApiResponses
    @DeleteMapping("/{relationId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_WRITE')")
    public ResponseEntity<Void> deactivateRelationship(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long relationId) {
        
        return executeVoid("deactivate relationship", () -> {
            RestaurantAgency relation = restaurantAgencyService.getById(relationId);
            validateOwnership(user, relation);
            restaurantAgencyService.deactivate(relationId);
        });
    }

    @Operation(summary = "Riattiva relazione")
    @ReadApiResponses
    @PostMapping("/{relationId}/reactivate")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_WRITE')")
    public ResponseEntity<RestaurantAgency> reactivateRelationship(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long relationId) {
        
        return execute("reactivate relationship", () -> {
            RestaurantAgency relation = restaurantAgencyService.getById(relationId);
            validateOwnership(user, relation);
            return restaurantAgencyService.reactivate(relationId);
        });
    }

    // ==================== VALIDATION ====================

    @Operation(summary = "Verifica se l'agenzia può prenotare")
    @ReadApiResponses
    @GetMapping("/agency/{agencyId}/can-book")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_READ')")
    public ResponseEntity<Boolean> canAgencyBook(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long agencyId) {
        
        return execute("check if agency can book", () ->
            restaurantAgencyService.canBook(user.getRestaurant().getId(), agencyId));
    }

    @Operation(summary = "Verifica se il PAX è valido per l'agenzia")
    @ReadApiResponses
    @GetMapping("/agency/{agencyId}/valid-pax")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_READ')")
    public ResponseEntity<Boolean> isValidPax(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long agencyId,
            @RequestParam int pax) {
        
        return execute("check valid pax", () ->
            restaurantAgencyService.isValidPax(user.getRestaurant().getId(), agencyId, pax));
    }

    // ==================== STATISTICS ====================

    @Operation(summary = "Conta relazioni attive")
    @ReadApiResponses
    @GetMapping("/count")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_AGENCY_READ')")
    public ResponseEntity<Long> countActiveRelationships(@AuthenticationPrincipal RUser user) {
        return execute("count active relationships", () ->
            restaurantAgencyService.countActiveByRestaurant(user.getRestaurant().getId()));
    }

    // ==================== HELPER ====================

    private void validateOwnership(RUser user, RestaurantAgency relation) {
        if (!relation.getRestaurant().getId().equals(user.getRestaurant().getId())) {
            throw new IllegalArgumentException("Relationship does not belong to this restaurant");
        }
    }

    // ==================== DTOs ====================

    public record UpdateCommercialTermsRequest(
        BigDecimal discountPercentage,
        BigDecimal commissionPercentage,
        Integer minPax,
        Integer maxPax,
        BigDecimal depositPercentage,
        Integer advanceBookingDays,
        Integer paymentTermsDays
    ) {}

    public record UpdateContactsRequest(
        String restaurantContactName,
        String restaurantContactEmail,
        String restaurantContactPhone,
        String agencyContactName,
        String agencyContactEmail,
        String agencyContactPhone
    ) {}

    public record UpdateNotesRequest(
        String notesForAgency,
        String internalNotes
    ) {}
}
