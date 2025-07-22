package com.application.admin.service.reservation;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.web.post.AdminNewReservationDTO;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.customer.dao.ReservationDAO;
import com.application.restaurant.web.post.NewReservationDTO;

@Service
@Transactional
public class AdminReservationService {

    private final ReservationDAO reservationDAO;

    public AdminReservationService(ReservationDAO reservationDAO) {
        this.reservationDAO = reservationDAO;
    }

    public void setStatus(Long reservationId, Reservation.Status status) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));

        reservation.setStatus(status);
        reservationDAO.save(reservation);
    }

    public void adminModifyReservation(Long oldReservationId, NewReservationDTO dTO) {
        Reservation reservation = reservationDAO.findById(oldReservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        if (reservation.getStatus() == Reservation.Status.DELETED) {
            throw new IllegalStateException("Cannot modify a deleted reservation");
        }
        reservation.setPax(dTO.getPax());
        reservation.setKids(dTO.getKids());
        reservation.setNotes(dTO.getNotes());
        reservation.setDate(dTO.getReservationDay());
        reservationDAO.save(reservation);
    }

    public void createReservation(AdminNewReservationDTO dTO) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createReservation'");
    }
}