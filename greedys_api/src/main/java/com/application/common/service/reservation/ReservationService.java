package com.application.common.service.reservation;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.mapper.ReservationMapper;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.reservation.ReservationRequest;
import com.application.common.persistence.model.reservation.Slot;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.common.persistence.mapper.Mapper.Weekday;
import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.customer.persistence.dao.ReservationRequestDAO;
import com.application.customer.persistence.model.Customer;
import com.application.restaurant.persistence.dao.ClosedDayDAO;
import com.application.restaurant.persistence.dao.ServiceDAO;
import com.application.restaurant.persistence.dao.SlotDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.service.agenda.RestaurantAgendaService;
import com.application.restaurant.service.ReservationWebSocketPublisher;
import com.application.restaurant.web.dto.reservation.RestaurantNewReservationDTO;
import com.application.restaurant.web.dto.reservation.RestaurantReservationWithExistingCustomerDTO;

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
    private final CustomerDAO customerDAO;
    private final ReservationMapper reservationMapper;
    private final RestaurantAgendaService restaurantAgendaService;
    private final ReservationWebSocketPublisher webSocketPublisher;

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
        return reservationDAO.findByRestaurantAndDateBetween(restaurantId, start, end).stream()
                .map(reservationMapper::toDTO).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> getAcceptedReservations(Long restaurantId, LocalDate start, LocalDate end) {
        Reservation.Status status = Reservation.Status.ACCEPTED;
        return reservationDAO.findByRestaurantAndDateBetweenAndStatus(restaurantId, start, end, status).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Collection<ReservationDTO> getPendingReservations(Long restaurantId, LocalDate start, LocalDate end) {
        Reservation.Status status = Reservation.Status.NOT_ACCEPTED;
        if (restaurantId == null) {
            throw new IllegalArgumentException("restaurantId cannot be null");
        }
        if (start != null && end != null) {
            return reservationDAO.findByRestaurantAndDateBetweenAndStatus(restaurantId, start, end, status).stream()
                    .map(reservationMapper::toDTO)
                    .collect(Collectors.toList());
        } else if (start != null) {
            return reservationDAO.findByRestaurantAndDateAndStatus(restaurantId, start, status).stream()
                    .map(reservationMapper::toDTO)
                    .collect(Collectors.toList());
        } else {
            return reservationDAO.findByRestaurantIdAndStatus(restaurantId, status).stream()
                    .map(reservationMapper::toDTO)
                    .collect(Collectors.toList());
        }
    }

    public Page<ReservationDTO> getReservationsPageable(Long restaurantId, LocalDate start, LocalDate end,
            Pageable pageable) {
        return reservationDAO.findReservationsByRestaurantAndDateRange(restaurantId, start, end, pageable)
                .map(reservationMapper::toDTO);
    }

    public Page<ReservationDTO> getPendingReservationsPageable(Long restaurantId, LocalDate start, LocalDate end,
            Pageable pageable) {
        Reservation.Status status = Reservation.Status.NOT_ACCEPTED;
        if (restaurantId == null) {
            throw new IllegalArgumentException("restaurantId cannot be null");
        }
        // TODO: Implement actual query or use existing methods
        return reservationDAO.findByRestaurantAndDateBetweenAndStatus(restaurantId, start, end, status, pageable)
                .map(reservationMapper::toDTO);
    }

    public Page<ReservationDTO> getAcceptedReservationsPageable(Long restaurantId, LocalDate start, LocalDate end,
            Pageable pageable) {
        Reservation.Status status = Reservation.Status.ACCEPTED;
        if (restaurantId == null) {
            throw new IllegalArgumentException("restaurantId cannot be null");
        }
        return reservationDAO.findByRestaurantAndDateBetweenAndStatus(restaurantId, start, end, status, pageable)
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

        if (slot.getDeleted()) {
            throw new IllegalArgumentException("Slot is deleted");
        }

        // Validate that the reservation day matches the slot's weekday
        DayOfWeek reservationDayOfWeek = reservationDto.getReservationDay().getDayOfWeek();
        Weekday reservationWeekday = convertDayOfWeekToWeekday(reservationDayOfWeek);
        if (!reservationWeekday.equals(slot.getWeekday())) {
            throw new IllegalArgumentException(
                String.format("Reservation day %s (%s) is not available for this service", 
                    reservationDto.getReservationDay(), 
                    reservationWeekday));
        }

        // Create or find customer (UNREGISTERED if new)
        Customer customer = createOrUpdateCustomerFromReservation(
                reservationDto.getUserName(), 
                reservationDto.getUserEmail(), 
                reservationDto.getUserPhoneNumber());

        var reservation = Reservation.builder()
                .userName(reservationDto.getUserName())
                .pax(reservationDto.getPax())
                .kids(reservationDto.getKids())
                .notes(reservationDto.getNotes())
                .date(reservationDto.getReservationDay())
                .slot(slot)
                .restaurant(restaurant)
                .customer(customer)
                .createdByUserType(Reservation.UserType.RESTAURANT_USER) // 🔧 FIX: aggiunto campo mancante per auditing  
                .status(Reservation.Status.ACCEPTED)
                .build();
        reservation = reservationDAO.save(reservation);
        
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

        // Get existing customer
        Customer customer = customerDAO.findById(reservationDto.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + reservationDto.getCustomerId()));

        Reservation reservation = Reservation.builder()
                .userName(reservationDto.getUserName())
                .pax(reservationDto.getPax())
                .kids(reservationDto.getKids())
                .notes(reservationDto.getNotes())
                .date(reservationDto.getReservationDay())
                .slot(slot)
                .restaurant(restaurant)
                .customer(customer)
                .createdByUserType(Reservation.UserType.RESTAURANT_USER)
                .status(Reservation.Status.ACCEPTED)
                .build();
        reservation = reservationDAO.save(reservation);
        
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

    public List<ReservationDTO> getDayReservations(Restaurant restaurant, LocalDate date) {
        return reservationDAO.findDayReservation(restaurant.getId(), date).stream()
                .map(reservationMapper::toDTO).collect(Collectors.toList());
    }

    /**
     * Get all reservations for a customer across all restaurants
     */
    public Collection<ReservationDTO> getCustomerReservations(Long customerId) {
        return reservationDAO.findByCustomer(customerId).stream()
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

}
