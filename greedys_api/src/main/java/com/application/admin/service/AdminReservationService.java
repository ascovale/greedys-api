package com.application.admin.service;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.web.dto.reservation.AdminNewReservationDTO;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.reservation.Reservation.Status;
import com.application.common.persistence.model.reservation.Slot;
import com.application.common.service.reservation.ReservationService;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.customer.persistence.model.Customer;
import com.application.customer.service.CustomerService;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.dao.SlotDAO;
import com.application.restaurant.persistence.model.Restaurant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdminReservationService {

    private final ReservationDAO reservationDAO;
    private final ReservationService reservationService;
    private final CustomerService customerService;
    private final RestaurantDAO restaurantDAO;
    private final SlotDAO slotDAO;

    public ReservationDTO createReservation(AdminNewReservationDTO reservationDto) {
        // Validate slot exists and is not deleted
        Slot slot = slotDAO.findById(reservationDto.getIdSlot())
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        if (slot.getDeleted()) {
            throw new IllegalArgumentException("Slot is deleted");
        }
        
        // Get restaurant
        Restaurant restaurant = restaurantDAO.findById(reservationDto.getRestaurantId())
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        
        // Handle customer - can be anonymous or existing customer
        Customer customer = null;
        if (!reservationDto.isAnonymous()) {
            customer = customerService.getCustomerByID(reservationDto.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        }
        
        // Use status from DTO, default to ACCEPTED if not specified
        Status reservationStatus = reservationDto.getStatus() != null ? 
            reservationDto.getStatus() : Status.ACCEPTED;
        
        Reservation reservation = Reservation.builder()
                .pax(reservationDto.getPax())
                .kids(reservationDto.getKids())
                .notes(reservationDto.getNotes())
                .date(reservationDto.getReservationDay())
                .slot(slot)
                .customer(customer)
                .restaurant(restaurant)
                .createdBy(customer) // For anonymous reservations this will be null
                .status(reservationStatus)
                .build();
        
        // Use the common service that publishes the event
        Reservation savedReservation = reservationService.createNewReservation(reservation);
        
        return new ReservationDTO(savedReservation);
    }

    public Collection<ReservationDTO> findAllCustomerReservations(Long customerId) {
        return reservationDAO.findByCustomer(customerId).stream()
                .map(ReservationDTO::new)
                .collect(Collectors.toList());
    }

    public Collection<ReservationDTO> findAcceptedCustomerReservations(Long customerId) {
        Status status = Status.ACCEPTED;
        return reservationDAO.findByCustomerAndStatus(customerId, status).stream()
                .map(ReservationDTO::new)
                .collect(Collectors.toList());
    }

    public Collection<ReservationDTO> findPendingCustomerReservations(Long customerId) {
        Status status = Status.NOT_ACCEPTED;
        return reservationDAO.findByCustomerAndStatus(customerId, status).stream()
                .map(ReservationDTO::new)
                .collect(Collectors.toList());
    }

    public void acceptReservation(Long reservationId) {
        Status status = Status.ACCEPTED;
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        reservation.setStatus(status);
        reservationDAO.save(reservation);
    }

    public void markReservationNoShow(Long reservationId) {
        Status status = Status.NO_SHOW;
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        reservation.setStatus(status);
        reservationDAO.save(reservation);
    }

    public void markReservationSeated(Long reservationId) {
        Status status = Status.SEATED;
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

    public void updateReservationStatus(Long reservationId, Status status) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        reservation.setStatus(status);
        reservationDAO.save(reservation);
    }

    public ReservationDTO modifyReservation(Long reservationId, AdminNewReservationDTO reservationDto) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));

        // Update slot if provided and exists
        if (reservationDto.getIdSlot() != null) {
            Slot slot = slotDAO.findById(reservationDto.getIdSlot())
                    .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
            if (slot.getDeleted()) {
                throw new IllegalArgumentException("Slot is deleted");
            }
            reservation.setSlot(slot);
        }

        // Update restaurant if provided
        if (reservationDto.getRestaurantId() != null) {
            Restaurant restaurant = restaurantDAO.findById(reservationDto.getRestaurantId())
                    .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
            reservation.setRestaurant(restaurant);
        }

        // Update customer if not anonymous
        if (!reservationDto.isAnonymous() && reservationDto.getUserId() != null) {
            Customer customer = customerService.getCustomerByID(reservationDto.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            reservation.setCustomer(customer);
            reservation.setCreatedBy(customer);
        } else if (reservationDto.isAnonymous()) {
            reservation.setCustomer(null);
            reservation.setCreatedBy(null);
        }

        // Update other fields
        if (reservationDto.getPax() != null) {
            reservation.setPax(reservationDto.getPax());
        }
        if (reservationDto.getKids() != null) {
            reservation.setKids(reservationDto.getKids());
        }
        if (reservationDto.getNotes() != null) {
            reservation.setNotes(reservationDto.getNotes());
        }
        if (reservationDto.getReservationDay() != null) {
            reservation.setDate(reservationDto.getReservationDay());
        }
        if (reservationDto.getStatus() != null) {
            reservation.setStatus(reservationDto.getStatus());
        }

        Reservation saved = reservationDAO.save(reservation);
        return new ReservationDTO(saved);
    }
}
