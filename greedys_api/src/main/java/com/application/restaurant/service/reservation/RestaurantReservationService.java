package com.application.restaurant.service.reservation;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.reservation.ReservationRequest;
import com.application.common.persistence.model.reservation.Slot;
import com.application.common.service.ReservationBusinessService;
import com.application.common.web.dto.get.ReservationDTO;
import com.application.customer.dao.ReservationDAO;
import com.application.customer.dao.ReservationRequestDAO;
import com.application.restaurant.dao.ClosedDayDAO;
import com.application.restaurant.dao.ServiceDAO;
import com.application.restaurant.model.Restaurant;
import com.application.restaurant.web.post.RestaurantNewReservationDTO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RestaurantReservationService {

    private final ReservationDAO reservationDAO;
    private final ReservationRequestDAO reservationRequestDAO;
    private final ServiceDAO serviceDAO;
    private final ClosedDayDAO closedDaysDAO;

    private final ReservationBusinessService reservationBusinessService;

    @PersistenceContext
    private EntityManager entityManager;

    public ReservationDTO createReservation(RestaurantNewReservationDTO reservationDto, Restaurant restaurant) {
        Slot slot = entityManager.getReference(Slot.class, reservationDto.getIdSlot());
        if (slot == null || slot.getDeleted()) {
            throw new IllegalArgumentException("Slot is either null or deleted");
        }
        var reservation = Reservation.builder()
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
        reservationBusinessService.setStatus(reservationId, status);
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

    public Collection<ReservationDTO> getPendingReservations(Long restaurantId, LocalDate start, LocalDate end) {
        return reservationBusinessService.getPendingReservations(restaurantId, start, end);
    }

    public Collection<ReservationDTO> getReservations(Long restaurantId, LocalDate start, LocalDate end) {
        return reservationBusinessService.getReservations(restaurantId, start, end);
    }

    public Collection<ReservationDTO> getAcceptedReservations(Long restaurantId, LocalDate start, LocalDate end) {
        return reservationBusinessService.getAcceptedReservations(restaurantId, start, end);
    }

    public Page<ReservationDTO> getReservationsPageable(Long restaurantId, LocalDate start, LocalDate end, Pageable pageable) {
        return reservationBusinessService.getReservationsPageable(restaurantId, start, end, pageable);
    }

    public Page<ReservationDTO> getPendingReservationsPageable(Long restaurantId, LocalDate start, LocalDate end,
            Pageable pageable) {
        return reservationBusinessService.getPendingReservationsPageable(restaurantId, start, end, pageable);
    }
 
}
