package com.application.customer.service.reservation;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.mapper.ReservationMapper;
import com.application.common.persistence.mapper.Mapper.Weekday;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.reservation.Reservation.Status;
import com.application.common.persistence.model.reservation.ReservationRequest;
import com.application.common.persistence.model.reservation.Slot;
import com.application.common.persistence.model.notification.EventOutbox;
import com.application.common.service.reservation.ReservationService;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.common.persistence.dao.EventOutboxDAO;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.customer.persistence.dao.ReservationRequestDAO;
import com.application.customer.persistence.model.Customer;
import com.application.customer.web.dto.reservations.CustomerNewReservationDTO;
import com.application.restaurant.persistence.dao.SlotDAO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CustomerReservationService {

    private final ReservationDAO reservationDAO;
    private final ReservationRequestDAO reservationRequestDAO;
    private final ReservationService reservationService;
    private final SlotDAO slotDAO;
    private final ReservationMapper reservationMapper;
    private final EventOutboxDAO eventOutboxDAO;

    public ReservationDTO createReservation(CustomerNewReservationDTO reservationDto, Customer customer) {
        Slot slot = slotDAO.getReferenceById(reservationDto.getIdSlot());
        if (slot == null || slot.getDeleted()) {
            throw new IllegalArgumentException("Slot is either null or deleted");
        }
        
        // Validate that the reservation day matches the slot's weekday
        DayOfWeek reservationDayOfWeek = reservationDto.getReservationDay().getDayOfWeek();
        Weekday reservationWeekday = convertDayOfWeekToWeekday(reservationDayOfWeek);
        if (!reservationWeekday.equals(slot.getWeekday())) {
            throw new IllegalArgumentException(
                String.format("Reservation day %s (%s) does not match slot weekday %s", 
                    reservationDto.getReservationDay(), 
                    reservationWeekday, 
                    slot.getWeekday()));
        }
        
        Reservation reservation = Reservation.builder()
                .userName(reservationDto.getUserName()) // 🎯 AGGIUNTO: nome utente per la prenotazione
                .pax(reservationDto.getPax())
                .kids(reservationDto.getKids())
                .notes(reservationDto.getNotes())
                .date(reservationDto.getReservationDay())
                .slot(slot)
                .restaurant(slot.getService().getRestaurant()) // 🎯 AGGIUNTO: restaurant dal slot
                .customer(customer)
                .createdBy(customer) // TODO : Test this that it works
                .createdByUserType(Reservation.UserType.CUSTOMER) // 🔧 FIX: aggiunto campo mancante per auditing
                .status(Reservation.Status.NOT_ACCEPTED) // 🔧 FIX: customer reservations should wait for restaurant approval
                .build();
        
        // 🎯 USA IL SERVICE COMUNE CHE PUBBLICA L'EVENTO
        Reservation savedReservation = reservationService.createNewReservation(reservation);
        
        // 📌 CREATE RESERVATION_REQUESTED EVENT (notifies restaurant staff)
        createReservationRequestedEvent(savedReservation);
        
        return reservationMapper.toDTO(savedReservation);
    }
    
    /**
     * Create RESERVATION_REQUESTED event (customer created reservation)
     * Notifies: RESTAURANT STAFF
     */
    private void createReservationRequestedEvent(Reservation reservation) {
        String eventId = "RESERVATION_REQUESTED_" + reservation.getId() + "_" + System.currentTimeMillis();
        String payload = buildReservationPayload(reservation);
        
        EventOutbox eventOutbox = EventOutbox.builder()
            .eventId(eventId)
            .eventType("RESERVATION_REQUESTED")
            .aggregateType("CUSTOMER")
            .aggregateId(reservation.getId())
            .payload(payload)
            .status(EventOutbox.Status.PENDING)
            .build();
        
        eventOutboxDAO.save(eventOutbox);
        
        log.info("✅ Created EventOutbox RESERVATION_REQUESTED: eventId={}, reservationId={}, aggregateType=CUSTOMER, status=PENDING", 
            eventId, reservation.getId());
    }
    
    /**
     * Build JSON payload for EventOutbox
     */
    private String buildReservationPayload(Reservation reservation) {
        Long customerId = reservation.getCustomer() != null ? reservation.getCustomer().getId() : null;
        String customerEmail = reservation.getCustomer() != null ? reservation.getCustomer().getEmail() : "anonymous";
        Long restaurantId = reservation.getSlot().getService().getRestaurant().getId();
        Integer kids = reservation.getKids() != null ? reservation.getKids() : 0;
        String notes = reservation.getNotes() != null ? reservation.getNotes().replace("\"", "\\\"") : "";
        
        return String.format(
            "{\"reservationId\":%d,\"customerId\":%s,\"restaurantId\":%d,\"email\":\"%s\",\"date\":\"%s\",\"pax\":%d,\"kids\":%d,\"notes\":\"%s\"}",
            reservation.getId(),
            customerId != null ? customerId : "null",
            restaurantId,
            customerEmail,
            reservation.getDate().toString(),
            reservation.getPax(),
            kids,
            notes
        );
    }

    @Transactional
    public void requestModifyReservation(Long oldReservationId, CustomerNewReservationDTO dTO, Customer currentUser) {

        Reservation reservation = reservationDAO.findById(oldReservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));

        if (reservation.getStatus() != Reservation.Status.NOT_ACCEPTED && reservation.getStatus() != Reservation.Status.ACCEPTED) {
            throw new IllegalStateException("Cannot modify this reservation");
        }

        Slot slot = slotDAO.getReferenceById(dTO.getIdSlot());

        ReservationRequest reservationRequest = ReservationRequest.builder()
                .pax(dTO.getPax())
                .kids(dTO.getKids())
                .notes(dTO.getNotes())
                .date(dTO.getReservationDay())
                .slot(slot)
                .reservation(reservation)
                .customer(currentUser)
                .build();

        reservationRequestDAO.save(reservationRequest);
    }

    public Collection<ReservationDTO> findAllCustomerReservations(Long customerId) {
        return reservationDAO.findByCustomer(customerId).stream()
                .map(reservationMapper::toDTO).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> findAcceptedCustomerReservations(Long customerId) {
        Status status = Reservation.Status.ACCEPTED;
        return reservationDAO.findByCustomerAndStatus(customerId, status).stream()
                .map(reservationMapper::toDTO).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> findPendingCustomerReservations(Long customerId) {
        Status status = Reservation.Status.NOT_ACCEPTED;
        return reservationDAO.findByCustomerAndStatus(customerId, status).stream()
                .map(reservationMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public void deleteReservation(Long reservationId) {
        var reservation = reservationDAO.findById(reservationId).orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        reservation.setStatus(Reservation.Status.DELETED);
        reservationDAO.save(reservation);
    }

    public void rejectReservation(Long reservationId) {
        Reservation.Status status = Reservation.Status.REJECTED;
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        reservation.setStatus(status);
        reservationDAO.save(reservation);
    }

    public ReservationDTO findReservationById(Long reservationId) {
        return reservationDAO.findById(reservationId)
                .map(reservationMapper::toDTO)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
    }

    /**
     * Convert Java DayOfWeek to our custom Weekday enum
     */
    private Weekday convertDayOfWeekToWeekday(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY:
                return Weekday.MONDAY;
            case TUESDAY:
                return Weekday.TUESDAY;
            case WEDNESDAY:
                return Weekday.WEDNESDAY;
            case THURSDAY:
                return Weekday.THURSDAY;
            case FRIDAY:
                return Weekday.FRIDAY;
            case SATURDAY:
                return Weekday.SATURDAY;
            case SUNDAY:
                return Weekday.SUNDAY;
            default:
                throw new IllegalArgumentException("Invalid day of week: " + dayOfWeek);
        }
    }

}
