package com.application.customer.service.reservation;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.mapper.ReservationMapper;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.reservation.Reservation.Status;
import com.application.common.persistence.model.reservation.ReservationModificationRequest;
import com.application.common.persistence.model.reservation.ServiceVersion;
import com.application.common.persistence.model.notification.EventOutbox;
import com.application.common.service.reservation.ReservationService;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.common.persistence.dao.EventOutboxDAO;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.customer.persistence.dao.ReservationModificationRequestDAO;
import com.application.customer.persistence.model.Customer;
import com.application.customer.web.dto.reservations.CustomerNewReservationDTO;
import com.application.restaurant.persistence.dao.ServiceDAO;
import com.application.restaurant.persistence.dao.ServiceVersionDAO;
import org.springframework.data.domain.PageRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CustomerReservationService {

    private final ReservationDAO reservationDAO;
    private final ReservationModificationRequestDAO modificationRequestDAO;
    private final ReservationService reservationService;
    private final ServiceDAO serviceDAO;
    private final ServiceVersionDAO serviceVersionDAO;
    private final ReservationMapper reservationMapper;
    private final EventOutboxDAO eventOutboxDAO;

    public ReservationDTO createReservation(CustomerNewReservationDTO reservationDto, Customer customer) {
        // Validate service exists
        com.application.common.persistence.model.reservation.Service service = serviceDAO.findById(reservationDto.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
        
        // Find active service version for the reservation date
        ServiceVersion serviceVersion = serviceVersionDAO
            .findActiveVersionByServiceAndDate(
                reservationDto.getServiceId(), 
                reservationDto.getReservationDateTime().toLocalDate()
            )
            .orElseThrow(() -> new IllegalArgumentException("No active service version found for the requested date"));
        
        Reservation reservation = Reservation.builder()
                .userName(reservationDto.getUserName())
                .pax(reservationDto.getPax())
                .kids(reservationDto.getKids())
                .notes(reservationDto.getNotes())
                .date(reservationDto.getReservationDateTime().toLocalDate())
                .reservationDateTime(reservationDto.getReservationDateTime())
                .serviceVersion(serviceVersion)
                .restaurant(service.getRestaurant())
                .customer(customer)
                .createdBy(customer)
                .status(Reservation.Status.NOT_ACCEPTED)
                .build();
        
        // 🎯 USE COMMON SERVICE THAT PUBLISHES THE EVENT
        Reservation savedReservation = reservationService.createNewReservationWithValidation(reservation);
        
        // 📌 CREATE RESERVATION_REQUESTED EVENT (notifies restaurant staff)
        createReservationRequestedEvent(savedReservation);
        
        return reservationMapper.toDTO(savedReservation);
    }
    
    /**
     * Create RESERVATION_REQUESTED event (customer created reservation)
     * Notifies: RESTAURANT STAFF
     */
    private void createReservationRequestedEvent(Reservation reservation) {
        String eventId = "RESERVATION_REQUESTED_" + reservation.getId() + "_" + System.currentTimeMillis();
        String payload = buildReservationPayload(reservation);
        
        EventOutbox eventOutbox = EventOutbox.builder()
            .eventId(eventId)
            .eventType("RESERVATION_REQUESTED")
            .aggregateType("CUSTOMER")
            .aggregateId(reservation.getId())
            .payload(payload)
            .status(EventOutbox.Status.PENDING)
            .build();
        
        eventOutboxDAO.save(eventOutbox);
        
        log.info("✅ Created EventOutbox RESERVATION_REQUESTED: eventId={}, reservationId={}, aggregateType=CUSTOMER, status=PENDING", 
            eventId, reservation.getId());
    }
    
    /**
     * Build JSON payload for EventOutbox
     * 
     * ⭐ INCLUDES initiated_by=CUSTOMER for intelligent routing
     * EventOutboxOrchestrator uses this to route to:
     * - notification.restaurant.reservations (TEAM notifications)
     * instead of default notification.restaurant.user
     */
    private String buildReservationPayload(Reservation reservation) {
        Long customerId = reservation.getCustomer() != null ? reservation.getCustomer().getId() : null;
        String customerEmail = reservation.getCustomer() != null ? reservation.getCustomer().getEmail() : "anonymous";
        Long restaurantId = reservation.getRestaurant().getId();
        Integer kids = reservation.getKids() != null ? reservation.getKids() : 0;
        String notes = reservation.getNotes() != null ? reservation.getNotes().replace("\"", "\\\"") : "";
        
        return String.format(
            "{\"reservationId\":%d,\"customerId\":%s,\"restaurantId\":%d,\"email\":\"%s\",\"datetime\":\"%s\",\"pax\":%d,\"kids\":%d,\"notes\":\"%s\",\"initiated_by\":\"CUSTOMER\"}",
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

    @Transactional
    public void requestModifyReservation(Long oldReservationId, CustomerNewReservationDTO dTO, Customer currentUser) {

        Reservation reservation = reservationDAO.findById(oldReservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));

        if (reservation.getStatus() != Reservation.Status.NOT_ACCEPTED && reservation.getStatus() != Reservation.Status.ACCEPTED) {
            throw new IllegalStateException("Cannot modify this reservation");
        }

        // ✅ VALIDATE NEW RESERVATION DATE BEFORE STORING REQUEST
        reservationService.validateReservationDateAvailability(
            reservation.getRestaurant(),
            dTO.getReservationDateTime().toLocalDate(),
            reservation.getServiceVersion()
        );

        // Create ReservationModificationRequest with PENDING_APPROVAL status
        // This stores both original and requested values for comparison
        ReservationModificationRequest modificationRequest = ReservationModificationRequest.builder()
                .reservation(reservation)
                .status(ReservationModificationRequest.Status.PENDING_APPROVAL)
                // Original values (from current reservation)
                .originalDate(reservation.getDate())
                .originalDateTime(reservation.getReservationDateTime())
                .originalPax(reservation.getPax())
                .originalKids(reservation.getKids())
                .originalNotes(reservation.getNotes())
                // Requested new values (from customer)
                .requestedDate(dTO.getReservationDateTime().toLocalDate())
                .requestedDateTime(dTO.getReservationDateTime())
                .requestedPax(dTO.getPax())
                .requestedKids(dTO.getKids())
                .requestedNotes(dTO.getNotes())
                // Auditing
                .requestedBy(currentUser)
                .requestedAt(java.time.LocalDateTime.now())
                .build();

        // Save the modification request
        modificationRequestDAO.save(modificationRequest);
        
        // 📌 CREATE EVENT: RESERVATION_MODIFICATION_REQUESTED (notifies restaurant staff)
        // Audit is handled when restaurant reviews the request (approve/reject)
        createReservationModificationRequestedEvent(reservation, modificationRequest);
        
        log.info("✅ Customer {} requested modification for reservation {}", currentUser.getId(), oldReservationId);
    }

    public Collection<ReservationDTO> findAllCustomerReservations(Long customerId) {
        return reservationDAO.findByCustomerIdOrderByReservationDatetimeDesc(customerId, PageRequest.of(0, Integer.MAX_VALUE)).stream()
                .map(reservationMapper::toDTO).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> findAcceptedCustomerReservations(Long customerId) {
        Status status = Reservation.Status.ACCEPTED;
        return reservationDAO.findByCustomerIdOrderByReservationDatetimeDesc(customerId, PageRequest.of(0, Integer.MAX_VALUE)).stream()
                .filter(r -> r.getStatus() == status)
                .map(reservationMapper::toDTO).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> findPendingCustomerReservations(Long customerId) {
        Status status = Reservation.Status.NOT_ACCEPTED;
        return reservationDAO.findByCustomerIdOrderByReservationDatetimeDesc(customerId, PageRequest.of(0, Integer.MAX_VALUE)).stream()
                .filter(r -> r.getStatus() == status)
                .map(reservationMapper::toDTO).collect(Collectors.toList());
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
                .map(reservationMapper::toDTO)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
    }

    /**
     * Create RESERVATION_MODIFICATION_REQUESTED event
     * 
     * Triggered when: CUSTOMER requests a modification to their reservation
     * Notifies: RESTAURANT STAFF (for approval/rejection)
     * Event Type: RESERVATION_MODIFICATION_REQUESTED
     * Aggregate Type: CUSTOMER (shows it's initiated by customer)
     */
    private void createReservationModificationRequestedEvent(Reservation reservation, ReservationModificationRequest modRequest) {
        String eventId = "RESERVATION_MODIFICATION_REQUESTED_" + modRequest.getId() + "_" + System.currentTimeMillis();
        String payload = buildReservationModificationPayload(reservation, modRequest);
        
        EventOutbox eventOutbox = EventOutbox.builder()
            .eventId(eventId)
            .eventType("RESERVATION_MODIFICATION_REQUESTED")
            .aggregateType("CUSTOMER")
            .aggregateId(modRequest.getId())
            .payload(payload)
            .status(EventOutbox.Status.PENDING)
            .build();
        
        eventOutboxDAO.save(eventOutbox);
        
        log.info("✅ Created EventOutbox RESERVATION_MODIFICATION_REQUESTED: eventId={}, modificationRequestId={}, reservationId={}, status=PENDING", 
            eventId, modRequest.getId(), reservation.getId());
    }

    /**
     * Build JSON payload for RESERVATION_MODIFICATION_REQUESTED event
     * 
     * Includes both original and requested values for comparison
     * Includes initiated_by=CUSTOMER for intelligent routing to restaurant staff
     */
    private String buildReservationModificationPayload(Reservation reservation, ReservationModificationRequest modRequest) {
        Long customerId = reservation.getCustomer() != null ? reservation.getCustomer().getId() : null;
        String customerEmail = reservation.getCustomer() != null ? reservation.getCustomer().getEmail() : "anonymous";
        Long restaurantId = reservation.getRestaurant().getId();
        
        // Escape special characters in notes
        String originalNotes = modRequest.getOriginalNotes() != null ? modRequest.getOriginalNotes().replace("\"", "\\\"") : "";
        String requestedNotes = modRequest.getRequestedNotes() != null ? modRequest.getRequestedNotes().replace("\"", "\\\"") : "";
        String customerReason = modRequest.getCustomerReason() != null ? modRequest.getCustomerReason().replace("\"", "\\\"") : "";
        
        return String.format(
            "{\"modificationRequestId\":%d,\"reservationId\":%d,\"customerId\":%s,\"restaurantId\":%d,\"email\":\"%s\"," +
            "\"originalDate\":\"%s\",\"requestedDate\":\"%s\"," +
            "\"originalPax\":%d,\"requestedPax\":%d," +
            "\"originalKids\":%d,\"requestedKids\":%d," +
            "\"originalNotes\":\"%s\",\"requestedNotes\":\"%s\",\"customerReason\":\"%s\"," +
            "\"initiated_by\":\"CUSTOMER\"}",
            modRequest.getId(),
            reservation.getId(),
            customerId != null ? customerId : "null",
            restaurantId,
            customerEmail,
            modRequest.getOriginalDate() != null ? modRequest.getOriginalDate().toString() : "",
            modRequest.getRequestedDate() != null ? modRequest.getRequestedDate().toString() : "",
            modRequest.getOriginalPax() != null ? modRequest.getOriginalPax() : 0,
            modRequest.getRequestedPax() != null ? modRequest.getRequestedPax() : 0,
            modRequest.getOriginalKids() != null ? modRequest.getOriginalKids() : 0,
            modRequest.getRequestedKids() != null ? modRequest.getRequestedKids() : 0,
            originalNotes,
            requestedNotes,
            customerReason
        );
    }
}
