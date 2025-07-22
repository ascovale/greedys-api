package com.application.common.service.reservation;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.events.ReservationCreatedEvent;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.customer.dao.ReservationDAO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service("reservationService")
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReservationService {
    // TODO verificare quando si crea che lo slot non sia stato cancellato
    // TODO verificare che la data della prenotazione sia maggiore o uguale alla
    // data attuale
    // TODO verificare che il servizio non sia deleted

    @PersistenceContext
    private EntityManager entityManager;
    private final ReservationDAO reservationDAO;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void save(Reservation reservation) {
        // Salva la prenotazione
        reservationDAO.save(reservation);
        
        // 🎯 PUBBLICA L'EVENTO QUANDO VIENE CREATA UNA NUOVA PRENOTAZIONE
        if (reservation.getId() != null) { // Solo per prenotazioni appena create
            ReservationCreatedEvent event = new ReservationCreatedEvent(
                this,
                reservation.getId(),
                reservation.getCustomer().getId(),
                reservation.getSlot().getService().getRestaurant().getId(),
                reservation.getCustomer().getEmail(),
                reservation.getDate().toString()
            );
            eventPublisher.publishEvent(event);
        }
    }

    /**
     * Metodo specifico per creare una nuova prenotazione con evento
     */
    @Transactional
    public Reservation createNewReservation(Reservation reservation) {
        // Salva prima di pubblicare l'evento per avere l'ID
        Reservation savedReservation = reservationDAO.save(reservation);
        
        // 🎯 PUBBLICA L'EVENTO PER NUOVA PRENOTAZIONE
        ReservationCreatedEvent event = new ReservationCreatedEvent(
            this,
            savedReservation.getId(),
            savedReservation.getCustomer().getId(),
            savedReservation.getSlot().getService().getRestaurant().getId(),
            savedReservation.getCustomer().getEmail(),
            savedReservation.getDate().toString()
        );
        eventPublisher.publishEvent(event);
        
        return savedReservation;
    }

    @Transactional
    public List<Reservation> findAll(long id) {
        return reservationDAO.findAll();
    }

    public Reservation findById(Long id) {
        return reservationDAO.findById(id).orElse(null);
    }
}