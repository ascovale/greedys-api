package com.application.restaurant.controller;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.persistence.model.group.GroupBooking;
import com.application.common.persistence.model.group.GroupBookingDietaryNeeds;
import com.application.common.persistence.model.group.enums.DietaryRequirement;
import com.application.common.persistence.model.group.enums.GroupBookingStatus;
import com.application.common.service.group.GroupBookingService;
import com.application.restaurant.persistence.model.user.RUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Controller per la gestione delle Prenotazioni di Gruppo.
 * Permette al ristoratore di gestire prenotazioni per gruppi con menù fissi
 * e esigenze alimentari specifiche.
 */
@RestController
@RequestMapping("/restaurant/group/bookings")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Restaurant Group Booking", description = "Gestione prenotazioni di gruppo")
@RequiredArgsConstructor
@Slf4j
public class RestaurantGroupBookingController extends BaseController {

    private final GroupBookingService groupBookingService;

    // ==================== CRUD ====================

    @Operation(summary = "Crea una nuova prenotazione di gruppo")
    @CreateApiResponses
    @PostMapping
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE')")
    public ResponseEntity<GroupBooking> createBooking(
            @AuthenticationPrincipal RUser user,
            @RequestBody CreateGroupBookingRequest request) {
        
        return executeCreate("create group booking", () -> {
            GroupBooking booking = GroupBooking.builder()
                .eventName(request.eventName())
                .eventDescription(request.eventDescription())
                .eventDate(request.eventDate())
                .eventTime(request.eventTime())
                .totalPax(request.totalPax())
                .adultsCount(request.adultsCount())
                .childrenCount(request.childrenCount())
                .contactName(request.contactName())
                .contactEmail(request.contactEmail())
                .contactPhone(request.contactPhone())
                .menuNotes(request.menuNotes())
                .internalNotes(request.internalNotes())
                .depositRequired(request.depositRequired())
                .build();
            
            return groupBookingService.createBooking(user.getRestaurant().getId(), booking);
        });
    }

    @Operation(summary = "Ottieni tutte le prenotazioni di gruppo")
    @ReadApiResponses
    @GetMapping
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_READ')")
    public ResponseEntity<Page<GroupBooking>> getBookings(
            @AuthenticationPrincipal RUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return executePaginated("get group bookings", () ->
            groupBookingService.getByRestaurant(user.getRestaurant().getId(), PageRequest.of(page, size)));
    }

    @Operation(summary = "Ottieni prenotazioni attive")
    @ReadApiResponses
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_READ')")
    public ResponseEntity<List<GroupBooking>> getActiveBookings(@AuthenticationPrincipal RUser user) {
        return executeList("get active bookings", () ->
            groupBookingService.getActiveByRestaurant(user.getRestaurant().getId()));
    }

    @Operation(summary = "Ottieni prenotazioni imminenti")
    @ReadApiResponses
    @GetMapping("/upcoming")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_READ')")
    public ResponseEntity<List<GroupBooking>> getUpcomingBookings(@AuthenticationPrincipal RUser user) {
        return executeList("get upcoming bookings", () ->
            groupBookingService.getUpcoming(user.getRestaurant().getId()));
    }

    @Operation(summary = "Ottieni una prenotazione specifica")
    @ReadApiResponses
    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_READ')")
    public ResponseEntity<GroupBooking> getBooking(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long bookingId) {
        
        return execute("get booking", () -> {
            GroupBooking booking = groupBookingService.getById(bookingId);
            validateOwnership(user, booking);
            return booking;
        });
    }

    @Operation(summary = "Cerca prenotazione per codice conferma")
    @ReadApiResponses
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_READ')")
    public ResponseEntity<GroupBooking> getByCode(
            @AuthenticationPrincipal RUser user,
            @PathVariable String code) {
        
        return execute("get booking by code", () ->
            groupBookingService.getByConfirmationCode(code)
                .filter(b -> b.getRestaurant().getId().equals(user.getRestaurant().getId()))
                .orElseThrow(() -> new IllegalArgumentException("Booking not found")));
    }

    @Operation(summary = "Aggiorna una prenotazione")
    @ReadApiResponses
    @PutMapping("/{bookingId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE')")
    public ResponseEntity<GroupBooking> updateBooking(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long bookingId,
            @RequestBody UpdateGroupBookingRequest request) {
        
        return execute("update booking", () -> {
            GroupBooking booking = groupBookingService.getById(bookingId);
            validateOwnership(user, booking);
            
            GroupBooking updates = GroupBooking.builder()
                .eventName(request.eventName())
                .eventDescription(request.eventDescription())
                .eventDate(request.eventDate())
                .eventTime(request.eventTime())
                .totalPax(request.totalPax())
                .adultsCount(request.adultsCount())
                .childrenCount(request.childrenCount())
                .contactName(request.contactName())
                .contactEmail(request.contactEmail())
                .contactPhone(request.contactPhone())
                .menuNotes(request.menuNotes())
                .internalNotes(request.internalNotes())
                .build();
            
            return groupBookingService.updateBooking(bookingId, updates);
        });
    }

    // ==================== QUERIES ====================

    @Operation(summary = "Prenotazioni per data")
    @ReadApiResponses
    @GetMapping("/date/{date}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_READ')")
    public ResponseEntity<List<GroupBooking>> getByDate(
            @AuthenticationPrincipal RUser user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        return executeList("get bookings by date", () ->
            groupBookingService.getByDate(user.getRestaurant().getId(), date));
    }

    @Operation(summary = "Prenotazioni per range di date")
    @ReadApiResponses
    @GetMapping("/range")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_READ')")
    public ResponseEntity<List<GroupBooking>> getByDateRange(
            @AuthenticationPrincipal RUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        
        return executeList("get bookings by date range", () ->
            groupBookingService.getByDateRange(user.getRestaurant().getId(), start, end));
    }

    @Operation(summary = "Ricerca prenotazioni")
    @ReadApiResponses
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_READ')")
    public ResponseEntity<Page<GroupBooking>> searchBookings(
            @AuthenticationPrincipal RUser user,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return executePaginated("search bookings", () ->
            groupBookingService.searchBookings(user.getRestaurant().getId(), query, PageRequest.of(page, size)));
    }

    // ==================== STATUS MANAGEMENT ====================

    @Operation(summary = "Conferma una prenotazione")
    @ReadApiResponses
    @PostMapping("/{bookingId}/confirm")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE')")
    public ResponseEntity<GroupBooking> confirmBooking(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long bookingId) {
        
        return execute("confirm booking", () -> {
            GroupBooking booking = groupBookingService.getById(bookingId);
            validateOwnership(user, booking);
            return groupBookingService.confirm(bookingId, user.getId());
        });
    }

    @Operation(summary = "Cancella una prenotazione")
    @ReadApiResponses
    @PostMapping("/{bookingId}/cancel")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE')")
    public ResponseEntity<GroupBooking> cancelBooking(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long bookingId,
            @RequestBody(required = false) CancelRequest request) {
        
        return execute("cancel booking", () -> {
            GroupBooking booking = groupBookingService.getById(bookingId);
            validateOwnership(user, booking);
            String reason = request != null ? request.reason() : "Cancellato dal ristorante";
            return groupBookingService.cancel(bookingId, reason, user.getId());
        });
    }

    @Operation(summary = "Segna prenotazione come completata")
    @ReadApiResponses
    @PostMapping("/{bookingId}/complete")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE')")
    public ResponseEntity<GroupBooking> completeBooking(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long bookingId) {
        
        return execute("complete booking", () -> {
            GroupBooking booking = groupBookingService.getById(bookingId);
            validateOwnership(user, booking);
            return groupBookingService.complete(bookingId);
        });
    }

    @Operation(summary = "Segna prenotazione come no-show")
    @ReadApiResponses
    @PostMapping("/{bookingId}/no-show")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE')")
    public ResponseEntity<GroupBooking> markNoShow(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long bookingId) {
        
        return execute("mark no-show", () -> {
            GroupBooking booking = groupBookingService.getById(bookingId);
            validateOwnership(user, booking);
            return groupBookingService.markNoShow(bookingId);
        });
    }

    // ==================== PAYMENT ====================

    @Operation(summary = "Registra pagamento caparra")
    @ReadApiResponses
    @PostMapping("/{bookingId}/deposit")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE')")
    public ResponseEntity<GroupBooking> recordDeposit(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long bookingId,
            @RequestBody DepositRequest request) {
        
        return execute("record deposit", () -> {
            GroupBooking booking = groupBookingService.getById(bookingId);
            validateOwnership(user, booking);
            return groupBookingService.markDepositPaid(bookingId, request.amount());
        });
    }

    @Operation(summary = "Segna come pagato totalmente")
    @ReadApiResponses
    @PostMapping("/{bookingId}/paid")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE')")
    public ResponseEntity<GroupBooking> markFullyPaid(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long bookingId) {
        
        return execute("mark fully paid", () -> {
            GroupBooking booking = groupBookingService.getById(bookingId);
            validateOwnership(user, booking);
            return groupBookingService.markFullyPaid(bookingId);
        });
    }

    // ==================== MENU SELECTION ====================

    @Operation(summary = "Seleziona menù per la prenotazione")
    @ReadApiResponses
    @PostMapping("/{bookingId}/menu/{menuId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE')")
    public ResponseEntity<GroupBooking> selectMenu(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long bookingId,
            @PathVariable Long menuId) {
        
        return execute("select menu", () -> {
            GroupBooking booking = groupBookingService.getById(bookingId);
            validateOwnership(user, booking);
            return groupBookingService.selectMenu(bookingId, menuId);
        });
    }

    @Operation(summary = "Applica proposta personalizzata")
    @ReadApiResponses
    @PostMapping("/{bookingId}/proposal/{proposalId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE')")
    public ResponseEntity<GroupBooking> applyProposal(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long bookingId,
            @PathVariable Long proposalId) {
        
        return execute("apply proposal", () -> {
            GroupBooking booking = groupBookingService.getById(bookingId);
            validateOwnership(user, booking);
            return groupBookingService.applyProposal(bookingId, proposalId);
        });
    }

    // ==================== DIETARY NEEDS ====================

    @Operation(summary = "Ottieni esigenze alimentari della prenotazione")
    @ReadApiResponses
    @GetMapping("/{bookingId}/dietary-needs")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_READ')")
    public ResponseEntity<List<GroupBookingDietaryNeeds>> getDietaryNeeds(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long bookingId) {
        
        return executeList("get dietary needs", () -> {
            GroupBooking booking = groupBookingService.getById(bookingId);
            validateOwnership(user, booking);
            return groupBookingService.getDietaryNeeds(bookingId);
        });
    }

    @Operation(summary = "Ottieni esigenze alimentari critiche")
    @ReadApiResponses
    @GetMapping("/{bookingId}/dietary-needs/critical")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_READ')")
    public ResponseEntity<List<GroupBookingDietaryNeeds>> getCriticalDietaryNeeds(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long bookingId) {
        
        return executeList("get critical dietary needs", () -> {
            GroupBooking booking = groupBookingService.getById(bookingId);
            validateOwnership(user, booking);
            return groupBookingService.getCriticalDietaryNeeds(bookingId);
        });
    }

    @Operation(summary = "Imposta esigenze alimentari (bulk)")
    @ReadApiResponses
    @PostMapping("/{bookingId}/dietary-needs")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE')")
    public ResponseEntity<GroupBooking> setDietaryNeeds(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long bookingId,
            @RequestBody Map<DietaryRequirement, Integer> needs) {
        
        return execute("set dietary needs", () -> {
            GroupBooking booking = groupBookingService.getById(bookingId);
            validateOwnership(user, booking);
            return groupBookingService.addDietaryNeeds(bookingId, needs);
        });
    }

    @Operation(summary = "Aggiungi singola esigenza alimentare")
    @CreateApiResponses
    @PostMapping("/{bookingId}/dietary-needs/add")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE')")
    public ResponseEntity<GroupBookingDietaryNeeds> addDietaryNeed(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long bookingId,
            @RequestBody AddDietaryNeedRequest request) {
        
        return executeCreate("add dietary need", () -> {
            GroupBooking booking = groupBookingService.getById(bookingId);
            validateOwnership(user, booking);
            return groupBookingService.addDietaryNeed(
                bookingId, 
                request.requirement(), 
                request.paxCount(), 
                request.notes(),
                request.isCritical()
            );
        });
    }

    // ==================== STATISTICS ====================

    @Operation(summary = "Conta prenotazioni per stato")
    @ReadApiResponses
    @GetMapping("/count/{status}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_READ')")
    public ResponseEntity<Long> countByStatus(
            @AuthenticationPrincipal RUser user,
            @PathVariable GroupBookingStatus status) {
        
        return execute("count by status", () ->
            groupBookingService.countByStatus(user.getRestaurant().getId(), status));
    }

    @Operation(summary = "Totale PAX per data")
    @ReadApiResponses
    @GetMapping("/pax-count/{date}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_RESERVATION_READ')")
    public ResponseEntity<Integer> getTotalPaxForDate(
            @AuthenticationPrincipal RUser user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        return execute("get total pax", () ->
            groupBookingService.getTotalPaxForDate(user.getRestaurant().getId(), date));
    }

    // ==================== HELPER ====================

    private void validateOwnership(RUser user, GroupBooking booking) {
        if (!booking.getRestaurant().getId().equals(user.getRestaurant().getId())) {
            throw new IllegalArgumentException("Booking does not belong to this restaurant");
        }
    }

    // ==================== DTOs ====================

    public record CreateGroupBookingRequest(
        String eventName,
        String eventDescription,
        LocalDate eventDate,
        LocalTime eventTime,
        Integer totalPax,
        Integer adultsCount,
        Integer childrenCount,
        String contactName,
        String contactEmail,
        String contactPhone,
        String menuNotes,
        String internalNotes,
        BigDecimal depositRequired
    ) {}

    public record UpdateGroupBookingRequest(
        String eventName,
        String eventDescription,
        LocalDate eventDate,
        LocalTime eventTime,
        Integer totalPax,
        Integer adultsCount,
        Integer childrenCount,
        String contactName,
        String contactEmail,
        String contactPhone,
        String menuNotes,
        String internalNotes
    ) {}

    public record CancelRequest(String reason) {}

    public record DepositRequest(BigDecimal amount) {}

    public record AddDietaryNeedRequest(
        DietaryRequirement requirement,
        Integer paxCount,
        String notes,
        Boolean isCritical
    ) {}
}
