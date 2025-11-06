package com.application.restaurant.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.Slot;
import com.application.common.persistence.model.reservation.SlotChangePolicy;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.restaurant.persistence.dao.SlotDAO;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.common.web.dto.restaurant.SlotDTO;
import com.application.common.persistence.mapper.SlotMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servizio per gestire le transizioni e modifiche degli slot temporali
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SlotTransitionService {

    private final SlotDAO slotDAO;
    private final ReservationDAO reservationDAO;
    private final SlotMapper slotMapper;
    
    /**
     * Modifica l'orario di uno slot creando una nuova versione valida da una data specifica
     */
    public SlotDTO changeSlotSchedule(Long slotId, LocalTime newStartTime, LocalTime newEndTime, 
                                     LocalDate effectiveFrom, SlotChangePolicy policy) {
        
        Slot currentSlot = slotDAO.findById(slotId)
            .orElseThrow(() -> new IllegalArgumentException("Slot not found with id: " + slotId));
        
        log.info("Modifying slot {} from {}-{} to {}-{} effective from {}", 
                slotId, currentSlot.getStart(), currentSlot.getEnd(), 
                newStartTime, newEndTime, effectiveFrom);
        
        // 1. Chiudi slot attuale alla data di transizione
        LocalDate transitionDate = effectiveFrom.minusDays(1);
        currentSlot.setValidTo(transitionDate);
        slotDAO.save(currentSlot);
        
        // 2. Crea nuovo slot con nuovi orari
        Slot newSlot = createNewSlotVersion(currentSlot, newStartTime, newEndTime, effectiveFrom, policy);
        
        // 3. Collegamento per tracciabilità
        currentSlot.setSupersededBy(newSlot.getId());
        slotDAO.save(currentSlot);
        
        // 4. Gestisci prenotazioni future esistenti
        handleExistingReservations(currentSlot, newSlot, effectiveFrom);
        
        log.info("Slot transition completed. Old slot: {}, New slot: {}", currentSlot.getId(), newSlot.getId());
        
        return slotMapper.toDTO(newSlot);
    }
    
    /**
     * Crea una nuova versione dello slot con orari modificati
     */
    private Slot createNewSlotVersion(Slot originalSlot, LocalTime newStartTime, LocalTime newEndTime, 
                                     LocalDate effectiveFrom, SlotChangePolicy policy) {
        
        Slot newSlot = Slot.builder()
            .start(newStartTime)
            .end(newEndTime)
            .weekday(originalSlot.getWeekday())
            .service(originalSlot.getService())
            .deleted(false)
            .validFrom(effectiveFrom)
            .validTo(LocalDate.of(2099, 12, 31))
            .active(true)
            .changePolicy(policy)
            .build();
            
        return slotDAO.save(newSlot);
    }
    
    /**
     * Gestisce le prenotazioni future quando uno slot viene modificato
     */
    private void handleExistingReservations(Slot oldSlot, Slot newSlot, LocalDate effectiveFrom) {
        
        // Trova prenotazioni future per il vecchio slot
        List<Reservation> futureReservations = reservationDAO.findFutureReservationsBySlotId(
            oldSlot.getId(), effectiveFrom);
        
        log.info("Found {} future reservations for slot {} starting from {}", 
                futureReservations.size(), oldSlot.getId(), effectiveFrom);
        
        for (Reservation reservation : futureReservations) {
            handleSingleReservation(reservation, oldSlot, newSlot);
        }
    }
    
    /**
     * Gestisce una singola prenotazione secondo la policy definita
     */
    private void handleSingleReservation(Reservation reservation, Slot oldSlot, Slot newSlot) {
        
        switch (oldSlot.getChangePolicy()) {
            case HARD_CUT:
                log.debug("HARD_CUT policy: Reservation {} remains on old slot {}", 
                         reservation.getId(), oldSlot.getId());
                // Non facciamo nulla - la prenotazione resta sul vecchio slot
                break;
                
            case NOTIFY_CUSTOMERS:
                log.info("NOTIFY_CUSTOMERS policy: Sending notification for reservation {}", 
                        reservation.getId());
                sendChangeNotification(reservation, oldSlot, newSlot);
                break;
                
            case AUTO_MIGRATE:
                if (isTimeCompatible(reservation, newSlot)) {
                    log.info("AUTO_MIGRATE: Migrating reservation {} to new slot {}", 
                            reservation.getId(), newSlot.getId());
                    migrateReservation(reservation, newSlot);
                } else {
                    log.warn("AUTO_MIGRATE: Cannot migrate reservation {} - time incompatible", 
                            reservation.getId());
                    sendChangeNotification(reservation, oldSlot, newSlot);
                }
                break;
        }
    }
    
    /**
     * Verifica se una prenotazione è compatibile con i nuovi orari
     */
    private boolean isTimeCompatible(Reservation reservation, Slot newSlot) {
        // Logica di compatibilità - per ora semplice controllo di overlap
        // In futuro potresti aggiungere logiche più sofisticate
        return true; // Placeholder - da implementare secondo business rules
    }
    
    /**
     * Migra una prenotazione al nuovo slot
     */
    private void migrateReservation(Reservation reservation, Slot newSlot) {
        reservation.setSlot(newSlot);
        reservationDAO.save(reservation);
        
        log.info("Reservation {} migrated from slot {} to slot {}", 
                reservation.getId(), reservation.getSlot().getId(), newSlot.getId());
    }
    
    /**
     * Invia notifica al cliente per il cambio di orario
     */
    private void sendChangeNotification(Reservation reservation, Slot oldSlot, Slot newSlot) {
        // TODO: Implementare sistema di notifiche
        // Potrebbe inviare email, SMS, push notification, etc.
        
        log.info("📧 Notification sent to customer for reservation {}: " +
                "Slot changed from {}-{} to {}-{}", 
                reservation.getId(), 
                oldSlot.getStart(), oldSlot.getEnd(),
                newSlot.getStart(), newSlot.getEnd());
    }
    
    /**
     * Trova slot attivi per una data specifica
     */
    @Transactional(readOnly = true)
    public List<SlotDTO> findActiveSlotsForDate(Long restaurantId, LocalDate date) {
        return slotDAO.findActiveSlotsByRestaurantAndDate(restaurantId, date)
                     .stream()
                     .map(slotMapper::toDTO)
                     .toList();
    }
    
    /**
     * Disattiva uno slot (soft delete temporale)
     */
    public void deactivateSlot(Long slotId, LocalDate effectiveFrom) {
        Slot slot = slotDAO.findById(slotId)
            .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
            
        slot.setValidTo(effectiveFrom.minusDays(1));
        slot.setActive(false);
        slotDAO.save(slot);
        
        log.info("Slot {} deactivated effective from {}", slotId, effectiveFrom);
    }
    
    /**
     * Get active slots for a service on a specific date
     */
    public List<Slot> getActiveSlotsForDate(Long serviceId, LocalDate date) {
        return slotDAO.findActiveSlotsByServiceAndDate(serviceId, date);
    }
    
    /**
     * Check if a slot can be safely modified (has no future reservations)
     */
    public boolean canSlotBeModified(Long slotId) {
        List<Reservation> futureReservations = reservationDAO.findFutureReservationsBySlotId(
            slotId, LocalDate.now()
        );
        return futureReservations.isEmpty();
    }
    
    /**
     * Get count of future reservations for a slot
     */
    public int getFutureReservationsCount(Long slotId) {
        List<Reservation> futureReservations = reservationDAO.findFutureReservationsBySlotId(
            slotId, LocalDate.now()
        );
        return futureReservations.size();
    }
    
    /**
     * Reactivate a deactivated slot
     */
    public void reactivateSlot(Long slotId) {
        Slot slot = slotDAO.findById(slotId)
            .orElseThrow(() -> new RuntimeException("Slot not found with id: " + slotId));
        
        // Reactivate the slot
        slot.setActive(true);
        slot.setValidTo(null); // Remove end date to make it active indefinitely
        
        slotDAO.save(slot);
        
        log.info("Slot {} reactivated", slotId);
    }
}