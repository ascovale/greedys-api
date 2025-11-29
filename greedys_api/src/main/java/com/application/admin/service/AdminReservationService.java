package com.application.admin.service;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.web.dto.reservation.AdminNewReservationDTO;
import com.application.common.persistence.mapper.ReservationMapper;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.reservation.Reservation.Status;
import com.application.common.persistence.model.notification.EventOutbox;
import com.application.common.service.reservation.ReservationService;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.common.persistence.dao.EventOutboxDAO;
import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.customer.persistence.model.Customer;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.dao.ServiceDAO;
import com.application.restaurant.persistence.model.Restaurant;
import org.springframework.data.domain.PageRequest;

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
    private final ServiceDAO serviceDAO;
    private final EventOutboxDAO eventOutboxDAO;

    public ReservationDTO createReservation(AdminNewReservationDTO reservationDto) {
        // Validate service exists and is not deleted
        com.application.common.persistence.model.reservation.Service service = serviceDAO.findById(reservationDto.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
        
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
                .date(reservationDto.getReservationDateTime().toLocalDate())
                .reservationDateTime(reservationDto.getReservationDateTime())
                .service(service)
                .customer(customer)
                .restaurant(restaurant)
                .userName(reservationDto.getUserName())
                .createdBy(customer) // For anonymous reservations this will be null
                .createdByUserType(customer != null ? Reservation.UserType.CUSTOMER : Reservation.UserType.ADMIN)
                .status(reservationStatus)
                .build();
        
        // Use the common service that publishes the event
        Reservation savedReservation = reservationService.createNewReservationWithValidation(reservation);
        
        // 📌 CREATE CUSTOMER_RESERVATION_CREATED EVENT (notifies customer only)
        createCustomerReservationCreatedEvent(savedReservation);
        
        return reservationMapper.toDTO(savedReservation);
    }
    
    /**
     * Create CUSTOMER_RESERVATION_CREATED event (restaurant user/admin created reservation)
     * Notifies: CUSTOMER ONLY (confirmation that their reservation was created)
     */
    private void createCustomerReservationCreatedEvent(Reservation reservation) {
        String eventId = "CUSTOMER_RESERVATION_CREATED_" + reservation.getId() + "_" + System.currentTimeMillis();
        String payload = buildReservationPayload(reservation);
        
        EventOutbox eventOutbox = EventOutbox.builder()
            .eventId(eventId)
            .eventType("CUSTOMER_RESERVATION_CREATED")
            .aggregateType("ADMIN")
            .aggregateId(reservation.getId())
            .payload(payload)
            .status(EventOutbox.Status.PENDING)
            .build();
        
        eventOutboxDAO.save(eventOutbox);
        
        log.info("✅ Created EventOutbox CUSTOMER_RESERVATION_CREATED: eventId={}, reservationId={}, aggregateType=ADMIN, status=PENDING", 
            eventId, reservation.getId());
    }
    
    /**
     * Build JSON payload for EventOutbox
     * 
     * ⭐ INCLUDES initiated_by=ADMIN for intelligent routing
     * EventOutboxOrchestrator uses this to route to both queues:
     * - notification.restaurant.reservations (TEAM notifications for restaurant)
     * - notification.customer (PERSONAL notifications for customer, if exists)
     */
    private String buildReservationPayload(Reservation reservation) {
        Long customerId = reservation.getCustomer() != null ? reservation.getCustomer().getId() : null;
        String customerEmail = reservation.getCustomer() != null ? reservation.getCustomer().getEmail() : "anonymous";
        Long restaurantId = reservation.getRestaurant().getId();
        Integer kids = reservation.getKids() != null ? reservation.getKids() : 0;
        String notes = reservation.getNotes() != null ? reservation.getNotes().replace("\"", "\\\"") : "";
        
        return String.format(
            "{\"reservationId\":%d,\"customerId\":%s,\"restaurantId\":%d,\"email\":\"%s\",\"datetime\":\"%s\",\"pax\":%d,\"kids\":%d,\"notes\":\"%s\",\"initiated_by\":\"ADMIN\"}",
            reservation.getId(),
            customerId != null ? customerId : "null",
            restaurantId,
            customerEmail,
            reservation.getReservationDateTime().toString(),
            reservation.getPax(),
            kids,
            notes
        );
    }

    public Collection<ReservationDTO> findAllCustomerReservations(Long customerId) {
        return reservationDAO.findByCustomerIdOrderByReservationDatetimeDesc(customerId, PageRequest.of(0, Integer.MAX_VALUE)).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Collection<ReservationDTO> findAcceptedCustomerReservations(Long customerId) {
        Status status = Status.ACCEPTED;
        return reservationDAO.findByCustomerIdOrderByReservationDatetimeDesc(customerId, PageRequest.of(0, Integer.MAX_VALUE)).stream()
                .filter(r -> r.getStatus() == status)
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Collection<ReservationDTO> findPendingCustomerReservations(Long customerId) {
        Status status = Status.NOT_ACCEPTED;
        return reservationDAO.findByCustomerIdOrderByReservationDatetimeDesc(customerId, PageRequest.of(0, Integer.MAX_VALUE)).stream()
                .filter(r -> r.getStatus() == status)
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

        // Update service if provided
        if (reservationDto.getServiceId() != null) {
            com.application.common.persistence.model.reservation.Service service = serviceDAO.findById(reservationDto.getServiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Service not found"));
            reservation.setService(service);
        }

        // Update reservation datetime if provided
        if (reservationDto.getReservationDateTime() != null) {
            reservation.setReservationDateTime(reservationDto.getReservationDateTime());
            reservation.setDate(reservationDto.getReservationDateTime().toLocalDate());
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

        // ✅ VALIDATE BEFORE SAVING (service/date may have changed)
        reservationService.validateReservationDateAvailability(
            reservation.getRestaurant(),
            reservation.getDate(),
            reservation.getService().getId()
        );

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
}
