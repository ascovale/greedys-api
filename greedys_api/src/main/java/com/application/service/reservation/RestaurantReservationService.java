package com.application.service.reservation;

import com.application.persistence.dao.restaurant.ClosedDayDAO;
import com.application.persistence.dao.restaurant.ServiceDAO;
import com.application.persistence.dao.customer.ReservationDAO;
import com.application.persistence.dao.customer.ReservationRequestDAO;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.reservation.Slot;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.reservation.ReservationRequest;
import com.application.web.dto.get.ReservationDTO;
import com.application.web.dto.post.restaurant.RestaurantNewReservationDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Transactional
public class RestaurantReservationService {

    private final ReservationDAO reservationDAO;
    private final ReservationRequestDAO reservationRequestDAO;
    private final ServiceDAO serviceDAO;
    private final ClosedDayDAO closedDaysDAO;

    @PersistenceContext
    private EntityManager entityManager;

    public RestaurantReservationService(ReservationDAO reservationDAO, ReservationRequestDAO reservationRequestDAO, ServiceDAO serviceDAO, ClosedDayDAO closedDaysDAO) {
        this.reservationDAO = reservationDAO;
        this.reservationRequestDAO = reservationRequestDAO;
        this.serviceDAO = serviceDAO;
        this.closedDaysDAO = closedDaysDAO;
    }

    public ReservationDTO createReservation(RestaurantNewReservationDTO reservationDto, Restaurant restaurant) {

        Slot slot = entityManager.getReference(Slot.class, reservationDto.getIdSlot());
        if (slot == null || slot.getDeleted()) {
            throw new IllegalArgumentException("Slot is either null or deleted");
        }
        
        var reservation = Reservation.builder()
                .userName(reservationDto.getUserName())
                .pax(reservationDto.getPax())
                .kids(reservationDto.getKids())
                .notes(reservationDto.getNotes())
                .date(reservationDto.getReservationDay())
                .slot(slot)
                .restaurant(restaurant)
                .status(Reservation.Status.ACCEPTED)
                .build();
        reservation = reservationDAO.save(reservation);
        
        return new ReservationDTO(reservation);
    }

    @Transactional
    public void AcceptReservatioModifyRequest(Long reservationRequestId) {
        ReservationRequest reservationRequest = reservationRequestDAO.findById(reservationRequestId)
                .orElseThrow(() -> new NoSuchElementException("Reservation request not found"));
        Reservation reservation = reservationRequest.getReservation();

        // Update reservation with the details from the request
        reservation.setPax(reservationRequest.getPax());
        reservation.setKids(reservationRequest.getKids());
        reservation.setNotes(reservationRequest.getNotes());
        reservation.setDate(reservationRequest.getDate());
        reservation.setSlot(reservationRequest.getSlot());

        // Save the updated reservation
        reservationDAO.save(reservation);

        // Notify the customer of the accepted reservation modification
        //customerNotificationService.createReservationNotification(reservation, Type.ALTERED);

        // Delete the reservation request after accepting it
        reservationRequestDAO.delete(reservationRequest);
    }

    public void setStatus(Long reservationId, Reservation.Status status) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        reservation.setStatus(status);
        reservationDAO.save(reservation);
    }

    public List<LocalDate> findNotAvailableDays(Long idRestaurant) {
        return serviceDAO.findClosedOrFullDays(idRestaurant);
    }

    public List<LocalDate> findClosedDays(Long idRestaurant) {
        return closedDaysDAO.findUpcomingClosedDay();
    }

    public List<ReservationDTO> getDayReservations(Restaurant restaurant, LocalDate date) {
        return reservationDAO.findDayReservation(restaurant.getId(), date).stream()
                .map(ReservationDTO::new).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> getReservations(Long restaurantId, LocalDate start, LocalDate end) {
        return reservationDAO.findByRestaurantAndDateBetween(restaurantId, start, end).stream()
                .map(ReservationDTO::new).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> getAcceptedReservations(Long restaurantId, LocalDate start, LocalDate end) {
        Reservation.Status status = Reservation.Status.ACCEPTED;
        return reservationDAO.findByRestaurantAndDateBetweenAndStatus(restaurantId, start, end, status).stream()
                .map(ReservationDTO::new)
                .collect(Collectors.toList());
    }

    public Collection<ReservationDTO> getPendingReservations(Long restaurantId, LocalDate start) {
        Reservation.Status status = Reservation.Status.NOT_ACCEPTED;
        return reservationDAO.findByRestaurantAndDateAndStatus(restaurantId, start, status).stream()
                .map(ReservationDTO::new)
                .collect(Collectors.toList());
    }

    public Collection<ReservationDTO> getPendingReservations(Long restaurantId, LocalDate start, LocalDate end) {
        Reservation.Status status = Reservation.Status.NOT_ACCEPTED;
        return reservationDAO.findByRestaurantAndDateBetweenAndStatus(restaurantId, start, end, status).stream()
                .map(ReservationDTO::new)
                .collect(Collectors.toList());
    }

    public Collection<ReservationDTO> getPendingReservations(Long restaurantId) {
        Reservation.Status status = Reservation.Status.NOT_ACCEPTED;
        return reservationDAO.findByRestaurantIdAndStatus(restaurantId, status).stream()
                .map(ReservationDTO::new)
                .collect(Collectors.toList());
    }

    public Page<ReservationDTO> getReservationsPageable(Long restaurantId, LocalDate start, LocalDate end, Pageable pageable) {
        return reservationDAO.findReservationsByRestaurantAndDateRange(restaurantId, start, end, pageable).map(ReservationDTO::new);
    }

    public Page<ReservationDTO> getPendingReservationsPageable(Long restaurantId, LocalDate start, LocalDate end,
            Pageable pageable) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPendingReservationsPageable'");
    }
 
}
