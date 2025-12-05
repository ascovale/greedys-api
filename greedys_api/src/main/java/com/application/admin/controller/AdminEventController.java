package com.application.admin.controller;

import com.application.admin.persistence.model.Admin;
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
import java.time.LocalTime;
import java.util.List;

/**
 * Admin Event Controller
 * Handles restaurant event management for admins - full system access
 */
@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Slf4j
public class AdminEventController {

    private final RestaurantEventService eventService;

    // ==================== EVENT RETRIEVAL (FULL ACCESS) ====================

    /**
     * Get all events (paginated)
     */
    @GetMapping
    public ResponseEntity<Page<RestaurantEvent>> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RestaurantEvent> events = eventService.getAvailableEvents(page, size);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events for a specific restaurant
     */
    @GetMapping("/restaurant/{restaurantId}")
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
    @GetMapping("/restaurant/{restaurantId}/upcoming")
    public ResponseEntity<List<RestaurantEvent>> getUpcomingEvents(@PathVariable Long restaurantId) {
        List<RestaurantEvent> events = eventService.getUpcomingEvents(restaurantId);
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
     * Get events in date range
     */
    @GetMapping("/range")
    public ResponseEntity<Page<RestaurantEvent>> getEventsInRange(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RestaurantEvent> events = eventService.getEventsInRange(start, end, page, size);
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

    /**
     * Search events
     */
    @GetMapping("/search")
    public ResponseEntity<Page<RestaurantEvent>> searchEvents(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RestaurantEvent> events = eventService.searchEvents(query, page, size);
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

    // ==================== EVENT MANAGEMENT (ADMIN CAN MANAGE ANY) ====================

    /**
     * Create an event for any restaurant
     */
    @PostMapping
    public ResponseEntity<RestaurantEvent> createEvent(
            @AuthenticationPrincipal Admin admin,
            @RequestBody CreateEventRequest request) {
        log.info("Admin {} creating event for restaurant {}: {}", 
                admin.getId(), request.restaurantId(), request.title());
        RestaurantEvent event = eventService.createEvent(
                request.restaurantId(),
                request.title(),
                request.description(),
                request.eventType(),
                request.eventDate(),
                request.startTime(),
                request.endTime(),
                request.maxCapacity()
        );
        return ResponseEntity.ok(event);
    }

    /**
     * Update any event
     */
    @PutMapping("/{eventId}")
    public ResponseEntity<RestaurantEvent> updateEvent(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long eventId,
            @RequestBody UpdateEventRequest request) {
        log.info("Admin {} updating event {}", admin.getId(), eventId);
        RestaurantEvent event = eventService.updateEvent(
                eventId,
                request.title(),
                request.description(),
                request.eventDate(),
                request.startTime(),
                request.endTime(),
                request.maxCapacity()
        );
        return ResponseEntity.ok(event);
    }

    /**
     * Publish any event
     */
    @PostMapping("/{eventId}/publish")
    public ResponseEntity<RestaurantEvent> publishEvent(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long eventId) {
        log.info("Admin {} publishing event {}", admin.getId(), eventId);
        RestaurantEvent event = eventService.publishEvent(eventId);
        return ResponseEntity.ok(event);
    }

    /**
     * Cancel any event
     */
    @PostMapping("/{eventId}/cancel")
    public ResponseEntity<RestaurantEvent> cancelEvent(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long eventId,
            @RequestBody(required = false) CancelEventRequest request) {
        log.warn("Admin {} cancelling event {}", admin.getId(), eventId);
        RestaurantEvent event = eventService.cancelEvent(
                eventId, 
                request != null ? request.reason() : "Cancellato dall'amministratore"
        );
        return ResponseEntity.ok(event);
    }

    /**
     * Delete any event
     */
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long eventId) {
        log.warn("Admin {} deleting event {}", admin.getId(), eventId);
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }

    // ==================== RSVP MANAGEMENT ====================

    /**
     * Get attendees for any event
     */
    @GetMapping("/{eventId}/attendees")
    public ResponseEntity<Page<EventRSVP>> getEventAttendees(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<EventRSVP> attendees = eventService.getEventAttendees(eventId, page, size);
        return ResponseEntity.ok(attendees);
    }

    /**
     * Get checked-in attendees
     */
    @GetMapping("/{eventId}/checked-in")
    public ResponseEntity<List<EventRSVP>> getCheckedInAttendees(@PathVariable Long eventId) {
        List<EventRSVP> checkedIn = eventService.getCheckedInAttendees(eventId);
        return ResponseEntity.ok(checkedIn);
    }

    /**
     * Check-in an attendee
     */
    @PostMapping("/{eventId}/check-in/rsvp/{rsvpId}")
    public ResponseEntity<EventRSVP> checkIn(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long eventId,
            @PathVariable Long rsvpId) {
        log.info("Admin {} checking in RSVP {} for event {}", admin.getId(), rsvpId, eventId);
        EventRSVP rsvp = eventService.checkIn(rsvpId, admin.getId());
        return ResponseEntity.ok(rsvp);
    }

    /**
     * Check-in by user ID
     */
    @PostMapping("/{eventId}/check-in/user/{userId}")
    public ResponseEntity<EventRSVP> checkInByUser(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long eventId,
            @PathVariable Long userId) {
        log.info("Admin {} checking in user {} for event {}", admin.getId(), userId, eventId);
        EventRSVP rsvp = eventService.checkInByUserId(eventId, userId, admin.getId());
        return ResponseEntity.ok(rsvp);
    }

    // ==================== REMINDERS ====================

    /**
     * Send reminders for an event
     */
    @PostMapping("/{eventId}/send-reminders")
    public ResponseEntity<Void> sendReminders(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long eventId) {
        log.info("Admin {} sending reminders for event {}", admin.getId(), eventId);
        eventService.sendReminders(eventId);
        return ResponseEntity.ok().build();
    }

    // ==================== MAINTENANCE ====================

    /**
     * Complete expired events manually
     */
    @PostMapping("/maintenance/complete-expired")
    public ResponseEntity<Void> completeExpiredEvents(@AuthenticationPrincipal Admin admin) {
        log.info("Admin {} triggering completion of expired events", admin.getId());
        eventService.completeExpiredEvents();
        return ResponseEntity.ok().build();
    }

    // ==================== REQUEST DTOs ====================

    public record CreateEventRequest(
            Long restaurantId,
            String title,
            String description,
            RestaurantEventType eventType,
            LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime,
            Integer maxCapacity
    ) {}

    public record UpdateEventRequest(
            String title,
            String description,
            LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime,
            Integer maxCapacity
    ) {}

    public record CancelEventRequest(String reason) {}
}
