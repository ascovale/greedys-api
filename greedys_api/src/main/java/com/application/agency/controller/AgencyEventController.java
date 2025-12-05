package com.application.agency.controller;

import com.application.agency.persistence.model.user.AgencyUser;
import com.application.common.persistence.model.event.EventRSVP;
import com.application.common.persistence.model.event.RestaurantEvent;
import com.application.common.persistence.model.event.RestaurantEventType;
import com.application.common.service.event.RestaurantEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Agency Event Controller
 * Handles restaurant event operations for agency users (view and RSVP)
 */
@RestController
@RequestMapping("/agency/events")
@RequiredArgsConstructor
@Slf4j
public class AgencyEventController {

    private final RestaurantEventService eventService;

    // ==================== EVENT DISCOVERY ====================

    /**
     * Get available events
     */
    @GetMapping
    public ResponseEntity<Page<RestaurantEvent>> getAvailableEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RestaurantEvent> events = eventService.getAvailableEvents(page, size);
        return ResponseEntity.ok(events);
    }

    /**
     * Get featured events
     */
    @GetMapping("/featured")
    public ResponseEntity<List<RestaurantEvent>> getFeaturedEvents() {
        List<RestaurantEvent> events = eventService.getFeaturedEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * Get events by type
     */
    @GetMapping("/type/{eventType}")
    public ResponseEntity<Page<RestaurantEvent>> getEventsByType(
            @PathVariable RestaurantEventType eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RestaurantEvent> events = eventService.getEventsByType(eventType, page, size);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events for a specific restaurant
     */
    @GetMapping("/restaurants/{restaurantId}")
    public ResponseEntity<Page<RestaurantEvent>> getRestaurantEvents(
            @PathVariable Long restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RestaurantEvent> events = eventService.getRestaurantEvents(restaurantId, page, size);
        return ResponseEntity.ok(events);
    }

    /**
     * Get upcoming events for a restaurant
     */
    @GetMapping("/restaurants/{restaurantId}/upcoming")
    public ResponseEntity<List<RestaurantEvent>> getUpcomingRestaurantEvents(
            @PathVariable Long restaurantId) {
        List<RestaurantEvent> events = eventService.getUpcomingEvents(restaurantId);
        return ResponseEntity.ok(events);
    }

    /**
     * Get a specific event
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<RestaurantEvent> getEvent(@PathVariable Long eventId) {
        return eventService.getEvent(eventId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search events by date range
     */
    @GetMapping("/search")
    public ResponseEntity<Page<RestaurantEvent>> searchEvents(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RestaurantEvent> events = eventService.getEventsInRange(startDate, endDate, page, size);
        return ResponseEntity.ok(events);
    }

    /**
     * Search events by text
     */
    @GetMapping("/search/text")
    public ResponseEntity<Page<RestaurantEvent>> searchEventsByText(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RestaurantEvent> events = eventService.searchEvents(query, page, size);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events by date
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<RestaurantEvent>> getEventsByDate(@PathVariable LocalDate date) {
        List<RestaurantEvent> events = eventService.getEventsByDate(date);
        return ResponseEntity.ok(events);
    }

    // ==================== RSVP (Agency can RSVP for events) ====================

    /**
     * RSVP to an event
     */
    @PostMapping("/{eventId}/rsvp")
    public ResponseEntity<EventRSVP> rsvpToEvent(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long eventId,
            @RequestBody RsvpRequest request) {
        log.info("Agency user {} RSVPing to event {}", agencyUser.getId(), eventId);
        EventRSVP rsvp = eventService.rsvp(
                eventId, 
                agencyUser.getId(), 
                request.guestCount() != null ? request.guestCount() : 0,
                request.notes()
        );
        return ResponseEntity.ok(rsvp);
    }

    /**
     * Cancel RSVP
     */
    @DeleteMapping("/{eventId}/rsvp")
    public ResponseEntity<Void> cancelRsvp(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long eventId) {
        log.info("Agency user {} cancelling RSVP for event {}", agencyUser.getId(), eventId);
        eventService.cancelRsvp(eventId, agencyUser.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Get agency user's RSVP for an event
     */
    @GetMapping("/{eventId}/rsvp")
    public ResponseEntity<EventRSVP> getMyRsvp(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long eventId) {
        return eventService.getUserRsvp(eventId, agencyUser.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all agency user's upcoming events
     */
    @GetMapping("/my-events")
    public ResponseEntity<List<EventRSVP>> getMyUpcomingEvents(
            @AuthenticationPrincipal AgencyUser agencyUser) {
        List<EventRSVP> rsvps = eventService.getUserUpcomingEvents(agencyUser.getId());
        return ResponseEntity.ok(rsvps);
    }

    // ==================== VIEW TRACKING ====================

    /**
     * Record event view
     */
    @PostMapping("/{eventId}/view")
    public ResponseEntity<Void> recordView(@PathVariable Long eventId) {
        eventService.recordView(eventId);
        return ResponseEntity.ok().build();
    }

    // ==================== REQUEST DTOs ====================

    public record RsvpRequest(
            Integer guestCount,
            String notes
    ) {}
}
