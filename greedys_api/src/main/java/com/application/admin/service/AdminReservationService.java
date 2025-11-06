package com.application.admin.service;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.web.dto.reservation.AdminNewReservationDTO;
import com.application.common.persistence.mapper.ReservationMapper;
import com.application.common.persistence.mapper.Mapper.Weekday;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.reservation.Reservation.Status;
import com.application.common.persistence.model.reservation.Slot;
import com.application.common.service.reservation.ReservationService;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.customer.persistence.model.Customer;
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
    private final ReservationMapper reservationMapper;
    private final CustomerDAO customerDAO;
    private final RestaurantDAO restaurantDAO;
    private final SlotDAO slotDAO;

    public ReservationDTO createReservation(AdminNewReservationDTO reservationDto) {
        // Validate slot exists and is not deleted
        Slot slot = slotDAO.findById(reservationDto.getIdSlot())
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        if (slot.getDeleted()) {
            throw new IllegalArgumentException("Slot is deleted");
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
        
        // Get restaurant
        Restaurant restaurant = restaurantDAO.findById(reservationDto.getRestaurantId())
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        
        // Handle customer - can be anonymous or existing customer
        Customer customer = null;
        if (!reservationDto.isAnonymous()) {
            customer = customerDAO.findById(reservationDto.getUserId())
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
                .createdByUserType(customer != null ? Reservation.UserType.CUSTOMER : Reservation.UserType.ADMIN) // 🔧 FIX: aggiunto campo mancante per auditing
                .status(reservationStatus)
                .build();
        
        // Use the common service that publishes the event
        Reservation savedReservation = reservationService.createNewReservation(reservation);
        
        return reservationMapper.toDTO(savedReservation);
    }

    public Collection<ReservationDTO> findAllCustomerReservations(Long customerId) {
        return reservationDAO.findByCustomer(customerId).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Collection<ReservationDTO> findAcceptedCustomerReservations(Long customerId) {
        Status status = Status.ACCEPTED;
        return reservationDAO.findByCustomerAndStatus(customerId, status).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Collection<ReservationDTO> findPendingCustomerReservations(Long customerId) {
        Status status = Status.NOT_ACCEPTED;
        return reservationDAO.findByCustomerAndStatus(customerId, status).stream()
                .map(reservationMapper::toDTO)
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
                .map(reservationMapper::toDTO)
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

        // Update reservation date if provided
        if (reservationDto.getReservationDay() != null) {
            reservation.setDate(reservationDto.getReservationDay());
        }

        // Validate weekday consistency if both slot and date are set
        if (reservation.getSlot() != null && reservation.getDate() != null) {
            DayOfWeek reservationDayOfWeek = reservation.getDate().getDayOfWeek();
            Weekday reservationWeekday = convertDayOfWeekToWeekday(reservationDayOfWeek);
            if (!reservationWeekday.equals(reservation.getSlot().getWeekday())) {
                throw new IllegalArgumentException(
                    String.format("Reservation day %s (%s) does not match slot weekday %s", 
                        reservation.getDate(), 
                        reservationWeekday, 
                        reservation.getSlot().getWeekday()));
            }
        }

        // Update restaurant if provided
        if (reservationDto.getRestaurantId() != null) {
            Restaurant restaurant = restaurantDAO.findById(reservationDto.getRestaurantId())
                    .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
            reservation.setRestaurant(restaurant);
        }

        // Update customer if not anonymous
        if (!reservationDto.isAnonymous() && reservationDto.getUserId() != null) {
            Customer customer = customerDAO.findById(reservationDto.getUserId())
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
        if (reservationDto.getStatus() != null) {
            reservation.setStatus(reservationDto.getStatus());
        }

        Reservation saved = reservationDAO.save(reservation);
        return reservationMapper.toDTO(saved);
    }

    /**
     * Fix old reservations by filling missing userName with default value (Guest + ID)
     * @return message with count of updated reservations
     */
    public String fixMissingUsernames() {
        // Get all reservations with NULL or empty userName
        Collection<Reservation> reservationsToFix = reservationDAO.findAll().stream()
                .filter(r -> r.getUserName() == null || r.getUserName().trim().isEmpty())
                .collect(Collectors.toList());
        
        long count = reservationsToFix.size();
        
        if (count == 0) {
            log.info("✅ No reservations need fixing - all have userName");
            return "No reservations need fixing - all have userName already";
        }
        
        log.info("🔄 Found {} reservations without userName, updating...", count);
        
        // Update each reservation
        for (Reservation reservation : reservationsToFix) {
            reservation.setUserName("Guest " + reservation.getId());
        }
        
        // Save all at once
        reservationDAO.saveAll(reservationsToFix);
        
        String message = "✅ Fixed " + count + " reservations with default userName";
        log.info(message);
        return message;
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
