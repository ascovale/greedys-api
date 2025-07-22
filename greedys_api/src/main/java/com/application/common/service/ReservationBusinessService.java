package com.application.common.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.web.dto.get.ReservationDTO;
import com.application.customer.dao.ReservationDAO;

import lombok.RequiredArgsConstructor;

/**
 * Common service containing shared reservation business logic
 * Used by both AdminReservationService and RestaurantReservationService
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ReservationBusinessService {

    private final ReservationDAO reservationDAO;

    /**
     * Change reservation status
     */
    public void setStatus(Long reservationId, Reservation.Status status) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));

        reservation.setStatus(status);
        reservationDAO.save(reservation);
    }

    /**
     * Get all reservations for a restaurant in a date range
     */
    public Collection<ReservationDTO> getReservations(Long restaurantId, LocalDate start, LocalDate end) {
        return reservationDAO.findByRestaurantAndDateBetween(restaurantId, start, end).stream()
                .map(ReservationDTO::new).collect(Collectors.toList());
    }

    /**
     * Get accepted reservations for a restaurant in a date range
     */
    public Collection<ReservationDTO> getAcceptedReservations(Long restaurantId, LocalDate start, LocalDate end) {
        Reservation.Status status = Reservation.Status.ACCEPTED;
        return reservationDAO.findByRestaurantAndDateBetweenAndStatus(restaurantId, start, end, status).stream()
                .map(ReservationDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Get pending reservations for a restaurant with optional date filtering
     * @param restaurantId - Required restaurant ID
     * @param start - Optional start date (can be null)
     * @param end - Optional end date (can be null). If start is provided but end is null, treats start as single date
     * @return Collection of pending reservations based on date filtering
     */
    public Collection<ReservationDTO> getPendingReservations(Long restaurantId, LocalDate start, LocalDate end) {
        Reservation.Status status = Reservation.Status.NOT_ACCEPTED;
        if (restaurantId == null) {
            throw new IllegalArgumentException("restaurantId cannot be null");
        }
        if (start != null && end != null) {
            return reservationDAO.findByRestaurantAndDateBetweenAndStatus(restaurantId, start, end, status).stream()
                    .map(ReservationDTO::new)
                    .collect(Collectors.toList());
        } else if (start != null) {
            return reservationDAO.findByRestaurantAndDateAndStatus(restaurantId, start, status).stream()
                    .map(ReservationDTO::new)
                    .collect(Collectors.toList());
        } else {
            return reservationDAO.findByRestaurantIdAndStatus(restaurantId, status).stream()
                    .map(ReservationDTO::new)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Get reservations with pagination
     */
    public Page<ReservationDTO> getReservationsPageable(Long restaurantId, LocalDate start, LocalDate end,
            Pageable pageable) {
        return reservationDAO.findReservationsByRestaurantAndDateRange(restaurantId, start, end, pageable)
                .map(ReservationDTO::new);
    }

    /**
     * Get pending reservations with pagination
     */
    public Page<ReservationDTO> getPendingReservationsPageable(Long restaurantId, LocalDate start, LocalDate end,
            Pageable pageable) {
        Reservation.Status status = Reservation.Status.NOT_ACCEPTED;
        if (restaurantId == null) {
            throw new IllegalArgumentException("restaurantId cannot be null");
        }
        // TODO: Implement actual query or use existing methods
        return reservationDAO.findByRestaurantAndDateBetweenAndStatus(restaurantId, start, end, status, pageable)
                .map(ReservationDTO::new);
    }
}
