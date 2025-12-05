package com.application.restaurant.controller;

import com.application.common.persistence.model.event.EventRSVP;
import com.application.common.persistence.model.event.RestaurantEvent;
import com.application.common.persistence.model.event.RestaurantEventType;
import com.application.common.service.event.RestaurantEventService;
import com.application.restaurant.persistence.model.user.RUser;
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
 * Restaurant Event Controller
 * Handles restaurant event management operations
 */
@RestController
@RequestMapping("/restaurant/events")
@RequiredArgsConstructor
@Slf4j
public class RestaurantEventController {

    private final RestaurantEventService eventService;

    // ==================== EVENT CREATION ====================

    /**
     * Create a new event
     */
    @PostMapping
    public ResponseEntity<RestaurantEvent> createEvent(
            @AuthenticationPrincipal RUser restaurantUser,
            @RequestBody CreateEventRequest request) {
        log.info("Restaurant {} creating event: {}", restaurantUser.getRestaurant().getId(), request.title());
        RestaurantEvent event = eventService.createEvent(
                restaurantUser.getRestaurant().getId(),
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

    // ==================== EVENT MANAGEMENT ====================

    /**
     * Update an event
     */
    @PutMapping("/{eventId}")
    public ResponseEntity<RestaurantEvent> updateEvent(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long eventId,
            @RequestBody UpdateEventRequest request) {
        return eventService.getEvent(eventId)
                .filter(event -> event.getRestaurantId().equals(restaurantUser.getRestaurant().getId()))
                .map(event -> {
                    log.info("Restaurant {} updating event {}", restaurantUser.getRestaurant().getId(), eventId);
                    RestaurantEvent updated = eventService.updateEvent(
                            eventId,
                            request.title(),
                            request.description(),
                            request.eventDate(),
                            request.startTime(),
                            request.endTime(),
                            request.maxCapacity()
                    );
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Publish an event
     */
    @PostMapping("/{eventId}/publish")
    public ResponseEntity<RestaurantEvent> publishEvent(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long eventId) {
        return eventService.getEvent(eventId)
                .filter(event -> event.getRestaurantId().equals(restaurantUser.getRestaurant().getId()))
                .map(event -> {
                    log.info("Restaurant {} publishing event {}", restaurantUser.getRestaurant().getId(), eventId);
                    RestaurantEvent published = eventService.publishEvent(eventId);
                    return ResponseEntity.ok(published);
                })
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Cancel an event
     */
    @PostMapping("/{eventId}/cancel")
    public ResponseEntity<RestaurantEvent> cancelEvent(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long eventId,
            @RequestBody(required = false) CancelEventRequest request) {
        return eventService.getEvent(eventId)
                .filter(event -> event.getRestaurantId().equals(restaurantUser.getRestaurant().getId()))
                .map(event -> {
                    log.info("Restaurant {} cancelling event {}", restaurantUser.getRestaurant().getId(), eventId);
                    RestaurantEvent cancelled = eventService.cancelEvent(
                            eventId, 
                            request != null ? request.reason() : "Evento cancellato"
                    );
                    return ResponseEntity.ok(cancelled);
                })
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Delete an event
     */
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long eventId) {
        return eventService.getEvent(eventId)
                .filter(event -> event.getRestaurantId().equals(restaurantUser.getRestaurant().getId()))
                .map(event -> {
                    log.info("Restaurant {} deleting event {}", restaurantUser.getRestaurant().getId(), eventId);
                    eventService.deleteEvent(eventId);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.status(403).build());
    }

    // ==================== EVENT RETRIEVAL ====================

    /**
     * Get all restaurant's events
     */
    @GetMapping
    public ResponseEntity<Page<RestaurantEvent>> getMyEvents(
            @AuthenticationPrincipal RUser restaurantUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RestaurantEvent> events = eventService.getRestaurantEvents(
                restaurantUser.getRestaurant().getId(), page, size);
        return ResponseEntity.ok(events);
    }

    /**
     * Get upcoming events for the restaurant
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<RestaurantEvent>> getUpcomingEvents(
            @AuthenticationPrincipal RUser restaurantUser) {
        List<RestaurantEvent> events = eventService.getUpcomingEvents(restaurantUser.getRestaurant().getId());
        return ResponseEntity.ok(events);
    }

    /**
     * Get a specific event
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<RestaurantEvent> getEvent(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long eventId) {
        return eventService.getEvent(eventId)
                .filter(event -> event.getRestaurantId().equals(restaurantUser.getRestaurant().getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get events by date
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<RestaurantEvent>> getEventsByDate(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable LocalDate date) {
        // Filter by restaurant
        List<RestaurantEvent> events = eventService.getEventsByDate(date).stream()
                .filter(e -> e.getRestaurantId().equals(restaurantUser.getRestaurant().getId()))
                .toList();
        return ResponseEntity.ok(events);
    }

    // ==================== RSVP MANAGEMENT ====================

    /**
     * Get attendees for an event
     */
    @GetMapping("/{eventId}/attendees")
    public ResponseEntity<Page<EventRSVP>> getEventAttendees(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return eventService.getEvent(eventId)
                .filter(event -> event.getRestaurantId().equals(restaurantUser.getRestaurant().getId()))
                .map(event -> {
                    Page<EventRSVP> attendees = eventService.getEventAttendees(eventId, page, size);
                    return ResponseEntity.ok(attendees);
                })
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Get checked-in attendees
     */
    @GetMapping("/{eventId}/checked-in")
    public ResponseEntity<List<EventRSVP>> getCheckedInAttendees(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long eventId) {
        return eventService.getEvent(eventId)
                .filter(event -> event.getRestaurantId().equals(restaurantUser.getRestaurant().getId()))
                .map(event -> {
                    List<EventRSVP> checkedIn = eventService.getCheckedInAttendees(eventId);
                    return ResponseEntity.ok(checkedIn);
                })
                .orElse(ResponseEntity.status(403).build());
    }

    // ==================== CHECK-IN ====================

    /**
     * Check-in an attendee by RSVP ID
     */
    @PostMapping("/{eventId}/check-in/rsvp/{rsvpId}")
    public ResponseEntity<EventRSVP> checkInByRsvp(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long eventId,
            @PathVariable Long rsvpId) {
        return eventService.getEvent(eventId)
                .filter(event -> event.getRestaurantId().equals(restaurantUser.getRestaurant().getId()))
                .map(event -> {
                    log.info("Restaurant {} checking in RSVP {} for event {}", 
                            restaurantUser.getRestaurant().getId(), rsvpId, eventId);
                    EventRSVP rsvp = eventService.checkIn(rsvpId, restaurantUser.getId());
                    return ResponseEntity.ok(rsvp);
                })
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Check-in an attendee by user ID
     */
    @PostMapping("/{eventId}/check-in/user/{userId}")
    public ResponseEntity<EventRSVP> checkInByUser(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long eventId,
            @PathVariable Long userId) {
        return eventService.getEvent(eventId)
                .filter(event -> event.getRestaurantId().equals(restaurantUser.getRestaurant().getId()))
                .map(event -> {
                    log.info("Restaurant {} checking in user {} for event {}", 
                            restaurantUser.getRestaurant().getId(), userId, eventId);
                    EventRSVP rsvp = eventService.checkInByUserId(eventId, userId, restaurantUser.getId());
                    return ResponseEntity.ok(rsvp);
                })
                .orElse(ResponseEntity.status(403).build());
    }

    // ==================== REMINDERS ====================

    /**
     * Send reminders to attendees
     */
    @PostMapping("/{eventId}/send-reminders")
    public ResponseEntity<Void> sendReminders(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long eventId) {
        return eventService.getEvent(eventId)
                .filter(event -> event.getRestaurantId().equals(restaurantUser.getRestaurant().getId()))
                .map(event -> {
                    log.info("Restaurant {} sending reminders for event {}", 
                            restaurantUser.getRestaurant().getId(), eventId);
                    eventService.sendReminders(eventId);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.status(403).build());
    }

    // ==================== REQUEST DTOs ====================

    public record CreateEventRequest(
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
