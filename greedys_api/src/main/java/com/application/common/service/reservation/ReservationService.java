package com.application.common.service.reservation;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.mapper.ReservationMapper;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.reservation.ReservationRequest;
import com.application.common.persistence.model.reservation.Slot;
import com.application.common.service.events.ReservationCreatedEvent;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.customer.persistence.dao.ReservationRequestDAO;
import com.application.restaurant.persistence.dao.ClosedDayDAO;
import com.application.restaurant.persistence.dao.ServiceDAO;
import com.application.restaurant.persistence.dao.SlotDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.web.dto.reservation.RestaurantNewReservationDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReservationService {
    // TODO verify when creating that the slot has not been deleted
    // TODO verify that the reservation date is greater than or equal to the
    // current date
    // TODO verify that the service is not deleted

    private final ReservationDAO reservationDAO;
    private final ReservationRequestDAO reservationRequestDAO;
    private final ServiceDAO serviceDAO;
    private final ClosedDayDAO closedDaysDAO;
    private final SlotDAO slotDAO;
    private final ApplicationEventPublisher eventPublisher;
    private final CustomerDAO customerDAO;
    private final ReservationMapper reservationMapper;

    public void save(Reservation reservation) {
        // Save the reservation
        reservationDAO.save(reservation);
    }

    public Reservation createNewReservation(Reservation reservation) {
        // Save first to get the ID
        Reservation savedReservation = reservationDAO.save(reservation);
        // 🎯 PUBLISH EVENT FOR NEW RESERVATION
        publishReservationCreatedEvent(savedReservation);
        // Return the saved reservation
        return savedReservation;
    }

    private void publishReservationCreatedEvent(Reservation reservation) {
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

    public List<Reservation> findAll(long id) {
        return reservationDAO.findAll();
    }

    public Reservation findById(Long id) {
        return reservationDAO.findById(id).orElse(null);
    }

    public ReservationDTO setStatus(Long reservationId, Reservation.Status status) {
        Reservation reservation = reservationDAO.findByIdWithRestaurant(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        reservation.setStatus(status);
        reservationDAO.save(reservation);
        return reservationMapper.toDTO(reservation);
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

    public Page<ReservationDTO> getReservationsPageable(Long restaurantId, LocalDate start, LocalDate end,
            Pageable pageable) {
        return reservationDAO.findReservationsByRestaurantAndDateRange(restaurantId, start, end, pageable)
                .map(ReservationDTO::new);
    }

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

    public ReservationDTO AcceptReservatioModifyRequestAndReturnDTO(Long reservationRequestId) {
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
        Reservation savedReservation = reservationDAO.save(reservation);

        // Notify the customer of the accepted reservation modification
        //customerNotificationService.createReservationNotification(reservation, Type.ALTERED);

        // Delete the reservation request after accepting it
        reservationRequestDAO.delete(reservationRequest);

        // Return the updated reservation as DTO
        return reservationMapper.toDTO(savedReservation);
    }

    
    public ReservationDTO createReservation(RestaurantNewReservationDTO reservationDto, Restaurant restaurant) {
        log.debug("Creating reservation with DTO: {}", reservationDto);
        log.debug("Restaurant: {}", restaurant);
        
        Slot slot = slotDAO.findById(reservationDto.getIdSlot())
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));

        //Customer customer = customerDAO.findByEmail(reservationDto.getUserEmail());
        //if (customer == null) {
        //    throw new IllegalArgumentException("Customer not found with email: " + reservationDto.getUserEmail());
        //}
        if (slot.getDeleted()) {
            throw new IllegalArgumentException("Slot is deleted");
        }
        var reservation = Reservation.builder()
                .pax(reservationDto.getPax())
                .kids(reservationDto.getKids())
                .notes(reservationDto.getNotes())
                .date(reservationDto.getReservationDay())
                .slot(slot)
                .restaurant(restaurant)
            //    .customer(customer)
                .createdByUserType(Reservation.UserType.RESTAURANT_USER) // 🔧 FIX: aggiunto campo mancante per auditing  
                .status(Reservation.Status.ACCEPTED)
                .build();
        reservation = reservationDAO.save(reservation);
        
        return new ReservationDTO(reservation);
    }

    @Cacheable(value = "closedDays", key = "#idRestaurant")
    public List<LocalDate> findNotAvailableDays(Long idRestaurant) {
        return serviceDAO.findClosedOrFullDays(idRestaurant);
    }

    @Cacheable(value = "closedDays", key = "'global'")
    public List<LocalDate> findClosedDays(Long idRestaurant) {
        return closedDaysDAO.findUpcomingClosedDay();
    }

    public List<ReservationDTO> getDayReservations(Restaurant restaurant, LocalDate date) {
        return reservationDAO.findDayReservation(restaurant.getId(), date).stream()
                .map(ReservationDTO::new).collect(Collectors.toList());
    }
 


}
