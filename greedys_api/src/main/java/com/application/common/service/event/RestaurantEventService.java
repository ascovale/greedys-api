package com.application.common.service.event;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.domain.event.EventType;
import com.application.common.persistence.dao.EventOutboxDAO;
import com.application.common.persistence.dao.event.EventRSVPDAO;
import com.application.common.persistence.dao.event.RestaurantEventDAO;
import com.application.common.persistence.model.event.EventRSVP;
import com.application.common.persistence.model.event.EventStatus;
import com.application.common.persistence.model.event.RSVPStatus;
import com.application.common.persistence.model.event.RestaurantEvent;
import com.application.common.persistence.model.event.RestaurantEventType;
import com.application.common.persistence.model.notification.EventOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ RESTAURANT EVENT SERVICE
 * 
 * Service per la gestione degli eventi dei ristoranti.
 * 
 * FEATURES:
 * - Creazione/modifica/eliminazione eventi
 * - Gestione RSVP (partecipazione)
 * - Waitlist
 * - Check-in
 * - Reminder automatici
 * - Notifiche followers
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RestaurantEventService {

    private final RestaurantEventDAO eventDAO;
    private final EventRSVPDAO rsvpDAO;
    private final EventOutboxDAO eventOutboxDAO;
    private final ObjectMapper objectMapper;

    // ==================== EVENT MANAGEMENT ====================

    /**
     * Crea un nuovo evento
     */
    public RestaurantEvent createEvent(
            Long restaurantId,
            String title,
            String description,
            RestaurantEventType eventType,
            LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime,
            Integer maxCapacity
    ) {
        RestaurantEvent event = new RestaurantEvent();
        event.setRestaurantId(restaurantId);
        event.setTitle(title);
        event.setDescription(description);
        event.setEventType(eventType);
        event.setEventDate(eventDate);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setMaxCapacity(maxCapacity);
        event.setStatus(EventStatus.DRAFT);
        
        event = eventDAO.save(event);
        
        log.info("✅ Creato evento {} - '{}' per restaurant {}", 
            event.getId(), title, restaurantId);
        
        return event;
    }

    /**
     * Aggiorna un evento
     */
    public RestaurantEvent updateEvent(
            Long eventId,
            String title,
            String description,
            LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime,
            Integer maxCapacity
    ) {
        RestaurantEvent event = eventDAO.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento non trovato: " + eventId));
        
        if (title != null) event.setTitle(title);
        if (description != null) event.setDescription(description);
        if (eventDate != null) event.setEventDate(eventDate);
        if (startTime != null) event.setStartTime(startTime);
        if (endTime != null) event.setEndTime(endTime);
        if (maxCapacity != null) event.setMaxCapacity(maxCapacity);
        
        event = eventDAO.save(event);
        
        // Notifica partecipanti se evento pubblicato
        if (event.getStatus() == EventStatus.PUBLISHED) {
            notifyAttendees(event, EventType.EVENT_UPDATED);
        }
        
        return event;
    }

    /**
     * Pubblica un evento
     */
    public RestaurantEvent publishEvent(Long eventId) {
        RestaurantEvent event = eventDAO.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento non trovato: " + eventId));
        
        event.publish();
        event = eventDAO.save(event);
        
        // Notifica followers del ristorante
        triggerEventNotification(event, EventType.EVENT_CREATED);
        
        log.info("📢 Evento {} pubblicato", eventId);
        
        return event;
    }

    /**
     * Cancella un evento
     */
    public RestaurantEvent cancelEvent(Long eventId, String reason) {
        RestaurantEvent event = eventDAO.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento non trovato: " + eventId));
        
        event.cancel();
        event = eventDAO.save(event);
        
        // Notifica tutti i partecipanti
        notifyAttendeesWithMessage(event, EventType.EVENT_CANCELLED, reason);
        
        log.warn("❌ Evento {} cancellato: {}", eventId, reason);
        
        return event;
    }

    /**
     * Elimina un evento (soft delete)
     */
    public void deleteEvent(Long eventId) {
        RestaurantEvent event = eventDAO.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento non trovato: " + eventId));
        
        event.softDelete();
        eventDAO.save(event);
        
        log.info("🗑️ Evento {} eliminato", eventId);
    }

    /**
     * Ottieni evento per ID
     */
    @Transactional(readOnly = true)
    public Optional<RestaurantEvent> getEvent(Long eventId) {
        return eventDAO.findById(eventId);
    }

    /**
     * Registra visualizzazione
     */
    public void recordView(Long eventId) {
        eventDAO.incrementViews(eventId);
    }

    // ==================== LISTINGS ====================

    /**
     * Eventi di un ristorante
     */
    @Transactional(readOnly = true)
    public Page<RestaurantEvent> getRestaurantEvents(Long restaurantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return eventDAO.findByRestaurantId(restaurantId, pageable);
    }

    /**
     * Eventi futuri di un ristorante
     */
    @Transactional(readOnly = true)
    public List<RestaurantEvent> getUpcomingEvents(Long restaurantId) {
        return eventDAO.findUpcomingByRestaurantId(restaurantId, LocalDate.now());
    }

    /**
     * Eventi per data
     */
    @Transactional(readOnly = true)
    public List<RestaurantEvent> getEventsByDate(LocalDate date) {
        return eventDAO.findByEventDate(date);
    }

    /**
     * Eventi in un range di date
     */
    @Transactional(readOnly = true)
    public Page<RestaurantEvent> getEventsInRange(LocalDate start, LocalDate end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return eventDAO.findByDateRange(start, end, pageable);
    }

    /**
     * Eventi per tipo
     */
    @Transactional(readOnly = true)
    public Page<RestaurantEvent> getEventsByType(RestaurantEventType type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return eventDAO.findByEventType(type, LocalDate.now(), pageable);
    }

    /**
     * Eventi featured
     */
    @Transactional(readOnly = true)
    public List<RestaurantEvent> getFeaturedEvents() {
        return eventDAO.findFeatured(LocalDate.now());
    }

    /**
     * Eventi con posti disponibili
     */
    @Transactional(readOnly = true)
    public Page<RestaurantEvent> getAvailableEvents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return eventDAO.findAvailable(LocalDate.now(), pageable);
    }

    /**
     * Cerca eventi
     */
    @Transactional(readOnly = true)
    public Page<RestaurantEvent> searchEvents(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return eventDAO.search(query, pageable);
    }

    // ==================== RSVP MANAGEMENT ====================

    /**
     * Registra partecipazione a un evento (RSVP)
     */
    public EventRSVP rsvp(Long eventId, Long userId, int guestsCount, String notes) {
        RestaurantEvent event = eventDAO.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento non trovato: " + eventId));
        
        // Verifica se registrazione aperta
        if (!event.isRegistrationOpen()) {
            throw new IllegalStateException("Registrazioni chiuse per questo evento");
        }
        
        // Verifica se già registrato
        if (rsvpDAO.existsByEventIdAndUserId(eventId, userId)) {
            throw new IllegalStateException("Utente già registrato per questo evento");
        }
        
        int totalGuests = guestsCount + 1; // +1 per l'utente stesso
        
        // Verifica disponibilità
        EventRSVP rsvp = new EventRSVP();
        rsvp.setEvent(event);
        rsvp.setUserId(userId);
        rsvp.setGuestsCount(totalGuests);
        rsvp.setNotes(notes);
        
        if (event.hasAvailableSpots() && event.getAvailableSpots() >= totalGuests) {
            // C'è posto
            rsvp.setStatus(RSVPStatus.GOING);
            eventDAO.incrementAttendees(eventId, totalGuests);
        } else if (event.getAllowsWaitlist()) {
            // Metti in waitlist
            rsvp.setStatus(RSVPStatus.WAITLIST);
            rsvp.setWaitlistPosition(rsvpDAO.getNextWaitlistPosition(eventId));
        } else {
            throw new IllegalStateException("Evento al completo e waitlist non disponibile");
        }
        
        rsvp = rsvpDAO.save(rsvp);
        
        // Trigger evento notifica
        triggerRsvpNotification(event, rsvp);
        
        log.info("✅ RSVP {} - User {} → Event {} ({})", 
            rsvp.getId(), userId, eventId, rsvp.getStatus());
        
        return rsvp;
    }

    /**
     * Cancella RSVP
     */
    public void cancelRsvp(Long eventId, Long userId) {
        EventRSVP rsvp = rsvpDAO.findByEventIdAndUserId(eventId, userId)
            .orElseThrow(() -> new IllegalArgumentException("RSVP non trovato"));
        
        RSVPStatus previousStatus = rsvp.getStatus();
        rsvp.setStatus(RSVPStatus.CANCELLED);
        rsvpDAO.save(rsvp);
        
        // Se era confermato, libera posti
        if (previousStatus == RSVPStatus.GOING || previousStatus == RSVPStatus.CONFIRMED) {
            eventDAO.decrementAttendees(eventId, rsvp.getGuestsCount());
            
            // Promuovi dalla waitlist
            promoteFromWaitlist(eventId);
        }
        
        log.info("❌ RSVP cancellato - User {} @ Event {}", userId, eventId);
    }

    /**
     * Conferma partecipazione (per eventi a pagamento)
     */
    public EventRSVP confirmRsvp(Long rsvpId) {
        EventRSVP rsvp = rsvpDAO.findById(rsvpId)
            .orElseThrow(() -> new IllegalArgumentException("RSVP non trovato"));
        
        rsvp.setStatus(RSVPStatus.CONFIRMED);
        rsvp.setConfirmedAt(Instant.now());
        return rsvpDAO.save(rsvp);
    }

    /**
     * Ottieni RSVP di un utente per un evento
     */
    @Transactional(readOnly = true)
    public Optional<EventRSVP> getUserRsvp(Long eventId, Long userId) {
        return rsvpDAO.findByEventIdAndUserId(eventId, userId);
    }

    /**
     * Lista partecipanti di un evento
     */
    @Transactional(readOnly = true)
    public Page<EventRSVP> getEventAttendees(Long eventId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return rsvpDAO.findByEventId(eventId, pageable);
    }

    /**
     * Eventi a cui un utente partecipa
     */
    @Transactional(readOnly = true)
    public List<EventRSVP> getUserUpcomingEvents(Long userId) {
        return rsvpDAO.findUpcomingByUserId(userId, LocalDate.now());
    }

    /**
     * Promuovi dalla waitlist
     */
    private void promoteFromWaitlist(Long eventId) {
        RestaurantEvent event = eventDAO.findById(eventId).orElse(null);
        if (event == null || !event.hasAvailableSpots()) return;
        
        List<EventRSVP> waitlist = rsvpDAO.findWaitlistByEventId(eventId);
        
        for (EventRSVP waiting : waitlist) {
            if (event.getAvailableSpots() >= waiting.getGuestsCount()) {
                waiting.setStatus(RSVPStatus.GOING);
                waiting.setPromotedFromWaitlistAt(Instant.now());
                rsvpDAO.save(waiting);
                
                eventDAO.incrementAttendees(eventId, waiting.getGuestsCount());
                
                // Notifica utente promosso
                triggerWaitlistPromotionNotification(event, waiting);
                
                log.info("✅ User {} promosso dalla waitlist per evento {}", 
                    waiting.getUserId(), eventId);
                
                if (!event.hasAvailableSpots()) break;
            }
        }
    }

    // ==================== CHECK-IN ====================

    /**
     * Check-in partecipante
     */
    public EventRSVP checkIn(Long rsvpId, Long checkedInBy) {
        EventRSVP rsvp = rsvpDAO.findById(rsvpId)
            .orElseThrow(() -> new IllegalArgumentException("RSVP non trovato"));
        
        rsvp.setCheckedIn(true);
        rsvp.setCheckedInAt(Instant.now());
        rsvp.setCheckedInBy(checkedInBy);
        
        return rsvpDAO.save(rsvp);
    }

    /**
     * Check-in per user ID
     */
    public EventRSVP checkInByUserId(Long eventId, Long userId, Long checkedInBy) {
        EventRSVP rsvp = rsvpDAO.findByEventIdAndUserId(eventId, userId)
            .orElseThrow(() -> new IllegalArgumentException("RSVP non trovato per questo utente"));
        
        return checkIn(rsvp.getId(), checkedInBy);
    }

    /**
     * Lista check-in per un evento
     */
    @Transactional(readOnly = true)
    public List<EventRSVP> getCheckedInAttendees(Long eventId) {
        return rsvpDAO.findCheckedIn(eventId);
    }

    // ==================== REMINDERS ====================

    /**
     * Invia reminder a tutti i partecipanti di un evento
     */
    public void sendReminders(Long eventId) {
        RestaurantEvent event = eventDAO.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento non trovato"));
        
        List<EventRSVP> needReminder = rsvpDAO.findNeedingReminder(eventId);
        
        for (EventRSVP rsvp : needReminder) {
            triggerReminderNotification(event, rsvp);
            rsvpDAO.markReminderSent(rsvp.getId(), Instant.now());
        }
        
        log.info("📧 Inviati {} reminder per evento {}", needReminder.size(), eventId);
    }

    /**
     * Trova eventi che richiedono reminder (per scheduler)
     */
    @Transactional(readOnly = true)
    public List<RestaurantEvent> getEventsNeedingReminder(LocalDate reminderDate) {
        return eventDAO.findEventsOnDate(reminderDate);
    }

    // ==================== MAINTENANCE ====================

    /**
     * Completa eventi passati (per scheduler)
     */
    public void completeExpiredEvents() {
        List<RestaurantEvent> expired = eventDAO.findPastNotCompleted(LocalDate.now());
        
        for (RestaurantEvent event : expired) {
            event.setStatus(EventStatus.COMPLETED);
            eventDAO.save(event);
        }
        
        if (!expired.isEmpty()) {
            log.info("✅ {} eventi marcati come completati", expired.size());
        }
    }

    // ==================== NOTIFICATIONS ====================

    private void triggerEventNotification(RestaurantEvent event, EventType eventType) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "eventId", event.getId(),
                "restaurantId", event.getRestaurantId(),
                "title", event.getTitle(),
                "eventDate", event.getEventDate().toString(),
                "startTime", event.getStartTime().toString(),
                "eventType", event.getEventType().name()
            ));
            
            EventOutbox outbox = EventOutbox.builder()
                .eventId("restaurant_event_" + event.getId() + "_" + System.currentTimeMillis())
                .eventType(eventType.name())
                .aggregateType("RestaurantEvent")
                .aggregateId(event.getId())
                .payload(payload)
                .build();
            
            eventOutboxDAO.save(outbox);
        } catch (Exception e) {
            log.error("Errore creazione evento notifica: {}", e.getMessage());
        }
    }

    private void notifyAttendees(RestaurantEvent event, EventType eventType) {
        try {
            List<EventRSVP> attendees = rsvpDAO.findByEventIdAndStatus(event.getId(), RSVPStatus.GOING);
            attendees.addAll(rsvpDAO.findByEventIdAndStatus(event.getId(), RSVPStatus.CONFIRMED));
            
            for (EventRSVP rsvp : attendees) {
                String payload = objectMapper.writeValueAsString(Map.of(
                    "eventId", event.getId(),
                    "title", event.getTitle(),
                    "eventDate", event.getEventDate().toString(),
                    "userId", rsvp.getUserId()
                ));
                
                EventOutbox outbox = EventOutbox.builder()
                    .eventId("event_notify_" + event.getId() + "_" + rsvp.getUserId())
                    .eventType(eventType.name())
                    .aggregateType("RestaurantEvent")
                    .aggregateId(event.getId())
                    .payload(payload)
                    .build();
                
                eventOutboxDAO.save(outbox);
            }
        } catch (Exception e) {
            log.error("Errore notifica partecipanti: {}", e.getMessage());
        }
    }

    private void notifyAttendeesWithMessage(RestaurantEvent event, EventType eventType, String message) {
        try {
            List<EventRSVP> attendees = rsvpDAO.findByEventIdAndStatus(event.getId(), RSVPStatus.GOING);
            attendees.addAll(rsvpDAO.findByEventIdAndStatus(event.getId(), RSVPStatus.CONFIRMED));
            attendees.addAll(rsvpDAO.findByEventIdAndStatus(event.getId(), RSVPStatus.WAITLIST));
            
            for (EventRSVP rsvp : attendees) {
                String payload = objectMapper.writeValueAsString(Map.of(
                    "eventId", event.getId(),
                    "title", event.getTitle(),
                    "eventDate", event.getEventDate().toString(),
                    "userId", rsvp.getUserId(),
                    "message", message != null ? message : ""
                ));
                
                EventOutbox outbox = EventOutbox.builder()
                    .eventId("event_cancel_" + event.getId() + "_" + rsvp.getUserId())
                    .eventType(eventType.name())
                    .aggregateType("RestaurantEvent")
                    .aggregateId(event.getId())
                    .payload(payload)
                    .build();
                
                eventOutboxDAO.save(outbox);
            }
        } catch (Exception e) {
            log.error("Errore notifica cancellazione: {}", e.getMessage());
        }
    }

    private void triggerRsvpNotification(RestaurantEvent event, EventRSVP rsvp) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "eventId", event.getId(),
                "restaurantId", event.getRestaurantId(),
                "rsvpId", rsvp.getId(),
                "userId", rsvp.getUserId(),
                "status", rsvp.getStatus().name(),
                "guestsCount", rsvp.getGuestsCount()
            ));
            
            EventOutbox outbox = EventOutbox.builder()
                .eventId("event_rsvp_" + rsvp.getId())
                .eventType(EventType.EVENT_NEW_RSVP.name())
                .aggregateType("EventRSVP")
                .aggregateId(rsvp.getId())
                .payload(payload)
                .build();
            
            eventOutboxDAO.save(outbox);
        } catch (Exception e) {
            log.error("Errore creazione evento RSVP: {}", e.getMessage());
        }
    }

    private void triggerWaitlistPromotionNotification(RestaurantEvent event, EventRSVP rsvp) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "eventId", event.getId(),
                "title", event.getTitle(),
                "eventDate", event.getEventDate().toString(),
                "userId", rsvp.getUserId(),
                "rsvpId", rsvp.getId()
            ));
            
            EventOutbox outbox = EventOutbox.builder()
                .eventId("waitlist_promo_" + rsvp.getId())
                .eventType(EventType.EVENT_RSVP_STATUS_CHANGED.name())
                .aggregateType("EventRSVP")
                .aggregateId(rsvp.getId())
                .payload(payload)
                .build();
            
            eventOutboxDAO.save(outbox);
        } catch (Exception e) {
            log.error("Errore notifica promozione waitlist: {}", e.getMessage());
        }
    }

    private void triggerReminderNotification(RestaurantEvent event, EventRSVP rsvp) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "eventId", event.getId(),
                "title", event.getTitle(),
                "eventDate", event.getEventDate().toString(),
                "startTime", event.getStartTime().toString(),
                "userId", rsvp.getUserId()
            ));
            
            EventOutbox outbox = EventOutbox.builder()
                .eventId("event_reminder_" + event.getId() + "_" + rsvp.getUserId())
                .eventType(EventType.EVENT_REMINDER.name())
                .aggregateType("RestaurantEvent")
                .aggregateId(event.getId())
                .payload(payload)
                .build();
            
            eventOutboxDAO.save(outbox);
        } catch (Exception e) {
            log.error("Errore creazione reminder: {}", e.getMessage());
        }
    }
}
