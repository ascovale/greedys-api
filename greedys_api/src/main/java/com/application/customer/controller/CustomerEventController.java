package com.application.customer.controller;

import com.application.common.persistence.model.event.EventRSVP;
import com.application.common.persistence.model.event.RestaurantEvent;
import com.application.common.persistence.model.event.RestaurantEventType;
import com.application.common.service.event.RestaurantEventService;
import com.application.customer.persistence.model.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Customer Event Controller
 * Handles restaurant event operations for customers (view events, RSVP, check-in)
 */
@RestController
@RequestMapping("/customer/events")
@RequiredArgsConstructor
@Slf4j
public class CustomerEventController {

    private final RestaurantEventService eventService;

    // ==================== EVENT DISCOVERY ====================

    /**
     * Get events with available spots
     */
    @GetMapping
    public ResponseEntity<Page<RestaurantEvent>> getAvailableEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Fetching available events");
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
     * Get upcoming events for a specific restaurant
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
     * Search events by query
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
     * Get events on a specific date
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<RestaurantEvent>> getEventsByDate(
            @PathVariable LocalDate date) {
        List<RestaurantEvent> events = eventService.getEventsByDate(date);
        return ResponseEntity.ok(events);
    }

    // ==================== RSVP MANAGEMENT ====================

    /**
     * RSVP to an event
     */
    @PostMapping("/{eventId}/rsvp")
    public ResponseEntity<EventRSVP> rsvpToEvent(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long eventId,
            @RequestBody RsvpRequest request) {
        log.info("Customer {} RSVPing to event {}", customer.getId(), eventId);
        EventRSVP rsvp = eventService.rsvp(
                eventId, 
                customer.getId(), 
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
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long eventId) {
        log.info("Customer {} cancelling RSVP for event {}", customer.getId(), eventId);
        eventService.cancelRsvp(eventId, customer.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Get customer's RSVP for an event
     */
    @GetMapping("/{eventId}/rsvp")
    public ResponseEntity<EventRSVP> getMyRsvp(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long eventId) {
        return eventService.getUserRsvp(eventId, customer.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all customer's upcoming events (RSVPs)
     */
    @GetMapping("/my-events")
    public ResponseEntity<List<EventRSVP>> getMyUpcomingEvents(
            @AuthenticationPrincipal Customer customer) {
        List<EventRSVP> rsvps = eventService.getUserUpcomingEvents(customer.getId());
        return ResponseEntity.ok(rsvps);
    }

    // ==================== VIEW TRACKING ====================

    /**
     * Record an event view
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
