package com.application.admin.service;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.web.dto.reservation.AdminNewReservationDTO;
import com.application.common.domain.event.EventType;
import com.application.common.persistence.mapper.ReservationMapper;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.reservation.Reservation.Status;
import com.application.common.persistence.model.notification.EventOutbox;
import com.application.common.persistence.model.audit.ReservationAuditLog.UserType;
import com.application.common.service.audit.AuditService;
import com.application.common.service.reservation.ReservationService;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.common.persistence.dao.EventOutboxDAO;
import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.customer.persistence.model.Customer;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.dao.ServiceDAO;
import com.application.restaurant.persistence.dao.ServiceVersionDAO;
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
    private final ServiceVersionDAO serviceVersionDAO;
    private final EventOutboxDAO eventOutboxDAO;
    private final AuditService auditService;

    public ReservationDTO createReservation(AdminNewReservationDTO reservationDto, Long adminId) {
        // Get restaurant
        Restaurant restaurant = restaurantDAO.findById(reservationDto.getRestaurantId())
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        
        // Get service
        com.application.common.persistence.model.reservation.Service service = serviceDAO.findById(reservationDto.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
        
        // Find active service version for scheduling validation and snapshot data
        com.application.common.persistence.model.reservation.ServiceVersion serviceVersion = serviceVersionDAO
            .findActiveVersionByServiceAndDate(
                reservationDto.getServiceId(),
                reservationDto.getReservationDateTime().toLocalDate()
            )
            .orElseThrow(() -> new IllegalArgumentException("No active service version found for the requested date"));
        
        // Handle customer - can be anonymous or existing customer
        Customer customer = null;
        if (!reservationDto.isAnonymous()) {
            customer = customerDAO.findById(reservationDto.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        }
        
        // Use status from DTO, default to ACCEPTED if not specified
        Status reservationStatus = reservationDto.getStatus() != null ? 
            reservationDto.getStatus() : Status.ACCEPTED;
        
        // Build reservation with Service reference + snapshot fields
        Reservation reservation = Reservation.builder()
                .pax(reservationDto.getPax())
                .kids(reservationDto.getKids())
                .notes(reservationDto.getNotes())
                .date(reservationDto.getReservationDateTime().toLocalDate())
                .reservationDateTime(reservationDto.getReservationDateTime())
                .service(service) // Reference to Service (not ServiceVersion)
                // SNAPSHOT FIELDS - Capture at booking time
                .bookedServiceName(service.getName())
                .bookedSlotDuration(serviceVersion.getDuration())
                .customer(customer)
                .restaurant(restaurant)
                .userName(reservationDto.getUserName())
                .createdBy(customer)
                .status(reservationStatus)
                .build();
        
        // Use the common service that publishes the event
        Reservation savedReservation = reservationService.createNewReservationWithValidation(reservation);
        
        // 📌 CREATE CUSTOMER_RESERVATION_CREATED EVENT (notifies customer only)
        createCustomerReservationCreatedEvent(savedReservation);
        
        // 📝 AUDIT: Log reservation creation by admin
        auditService.auditReservationCreated(
            savedReservation.getId(),
            savedReservation.getRestaurant().getId(),
            adminId,
            UserType.ADMIN,
            buildAuditReservationData(savedReservation)
        );
        
        return reservationMapper.toDTO(savedReservation);
    }
    
    /**
     * Create CUSTOMER_RESERVATION_CREATED event (restaurant user/admin created reservation)
     * Notifies: CUSTOMER ONLY (confirmation that their reservation was created)
     */
    private void createCustomerReservationCreatedEvent(Reservation reservation) {
        String eventId = EventType.CUSTOMER_RESERVATION_CREATED.name() + "_" + reservation.getId() + "_" + System.currentTimeMillis();
        String payload = buildReservationPayload(reservation);
        
        EventOutbox eventOutbox = EventOutbox.builder()
            .eventId(eventId)
            .eventType(EventType.CUSTOMER_RESERVATION_CREATED.name())
            .aggregateType("ADMIN")
            .aggregateId(reservation.getId())
            .payload(payload)
            .status(EventOutbox.Status.PENDING)
            .build();
        
        eventOutboxDAO.save(eventOutbox);
        
        log.info("✅ Created EventOutbox {}: eventId={}, reservationId={}, aggregateType=ADMIN, status=PENDING", 
            EventType.CUSTOMER_RESERVATION_CREATED.name(), eventId, reservation.getId());
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

    public ReservationDTO markReservationNoShow(Long reservationId, Long adminId) {
        Status status = Status.NO_SHOW;
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        reservation.setStatus(status);
        Reservation saved = reservationDAO.save(reservation);
        
        // 📝 AUDIT: Log no-show by admin
        auditService.auditNoShow(
            reservationId,
            reservation.getRestaurant().getId(),
            adminId,
            UserType.ADMIN
        );
        
        return reservationMapper.toDTO(saved);
    }

    public ReservationDTO markReservationSeated(Long reservationId, Long adminId) {
        Status status = Status.SEATED;
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        reservation.setStatus(status);
        Reservation saved = reservationDAO.save(reservation);
        
        // 📝 AUDIT: Log seating by admin
        auditService.auditCustomerSeated(
            reservationId,
            reservation.getRestaurant().getId(),
            adminId,
            UserType.ADMIN
        );
        
        return reservationMapper.toDTO(saved);
    }

    public ReservationDTO findReservationById(Long reservationId) {
        return reservationDAO.findById(reservationId)
                .map(reservationMapper::toDTO)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
    }

    public void updateReservationStatus(Long reservationId, Status status) {
        updateReservationStatus(reservationId, status, null);
    }

    public ReservationDTO updateReservationStatus(Long reservationId, Status status, Long adminId) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        String oldStatus = reservation.getStatus().name();
        reservation.setStatus(status);
        Reservation saved = reservationDAO.save(reservation);
        
        // 📝 AUDIT: Log status change by admin
        auditService.auditReservationStatusChanged(
            reservationId,
            reservation.getRestaurant().getId(),
            adminId,
            UserType.ADMIN,
            oldStatus,
            status.name(),
            "Status updated by admin"
        );
        
        return reservationMapper.toDTO(saved);
    }

    public ReservationDTO modifyReservation(Long reservationId, AdminNewReservationDTO reservationDto) {
        return modifyReservation(reservationId, reservationDto, null);
    }

    public ReservationDTO modifyReservation(Long reservationId, AdminNewReservationDTO reservationDto, Long adminId) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));

        // Capture old values for audit
        String oldServiceName = reservation.getBookedServiceName();
        java.time.LocalDateTime oldDateTime = reservation.getReservationDateTime();
        Integer oldPax = reservation.getPax();
        
        // Update service if provided (also updates snapshot fields)
        if (reservationDto.getServiceId() != null) {
            com.application.common.persistence.model.reservation.Service newService = serviceDAO.findById(reservationDto.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
            
            com.application.common.persistence.model.reservation.ServiceVersion serviceVersion = serviceVersionDAO
                .findActiveVersionByServiceAndDate(
                    reservationDto.getServiceId(),
                    reservationDto.getReservationDateTime() != null ? 
                        reservationDto.getReservationDateTime().toLocalDate() : 
                        reservation.getDate()
                )
                .orElseThrow(() -> new IllegalArgumentException("No active service version found for the requested date"));
            
            reservation.setService(newService);
            // Update snapshot fields when service changes
            reservation.setBookedServiceName(newService.getName());
            reservation.setBookedSlotDuration(serviceVersion.getDuration());
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

        // ✅ VALIDATE BEFORE SAVING (service/date may have changed) - use Service
        reservationService.validateReservationDateAvailability(
            reservation.getRestaurant(),
            reservation.getDate(),
            reservation.getService()
        );

        Reservation saved = reservationDAO.save(reservation);
        
        // 📝 AUDIT: Log modification by admin
        String changes = buildChangesSummary(oldServiceName, oldDateTime, oldPax, saved);
        auditService.auditReservationUpdated(
            reservationId,
            saved.getRestaurant().getId(),
            adminId,
            UserType.ADMIN,
            "multiple_fields",
            null,
            changes,
            "Modified by admin"
        );
        
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

    // ─────────────────────────────────────────────────────────────────────────────
    // AUDIT HELPER METHODS
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Build a map of reservation data for audit logging
     */
    private java.util.Map<String, Object> buildAuditReservationData(Reservation reservation) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("reservationId", reservation.getId());
        data.put("serviceId", reservation.getService() != null ? reservation.getService().getId() : null);
        data.put("serviceName", reservation.getBookedServiceName());
        data.put("date", reservation.getDate() != null ? reservation.getDate().toString() : null);
        data.put("dateTime", reservation.getReservationDateTime() != null ? reservation.getReservationDateTime().toString() : null);
        data.put("pax", reservation.getPax());
        data.put("kids", reservation.getKids());
        data.put("status", reservation.getStatus() != null ? reservation.getStatus().name() : null);
        data.put("bookedSlotDuration", reservation.getBookedSlotDuration());
        return data;
    }

    /**
     * Build a summary of changes for audit logging
     */
    private String buildChangesSummary(String oldServiceName, java.time.LocalDateTime oldDateTime, Integer oldPax, Reservation newReservation) {
        StringBuilder sb = new StringBuilder();
        
        if (!java.util.Objects.equals(oldServiceName, newReservation.getBookedServiceName())) {
            sb.append("service:").append(oldServiceName).append("->").append(newReservation.getBookedServiceName()).append("; ");
        }
        if (!java.util.Objects.equals(oldDateTime, newReservation.getReservationDateTime())) {
            sb.append("dateTime:").append(oldDateTime).append("->").append(newReservation.getReservationDateTime()).append("; ");
        }
        if (!java.util.Objects.equals(oldPax, newReservation.getPax())) {
            sb.append("pax:").append(oldPax).append("->").append(newReservation.getPax()).append("; ");
        }
        
        return sb.length() > 0 ? sb.toString() : "No significant changes";
    }
}
