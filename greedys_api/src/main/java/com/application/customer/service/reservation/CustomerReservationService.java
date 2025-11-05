package com.application.customer.service.reservation;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.mapper.ReservationMapper;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.reservation.Reservation.Status;
import com.application.common.persistence.model.reservation.ReservationRequest;
import com.application.common.persistence.model.reservation.Slot;
import com.application.common.service.reservation.ReservationService;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.customer.persistence.dao.ReservationRequestDAO;
import com.application.customer.persistence.model.Customer;
import com.application.customer.web.dto.reservations.CustomerNewReservationDTO;
import com.application.restaurant.persistence.dao.SlotDAO;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomerReservationService {

    private final ReservationDAO reservationDAO;
    private final ReservationRequestDAO reservationRequestDAO;
    private final ReservationService reservationService;
    private final SlotDAO slotDAO;
    private final ReservationMapper reservationMapper;

    public ReservationDTO createReservation(CustomerNewReservationDTO reservationDto, Customer customer) {
        Slot slot = slotDAO.getReferenceById(reservationDto.getIdSlot());
        if (slot == null || slot.getDeleted()) {
            throw new IllegalArgumentException("Slot is either null or deleted");
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
        return reservationMapper.toDTO(savedReservation);
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

}
