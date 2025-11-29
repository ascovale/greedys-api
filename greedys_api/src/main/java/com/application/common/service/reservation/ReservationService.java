package com.application.common.service.reservation;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.mapper.ReservationMapper;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.reservation.ReservationRequest;
import com.application.common.persistence.model.notification.EventOutbox;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.common.persistence.dao.EventOutboxDAO;
import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.customer.persistence.dao.ReservationRequestDAO;
import com.application.customer.persistence.model.Customer;
import com.application.restaurant.persistence.dao.ClosedDayDAO;
import com.application.restaurant.persistence.dao.ServiceDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.service.agenda.RestaurantAgendaService;
import com.application.restaurant.service.ReservationWebSocketPublisher;
import com.application.restaurant.web.dto.reservation.RestaurantNewReservationDTO;
import com.application.restaurant.web.dto.reservation.RestaurantReservationWithExistingCustomerDTO;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationDAO reservationDAO;
    private final ReservationRequestDAO reservationRequestDAO;
    private final ServiceDAO serviceDAO;
    private final ClosedDayDAO closedDaysDAO;
    private final CustomerDAO customerDAO;
    private final ReservationMapper reservationMapper;
    private final RestaurantAgendaService restaurantAgendaService;
    private final ReservationWebSocketPublisher webSocketPublisher;
    private final EventOutboxDAO eventOutboxDAO;

    public void save(Reservation reservation) {
        // Save the reservation
        reservationDAO.save(reservation);
    }

    /**
     * Create new reservation (core method - saves reservation only)
     * Callers are responsible for creating the appropriate EventOutbox
     */
    public Reservation createNewReservation(Reservation reservation) {
        Reservation savedReservation = reservationDAO.save(reservation);
        return savedReservation;
    }

    /**
     * ⭐ Create new reservation WITH validation
     * 
     * Validates that:
     * - Service is not deleted
     * - Reservation date is not in the past
     * - Other restaurant/service availability checks (TODO)
     * 
     * Throws IllegalArgumentException if validation fails.
     * 
     * @param reservation The reservation to create
     * @return Saved reservation entity
     * @throws IllegalArgumentException if validation fails
     */
    public Reservation createNewReservationWithValidation(Reservation reservation) {
        // Validate that the reservation date is available
        validateReservationDateAvailability(
            reservation.getRestaurant(),
            reservation.getDate(),
            reservation.getService().getId()
        );
        
        // If validation passed, create the reservation
        return createNewReservation(reservation);
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

    /**
     * ⭐ Accept a reservation with optional table number and notes.
     * 
     * @param reservationId ID of the reservation to accept
     * @param tableNumber Optional table number
     * @param notes Optional notes for the reservation
     * @return Updated reservation DTO
     */
    public ReservationDTO acceptReservation(Long reservationId, Integer tableNumber, String notes) {
        Reservation reservation = reservationDAO.findByIdWithRestaurant(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        
        reservation.setStatus(Reservation.Status.ACCEPTED);
        if (tableNumber != null) {
            reservation.setTableNumber(tableNumber);
        }
        if (notes != null) {
            reservation.setNotes(notes);
        }
        
        Reservation saved = reservationDAO.save(reservation);
        
        // Publish WebSocket event for reservation accepted
        webSocketPublisher.publishReservationAccepted(saved);
        
        return reservationMapper.toDTO(saved);
    }

    /**
     * ⭐ Reject a reservation with optional reason.
     * 
     * @param reservationId ID of the reservation to reject
     * @param reason Optional reason for rejection
     * @return Updated reservation DTO
     */
    public ReservationDTO rejectReservation(Long reservationId, String reason) {
        Reservation reservation = reservationDAO.findByIdWithRestaurant(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        
        reservation.setStatus(Reservation.Status.REJECTED);
        if (reason != null) {
            reservation.setRejectionReason(reason);
        }
        
        Reservation saved = reservationDAO.save(reservation);
        
        // Publish WebSocket event for reservation rejected
        webSocketPublisher.publishReservationRejected(saved);
        
        return reservationMapper.toDTO(saved);
    }

    public Collection<ReservationDTO> getReservations(Long restaurantId, LocalDate start, LocalDate end) {
        LocalDateTime startDT = start.atStartOfDay();
        LocalDateTime endDT = end.plusDays(1).atStartOfDay();
        return reservationDAO.findByRestaurantIdAndReservationDatetimeBetween(restaurantId, startDT, endDT, PageRequest.of(0, Integer.MAX_VALUE)).stream()
                .map(reservationMapper::toDTO).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> getAcceptedReservations(Long restaurantId, LocalDate start, LocalDate end) {
        LocalDateTime startDT = start.atStartOfDay();
        LocalDateTime endDT = end.plusDays(1).atStartOfDay();
        return reservationDAO.findConfirmedReservationsByRestaurantAndDay(restaurantId, startDT, endDT, PageRequest.of(0, Integer.MAX_VALUE)).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Collection<ReservationDTO> getPendingReservations(Long restaurantId, LocalDate start, LocalDate end) {
        LocalDateTime startDT = start.atStartOfDay();
        LocalDateTime endDT = end.plusDays(1).atStartOfDay();
        if (restaurantId == null) {
            throw new IllegalArgumentException("restaurantId cannot be null");
        }
        return reservationDAO.findPendingReservationsByRestaurantAndDay(restaurantId, startDT, endDT, PageRequest.of(0, Integer.MAX_VALUE)).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Page<ReservationDTO> getReservationsPageable(Long restaurantId, LocalDate start, LocalDate end,
            Pageable pageable) {
        LocalDateTime startDT = start.atStartOfDay();
        LocalDateTime endDT = end.plusDays(1).atStartOfDay();
        return reservationDAO.findByRestaurantIdAndReservationDatetimeBetween(restaurantId, startDT, endDT, pageable)
                .map(reservationMapper::toDTO);
    }

    public Page<ReservationDTO> getPendingReservationsPageable(Long restaurantId, LocalDate start, LocalDate end,
            Pageable pageable) {
        LocalDateTime startDT = start.atStartOfDay();
        LocalDateTime endDT = end.plusDays(1).atStartOfDay();
        if (restaurantId == null) {
            throw new IllegalArgumentException("restaurantId cannot be null");
        }
        return reservationDAO.findPendingReservationsByRestaurantAndDay(restaurantId, startDT, endDT, pageable)
                .map(reservationMapper::toDTO);
    }

    public Page<ReservationDTO> getAcceptedReservationsPageable(Long restaurantId, LocalDate start, LocalDate end,
            Pageable pageable) {
        LocalDateTime startDT = start.atStartOfDay();
        LocalDateTime endDT = end.plusDays(1).atStartOfDay();
        if (restaurantId == null) {
            throw new IllegalArgumentException("restaurantId cannot be null");
        }
        return reservationDAO.findConfirmedReservationsByRestaurantAndDay(restaurantId, startDT, endDT, pageable)
                .map(reservationMapper::toDTO);
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

        // ✅ VALIDATE BEFORE SAVING
        validateReservationDateAvailability(
            reservation.getRestaurant(),
            reservation.getDate(),
            reservation.getService().getId()
        );

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

        // ✅ VALIDATE BEFORE SAVING
        validateReservationDateAvailability(
            reservation.getRestaurant(),
            reservation.getDate(),
            reservation.getService().getId()
        );

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
        
        // Get service (replaces slot lookup)
        com.application.common.persistence.model.reservation.Service service = serviceDAO.findById(reservationDto.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        if (service.getDeleted()) {
            throw new IllegalArgumentException("Service is deleted");
        }

        // Create or find customer (UNREGISTERED if new)
        Customer customer = createOrUpdateCustomerFromReservation(
                reservationDto.getUserName(), 
                reservationDto.getUserEmail(), 
                reservationDto.getUserPhoneNumber());

        Reservation reservation = Reservation.builder()
                .userName(reservationDto.getUserName())
                .pax(reservationDto.getPax())
                .kids(reservationDto.getKids())
                .notes(reservationDto.getNotes())
                .reservationDateTime(reservationDto.getReservationDateTime())
                .service(service)
                .restaurant(restaurant)
                .customer(customer)
                .createdByUserType(Reservation.UserType.RESTAURANT_USER) // 🔧 FIX: aggiunto campo mancante per auditing  
                .status(Reservation.Status.ACCEPTED)
                .build();
        
        // ✅ VALIDATE RESERVATION DATE BEFORE SAVING
        reservation = createNewReservationWithValidation(reservation);
        
        // 📌 CREATE EVENT_OUTBOX: Restaurant staff created reservation
        // ⭐ INCLUDES initiated_by=RESTAURANT for intelligent routing to notification.customer queue (PERSONAL)
        createRestaurantReservationCreatedEvent(reservation);
        
        // 🎯 INTEGRAZIONE AGENDA: Aggiungi automaticamente il cliente all'agenda del ristorante
        try {
            restaurantAgendaService.addToAgendaOnReservation(
                customer.getId(), 
                restaurant.getId()
            );
            log.debug("Customer {} successfully added to restaurant {} agenda", customer.getEmail(), restaurant.getId());
        } catch (Exception e) {
            // Non blocchiamo la prenotazione se l'aggiunta all'agenda fallisce
            log.warn("Failed to add customer {} to restaurant {} agenda: {}", 
                customer.getEmail(), restaurant.getId(), e.getMessage());
        }
        
        return reservationMapper.toDTO(reservation);
    }

    /**
     * Create a reservation with existing customer from restaurant agenda
     */
    public ReservationDTO createReservationWithExistingCustomer(
            RestaurantReservationWithExistingCustomerDTO reservationDto, Restaurant restaurant) {
        log.debug("Creating reservation with existing customer: {}", reservationDto);
        
        // Get service (replaces slot lookup)
        com.application.common.persistence.model.reservation.Service service = serviceDAO.findById(reservationDto.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        if (service.getDeleted()) {
            throw new IllegalArgumentException("Service is deleted");
        }

        // Get existing customer
        Customer customer = customerDAO.findById(reservationDto.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + reservationDto.getCustomerId()));

        Reservation reservation = Reservation.builder()
                .userName(reservationDto.getUserName())
                .pax(reservationDto.getPax())
                .kids(reservationDto.getKids())
                .notes(reservationDto.getNotes())
                .reservationDateTime(reservationDto.getReservationDateTime())
                .service(service)
                .restaurant(restaurant)
                .customer(customer)
                .createdByUserType(Reservation.UserType.RESTAURANT_USER)
                .status(Reservation.Status.ACCEPTED)
                .build();
        
        // ✅ VALIDATE RESERVATION DATE BEFORE SAVING
        reservation = createNewReservationWithValidation(reservation);
        
        // 📌 CREATE EVENT_OUTBOX: Restaurant staff created reservation with existing customer
        // ⭐ INCLUDES initiated_by=RESTAURANT for intelligent routing to notification.customer queue (PERSONAL)
        createRestaurantReservationCreatedEvent(reservation);
        
        // 🎯 INTEGRAZIONE AGENDA: Customer già esistente nell'agenda - aggiorna info prenotazione
        try {
            restaurantAgendaService.addToAgendaOnReservation(
                customer.getId(), 
                restaurant.getId()
            );
            log.debug("Customer {} agenda updated for restaurant {}", customer.getEmail(), restaurant.getId());
        } catch (Exception e) {
            // Non blocchiamo la prenotazione se l'aggiornamento agenda fallisce
            log.warn("Failed to update customer {} agenda for restaurant {}: {}", 
                customer.getEmail(), restaurant.getId(), e.getMessage());
        }
        
        return reservationMapper.toDTO(reservation);
    }

    @Cacheable(value = "closedDays", key = "#idRestaurant")
    public List<LocalDate> findNotAvailableDays(Long idRestaurant) {
        return serviceDAO.findClosedOrFullDays(idRestaurant);
    }

    @Cacheable(value = "closedDays", key = "'global'")
    public List<LocalDate> findClosedDays(Long idRestaurant) {
        return closedDaysDAO.findUpcomingClosedDay();
    }

    /**
     * ⭐ VALIDATE RESERVATION DATE: Check if restaurant/service is available on requested date
     * 
     * Implements core validation checks:
     * 1. ✅ Restaurant is ENABLED (not deleted/closed)
     * 2. ✅ Service not deleted (critical)
     * 3. ✅ Reservation date >= today (critical)
     * 4. TODO: Restaurant closure days (ClosedDayDAO)
     * 5. TODO: Service-specific closure days (if service has custom closures)
     * 6. TODO: Restaurant availability settings (e.g., max covers per day)
     * 
     * Status: ESSENTIAL checks active, future checks ready as TODO
     * 
     * @param restaurant The restaurant entity
     * @param reservationDate The requested reservation date (LocalDate)
     * @param serviceId The service ID (Lunch, Dinner, etc.)
     * @return true if reservation is allowed on this date, false if closed/unavailable
     */
    public boolean validateReservationDateAvailability(Restaurant restaurant, LocalDate reservationDate, Long serviceId) {
        log.debug("🔍 Validating reservation date: restaurant={}, date={}, service={}", 
            restaurant.getId(), reservationDate, serviceId);
        
        try {
            // ✅ ESSENTIAL: Check if restaurant is ENABLED (not deleted or closed)
            if (restaurant.getStatus() == null || 
                restaurant.getStatus() == Restaurant.Status.DELETED || 
                restaurant.getStatus() == Restaurant.Status.DISABLED ||
                restaurant.getStatus() == Restaurant.Status.CLOSED ||
                restaurant.getStatus() == Restaurant.Status.TEMPORARILY_CLOSED) {
                log.warn("❌ Restaurant is not available: status={}", restaurant.getStatus());
                throw new IllegalArgumentException("Restaurant is no longer accepting reservations");
            }
            
            // ✅ ESSENTIAL: Check if reservation date is in the past
            if (reservationDate.isBefore(LocalDate.now())) {
                log.warn("❌ Reservation date is in the past: {}", reservationDate);
                throw new IllegalArgumentException("Reservation date cannot be in the past");
            }
            
            // ✅ ESSENTIAL: Check if service exists and is active (not deleted)
            com.application.common.persistence.model.reservation.Service service = serviceDAO.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
            
            if (service.getDeleted()) {
                log.warn("❌ Service is deleted: {}", serviceId);
                throw new IllegalArgumentException("Service is no longer available");
            }
            
            // TODO: Check if restaurant has closure days defined for this date
            // List<LocalDate> closedDays = findClosedDays(restaurant.getId());
            // if (closedDays != null && closedDays.contains(reservationDate)) {
            //     log.warn("❌ Restaurant is closed on date: {} for restaurant {}", reservationDate, restaurant.getId());
            //     return false;
            // }
            
            // TODO: Check if service has specific closure days (if service closure mechanism is implemented)
            // boolean serviceIsClosed = checkServiceClosureByDate(serviceId, reservationDate);
            // if (serviceIsClosed) {
            //     log.warn("❌ Service {} is closed on date: {}", serviceId, reservationDate);
            //     return false;
            // }
            
            // TODO: Check restaurant capacity/availability rules for the date
            // boolean isCapacityAvailable = checkRestaurantCapacity(restaurant.getId(), reservationDate, pax);
            // if (!isCapacityAvailable) {
            //     log.warn("❌ Restaurant at capacity on date: {} for restaurant {}", reservationDate, restaurant.getId());
            //     return false;
            // }
            
            log.debug("✅ Reservation date validation PASSED");
            return true;
            
        } catch (IllegalArgumentException iae) {
            // Re-throw business validation errors
            log.warn("⚠️ Reservation validation failed: {}", iae.getMessage());
            throw iae;
        } catch (Exception e) {
            log.error("⚠️ Error during reservation date validation: {}", e.getMessage(), e);
            // In case of unexpected errors, be permissive to not block legitimate reservations
            return true;
        }
    }

    public List<ReservationDTO> getDayReservations(Restaurant restaurant, LocalDate date) {
        LocalDateTime startDT = date.atStartOfDay();
        LocalDateTime endDT = date.plusDays(1).atStartOfDay();
        return reservationDAO.findByRestaurantIdAndReservationDatetimeBetween(restaurant.getId(), startDT, endDT, PageRequest.of(0, Integer.MAX_VALUE)).stream()
                .map(reservationMapper::toDTO).collect(Collectors.toList());
    }

    /**
     * Get all reservations for a customer across all restaurants
     */
    public Collection<ReservationDTO> getCustomerReservations(Long customerId) {
        return reservationDAO.findByCustomerIdOrderByReservationDatetimeDesc(customerId, PageRequest.of(0, Integer.MAX_VALUE)).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all reservations for a customer at a specific restaurant
     */
    public Collection<ReservationDTO> getCustomerReservationsByRestaurant(Long customerId, Long restaurantId) {
        return reservationDAO.findByCustomerIdAndRestaurantId(customerId, restaurantId).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
    }


    /**
     * Create or update customer from reservation data.
     * Used when making reservations via restaurant (phone bookings).
     * 
     * @param userName Customer name
     * @param userEmail Customer email (optional)
     * @param userPhoneNumber Customer phone number (key identifier)
     * @return Customer entity (existing or newly created)
     */
    private Customer createOrUpdateCustomerFromReservation(String userName, String userEmail, String userPhoneNumber) {
        log.debug("Creating/updating customer from reservation: name={}, email={}, phone={}", 
                userName, userEmail, userPhoneNumber);

        // Validate input - at least name and phone required
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required for reservation");
        }
        if (userPhoneNumber == null || userPhoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer phone number is required for reservation");
        }

        Customer customer = null;

        // First, try to find existing customer by phone number
        customer = customerDAO.findByPhoneNumber(userPhoneNumber);
        
        if (customer != null) {
            log.debug("Found existing customer by phone: {}", customer.getId());
            
            // Update customer info if needed
            boolean needsUpdate = false;
            
            // Update name if different (handle case: reservation with wife's name but husband's phone)
            if (!userName.equals(customer.getName())) {
                log.info("Updating customer name from '{}' to '{}' for customer {}", 
                        customer.getName(), userName, customer.getId());
                customer.setName(userName);
                needsUpdate = true;
            }
            
            // Add email if missing and provided
            if (customer.getEmail() == null && userEmail != null && !userEmail.trim().isEmpty()) {
                // Check if email already exists for another customer
                Customer existingByEmail = customerDAO.findByEmail(userEmail);
                if (existingByEmail == null) {
                    customer.setEmail(userEmail);
                    needsUpdate = true;
                    log.info("Added email '{}' to existing customer {}", userEmail, customer.getId());
                } else if (!existingByEmail.getId().equals(customer.getId())) {
                    log.warn("Email '{}' already belongs to another customer {}. Cannot update customer {}",
                            userEmail, existingByEmail.getId(), customer.getId());
                }
            }
            
            if (needsUpdate) {
                customer = customerDAO.save(customer);
            }
        } else {
            // No customer found by phone, check by email if provided
            if (userEmail != null && !userEmail.trim().isEmpty()) {
                customer = customerDAO.findByEmail(userEmail);
                
                if (customer != null) {
                    log.debug("Found existing customer by email: {}", customer.getId());
                    
                    // Add phone number if missing
                    if (customer.getPhoneNumber() == null) {
                        customer.setPhoneNumber(userPhoneNumber);
                        customer = customerDAO.save(customer);
                        log.info("Added phone '{}' to existing customer {}", userPhoneNumber, customer.getId());
                    }
                }
            }
            
            // Create new UNREGISTERED customer if still not found
            if (customer == null) {
                log.info("Creating new UNREGISTERED customer: name={}, email={}, phone={}", 
                        userName, userEmail, userPhoneNumber);
                
                // Parse first and last name (simple split)
                String[] nameParts = userName.trim().split("\\s+", 2);
                String firstName = nameParts[0];
                String lastName = nameParts.length > 1 ? nameParts[1] : "";
                
                // Generate a temporary random password for UNREGISTERED customers
                // (database requires non-null password, even for non-registered customers)
                String tempPassword = java.util.UUID.randomUUID().toString().substring(0, 20);
                
                customer = Customer.builder()
                        .name(firstName)
                        .surname(lastName)
                        .email(userEmail != null && !userEmail.trim().isEmpty() ? userEmail : null)
                        .phoneNumber(userPhoneNumber)
                        .password(tempPassword) // Temporary random password (not used for authentication)
                        .status(Customer.Status.VERIFY_TOKEN)
                        .roles(new java.util.ArrayList<>()) // No roles for customers created from reservations
                        .build();
                
                customer = customerDAO.save(customer);
                log.info("Created new UNREGISTERED customer with ID: {} (temporary password generated)", customer.getId());
            }
        }
        
        return customer;
    }

    /**
     * Create RESERVATION_CREATED event (restaurant staff created reservation)
     * Notifies: CUSTOMER ONLY (confirmation of their reservation)
     * 
     * ⭐ INCLUDES initiated_by=RESTAURANT for intelligent routing to notification.customer queue (PERSONAL)
     */
    private void createRestaurantReservationCreatedEvent(Reservation reservation) {
        String eventId = "RESERVATION_CREATED_" + reservation.getId() + "_" + System.currentTimeMillis();
        String payload = buildReservationPayload(reservation);
        
        EventOutbox eventOutbox = EventOutbox.builder()
            .eventId(eventId)
            .eventType("RESERVATION_CREATED")
            .aggregateType("RESTAURANT")
            .aggregateId(reservation.getId())
            .payload(payload)
            .status(EventOutbox.Status.PENDING)
            .build();
        
        eventOutboxDAO.save(eventOutbox);
        
        log.info("✅ Created EventOutbox RESERVATION_CREATED: eventId={}, reservationId={}, aggregateType=RESTAURANT, status=PENDING", 
            eventId, reservation.getId());
    }

    /**
     * Build JSON payload for EventOutbox
     * 
     * ⭐ INCLUDES initiated_by=RESTAURANT for intelligent routing
     * EventOutboxOrchestrator uses this to route to:
     * - notification.customer (PERSONAL customer notifications)
     * instead of restaurant team notifications
     */
    private String buildReservationPayload(Reservation reservation) {
        Long customerId = reservation.getCustomer() != null ? reservation.getCustomer().getId() : null;
        String customerEmail = reservation.getCustomer() != null ? reservation.getCustomer().getEmail() : "anonymous";
        Long restaurantId = reservation.getRestaurant().getId();
        Integer kids = reservation.getKids() != null ? reservation.getKids() : 0;
        String notes = reservation.getNotes() != null ? reservation.getNotes().replace("\"", "\\\"") : "";
        
        return String.format(
            "{\"reservationId\":%d,\"customerId\":%s,\"restaurantId\":%d,\"email\":\"%s\",\"date\":\"%s\",\"pax\":%d,\"kids\":%d,\"notes\":\"%s\",\"initiated_by\":\"RESTAURANT\"}",
            reservation.getId(),
            customerId != null ? customerId : "null",
            restaurantId,
            customerEmail,
            reservation.getDate().toString(),
            reservation.getPax(),
            kids,
            notes
        );
    }

}
