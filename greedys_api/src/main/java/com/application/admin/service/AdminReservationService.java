package com.application.admin.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.application.admin.web.post.AdminNewReservationDTO;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.service.ReservationBusinessService;
import com.application.common.web.dto.get.ReservationDTO;
import com.application.customer.dao.ReservationDAO;
import com.application.restaurant.web.post.NewReservationDTO;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AdminReservationService {

    private final ReservationDAO reservationDAO;
    private final ReservationBusinessService reservationBusinessService;

    public void setStatus(Long reservationId, Reservation.Status status) {
        reservationBusinessService.setStatus(reservationId, status);
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

    public Collection<ReservationDTO> getReservations(Long restaurantId, LocalDate start, LocalDate end) {
        return reservationBusinessService.getReservations(restaurantId, start, end);
    }

    public Collection<ReservationDTO> getAcceptedReservations(Long restaurantId, LocalDate start, LocalDate end) {
        return reservationBusinessService.getAcceptedReservations(restaurantId, start, end);
    }

    public Collection<ReservationDTO> getPendingReservations(Long restaurantId, LocalDate start, LocalDate end) {
        return reservationBusinessService.getPendingReservations(restaurantId, start, end);
    }

    public Page<ReservationDTO> getReservationsPageable(Long restaurantId, LocalDate start, LocalDate end,
            Pageable pageable) {
        return reservationBusinessService.getReservationsPageable(restaurantId, start, end, pageable);
    }

    public Page<ReservationDTO> getPendingReservationsPageable(Long restaurantId, LocalDate start, LocalDate end,
            Pageable pageable) {
        return reservationBusinessService.getPendingReservationsPageable(restaurantId, start, end, pageable);
    }
}
