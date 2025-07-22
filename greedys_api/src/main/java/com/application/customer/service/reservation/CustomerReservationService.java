package com.application.customer.service.reservation;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.reservation.Reservation.Status;
import com.application.common.persistence.model.reservation.ReservationRequest;
import com.application.common.persistence.model.reservation.Slot;
import com.application.common.web.dto.get.ReservationDTO;
import com.application.customer.dao.ReservationDAO;
import com.application.customer.dao.ReservationRequestDAO;
import com.application.customer.model.Customer;
import com.application.customer.web.post.CustomerNewReservationDTO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
@Transactional
public class CustomerReservationService {

    private final ReservationDAO reservationDAO;
    private final ReservationRequestDAO reservationRequestDAO;

    @PersistenceContext
    private EntityManager entityManager;

    public CustomerReservationService(ReservationDAO reservationDAO, ReservationRequestDAO reservationRequestDAO) {
        this.reservationDAO = reservationDAO;
        this.reservationRequestDAO = reservationRequestDAO;
    }



    public void createReservation(CustomerNewReservationDTO reservationDto, Customer customer) {
        Slot slot = entityManager.getReference(Slot.class, reservationDto.getIdSlot());
        if (slot == null || slot.getDeleted()) {
            throw new IllegalArgumentException("Slot is either null or deleted");
        }
        
        Reservation reservation = Reservation.builder()
                .pax(reservationDto.getPax())
                .kids(reservationDto.getKids())
                .notes(reservationDto.getNotes())
                .date(reservationDto.getReservationDay())
                .slot(slot)
                .customer(customer)
                .createdBy(customer) // TODO : Test this that it works
                .status(Reservation.Status.ACCEPTED)
                .build();
        
        reservationDAO.save(reservation);
    }

    @Transactional
    public void requestModifyReservation(Long oldReservationId, CustomerNewReservationDTO dTO, Customer currentUser) {
        
        Reservation reservation = reservationDAO.findById(oldReservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));

        if (reservation.getStatus() != Reservation.Status.NOT_ACCEPTED && reservation.getStatus() != Reservation.Status.ACCEPTED) {
            throw new IllegalStateException("Cannot modify this reservation");
        }

        Slot slot = entityManager.getReference(Slot.class, dTO.getIdSlot());
        
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
                .map(ReservationDTO::new).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> findAcceptedCustomerReservations(Long customerId) {
        Status status = Reservation.Status.ACCEPTED;
        return reservationDAO.findByCustomerAndStatus(customerId, status).stream()
                .map(ReservationDTO::new).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> findPendingCustomerReservations(Long customerId) {
        Status status = Reservation.Status.NOT_ACCEPTED;
        return reservationDAO.findByCustomerAndStatus(customerId, status).stream()
                .map(ReservationDTO::new).collect(Collectors.toList());
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
                .map(ReservationDTO::new)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
    }


}
