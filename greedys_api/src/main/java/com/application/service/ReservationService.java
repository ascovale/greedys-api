package com.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.customer.CustomerDAO;
import com.application.persistence.dao.customer.ReservationDAO;
import com.application.persistence.dao.customer.ReservationLogDAO;
import com.application.persistence.dao.customer.ReservationRequestDAO;
import com.application.persistence.dao.restaurant.ClosedDayDAO;
import com.application.persistence.dao.restaurant.RUserDAO;
import com.application.persistence.dao.restaurant.RestaurantDAO;
import com.application.persistence.dao.restaurant.ServiceDAO;
import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.reservation.ReservationLog;
import com.application.persistence.model.reservation.ReservationRequest;
import com.application.persistence.model.reservation.Slot;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RUser;
import com.application.persistence.model.restaurant.user.RestaurantNotification;
import com.application.service.notification.CustomerNotificationService;
import com.application.web.dto.get.ReservationDTO;
import com.application.web.dto.post.NewBaseReservationDTO;
import com.application.web.dto.post.NewReservationDTO;
import com.application.web.dto.post.admin.AdminNewReservationDTO;
import com.application.web.dto.post.customer.CustomerNewReservationDTO;
import com.application.web.dto.post.restaurant.RestaurantNewReservationDTO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service("reservationService")
@Transactional
public class ReservationService {
    // TODO verificare quando si crea che lo slot non sia stato cancellato
    // TODO verificare che la data della prenotazione sia maggiore o uguale alla
    // data attuale
    // TODO verificare che il servizio non sia deleted

    @PersistenceContext
    private EntityManager entityManager;
    private final ReservationDAO reservationDAO;
    private final ReservationRequestDAO reservationRequestDAO;
    private final ReservationLogDAO reservationLogDAO;
    private final ServiceDAO serviceDAO;
    private final ClosedDayDAO closedDaysDAO;
    private final RestaurantNotificationService restaurantNotificationService;
    private final CustomerNotificationService customerNotificationService;
    private final RestaurantDAO restaurantDAO;
    private final RUserDAO RUserDAO;
    private final CustomerDAO customerDAO;
    Logger logger = LoggerFactory.getLogger(ReservationService.class);

    public ReservationService(ReservationDAO reservationDAO, ReservationRequestDAO reservationRequestDAO,
            ReservationLogDAO reservationLogDAO, ServiceDAO serviceDAO, ClosedDayDAO closedDaysDAO,
            RestaurantNotificationService restaurantNotificationService,
            CustomerNotificationService customerNotificationService, RestaurantDAO restaurantDAO,
            RUserDAO RUserDAO,
            CustomerDAO customerDAO) {
        this.reservationDAO = reservationDAO;
        this.reservationRequestDAO = reservationRequestDAO;
        this.reservationLogDAO = reservationLogDAO;
        this.serviceDAO = serviceDAO;
        this.closedDaysDAO = closedDaysDAO;
        this.restaurantNotificationService = restaurantNotificationService;
        this.customerNotificationService = customerNotificationService;
        this.restaurantDAO = restaurantDAO;
        this.RUserDAO = RUserDAO;
        this.customerDAO = customerDAO;
    }

    @Transactional
    public void save(Reservation reservation) {
        reservationDAO.save(reservation);
    }

    @Transactional
    public Reservation findAll(long id) {
        return reservationDAO.findById(id).orElse(null);
    }

    public Reservation findById(Long id) {
        return reservationDAO.findById(id).orElse(null);
    }

    @Transactional
    public ReservationDTO createReservation(RestaurantNewReservationDTO reservationDto)
            throws NoSuchElementException {
        Long restaurantId = getCurrentRUser().getRestaurant().getId();
        Restaurant restaurant = restaurantDAO.findById(restaurantId)
                .orElseThrow(() -> new NoSuchElementException("Restaurant not found"));
        Slot slot = entityManager.getReference(Slot.class, reservationDto.getIdSlot());
        if (slot == null || slot.getDeleted()) {
            throw new IllegalArgumentException("Slot is either null or deleted");
        }
        Reservation reservation = buildReservationFromBaseDTO(reservationDto, slot);
        reservation.setRestaurant(restaurant);
        reservation.setRejected(false);
        
        reservation.setAccepted(true);
        reservation.setNoShow(false);
        reservation.setCreator(getCurrentRUser());
        Customer user = entityManager.getReference(Customer.class, reservationDto.getCustomerId());
        reservation.setCustomer(user);
        //TO DO
        ////customerNotificationService.createReservationNotification(reservation, Type.NEW_RESERVATION);
        reservation = reservationDAO.save(reservation);
        return new ReservationDTO(reservation);
    }

    @Transactional
    public ReservationDTO createReservation(AdminNewReservationDTO reservationDto) {
        Restaurant restaurant = restaurantDAO.findById(reservationDto.getRestaurant_id())
                .orElseThrow(() -> new NoSuchElementException("Restaurant not found"));
        Slot slot = entityManager.getReference(Slot.class, reservationDto.getIdSlot());
        if (slot == null || slot.getDeleted()) {
            throw new IllegalArgumentException("Slot is either null or deleted");
        }
        Reservation reservation = buildReservationFromBaseDTO(reservationDto, slot);
        reservation.setRestaurant(restaurant);
        reservation.setRejected(reservationDto.getRejected());
        reservation.setAccepted(reservationDto.getAccept());
        reservation.setNoShow(reservationDto.getNoShow());
        reservationDAO.save(reservation);
        // reservation.setCreator(getCurrentUser());
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.NEW_RESERVATION);
        //customerNotificationService.createReservationNotification(reservation, Type.NEW_RESERVATION);
        return new ReservationDTO(reservation);
    }

    @Transactional
    public ReservationDTO createReservation(CustomerNewReservationDTO dTO) throws NoSuchElementException {
        Restaurant restaurant = restaurantDAO.findById(dTO.getRestaurantId())
                .orElseThrow(() -> new NoSuchElementException("Restaurant not found"));
        Slot slot = entityManager.getReference(Slot.class, dTO.getIdSlot());
        if (slot == null || slot.getDeleted()) {
            throw new IllegalArgumentException("Slot is either null or deleted");
        }
        Reservation reservation = buildReservationFromBaseDTO(dTO, slot);
        reservation.setRestaurant(restaurant);
        reservation.setRejected(false);
        reservation.setAccepted(false);
        reservation.setNoShow(false);
        Customer customer = customerDAO.findById(getCurrentCustomer().getId()).get();
        reservation.setCreator(customer);
        reservation.setCustomer(customer);
        Reservation r = reservationDAO.save(reservation);
        try {
            restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                    RestaurantNotification.Type.REQUEST);
        } catch (Exception e) {
            System.err.println("Failed to send restaurant email: " + e.getMessage());
        }
        return new ReservationDTO(r);
    }

    public List<LocalDate> findNotAvailableDays(Long idRestaurant) {
        List<LocalDate> days = serviceDAO.findClosedOrFullDays(idRestaurant);
        if (days == null) {
            days = new ArrayList<>();
        }
        days.addAll(serviceDAO.findClosedOrFullDays(idRestaurant));
        return days;
    }

    public List<LocalDate> findClosedDays(Long idRestaurant) {
        return closedDaysDAO.findUpcomingClosedDay();
    }

    public List<Reservation> getDayReservations(Restaurant restaurant, LocalDate date) {
        return reservationDAO.findDayReservation(restaurant.getId(), date);
    }

    public Collection<ReservationDTO> getReservations(Long restaurant_id, LocalDate start, LocalDate end) {
        return reservationDAO.findByRestaurantAndDateBetween(restaurant_id, start, end).stream()
                .map(ReservationDTO::new).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> getAcceptedReservations(LocalDate start, LocalDate end) {
        return getAcceptedReservations(getCurrentRUser().getRestaurant().getId(), start, end);
    }

    public Collection<ReservationDTO> getPendingReservations(Long restaurant_id, LocalDate start) {
        return reservationDAO.findByRestaurantAndDateAndPending(restaurant_id, start).stream()
                .map(ReservationDTO::new).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> getPendingReservations(Long restaurant_id, LocalDate start, LocalDate end) {
        return reservationDAO.findByRestaurantAndDateBetweenAndPending(restaurant_id, start, end).stream()
                .map(ReservationDTO::new).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> getPendingReservations(Long restaurant_id) {
        return reservationDAO.findByRestaurantIdAndPending(restaurant_id).stream()
                .map(ReservationDTO::new).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> findAllCustomerReservations(Long customerId) {
        return reservationDAO.findByCustomer(customerId).stream()
                .map(ReservationDTO::new).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> findAcceptedCustomerReservations(Long customerId) {
        return reservationDAO.findByCustomerAndAccepted(customerId).stream()
                .map(ReservationDTO::new).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> findPendingCustomerReservations(Long customerId) {
        return reservationDAO.findByCustomerAndPending(customerId).stream()
                .map(ReservationDTO::new).collect(Collectors.toList());
    }

    @Transactional
    public Reservation modifyReservation(ReservationDTO dto) {
        Reservation reservation = reservationDAO.findById(dto.getId()).get();
        if (reservation.getDeleted() != null && reservation.getDeleted()) {
            throw new IllegalStateException("Cannot modify a deleted reservation");
        }
        if (reservation.getLockedByAdmin() != null && reservation.getLockedByAdmin() && !isCurrentUserAdmin()) {
            throw new IllegalStateException("Reservation is locked by admin and cannot be modified");
        }
        reservation.setPax(dto.getPax());
        reservation.setKids(dto.getKids());
        reservation.setNotes(dto.getNotes());
        reservation.setDate(dto.getReservationDay());
        // reservation.setSlot(entityManager.getReference(Slot.class, dto.getIdSlot()));
        // TODO: aggiungere la modifica della data
        return reservationDAO.save(reservation);
    }

    @Transactional
    public void adminDeleteReservation(Long reservationId) {
        Reservation reservation = reservationDAO.findById(reservationId).get();
        if (reservation.getDeleted() != null && reservation.getDeleted()) {
            throw new IllegalStateException("Cannot delete a reservation that is already deleted");
        }
        reservation.setDeleted(true);
        // reservation.setCancelUser(getCurrentUser());
        reservationDAO.save(reservation);
        //customerNotificationService.createReservationNotification(reservation, Type.CANCEL);
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.CANCEL);
    }

    @Transactional
    public void customerDeleteReservation(Long reservationId) {
        Reservation reservation = reservationDAO.findById(reservationId).get();
        if (reservation.getDeleted() != null && reservation.getDeleted()) {
            throw new IllegalStateException("Cannot delete a reservation that is already deleted");
        }
        reservation.setDeleted(true);
        // TODO: implementare il cancelUser e dire chi ha cancellato cosi anche con chi
        // ha creato ecc
        // reservation.setCancelUser(getCurrentUser());
        reservationDAO.save(reservation);
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.CANCEL);
    }

    @Transactional
    private Reservation modifyReservation(Long oldId, NewReservationDTO dTO) {
        Reservation reservation = reservationDAO.findById(oldId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        if (reservation.getDeleted() != null && reservation.getDeleted()) {
            throw new IllegalStateException("Cannot modify a deleted reservation");
        }
        if (reservation.getLockedByAdmin() != null && reservation.getLockedByAdmin() && !isCurrentUserAdmin()) {
            throw new IllegalStateException("Reservation is locked by admin and cannot be modified");
        }
        reservationLogDAO.save(new ReservationLog(reservation, getCurrentCustomer()));
        reservation.setPax(dTO.getPax());
        reservation.setKids(dTO.getKids());
        reservation.setNotes(dTO.getNotes());
        reservation.setDate(dTO.getReservationDay());
        reservation.setSlot(entityManager.getReference(Slot.class, dTO.getIdSlot()));
        return reservation;
    }

    public void adminModifyReservation(Long oldReservationId, NewReservationDTO dTO, Customer currentUser) {
        Reservation reservation = modifyReservation(oldReservationId, dTO);
        //eventuali altre modifiche da admin
        reservationDAO.save(reservation);
        reservationLogDAO.save(new ReservationLog(reservation, getCurrentCustomer()));
        //customerNotificationService.createReservationNotification(reservation, Type.ALTERED);
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.ALTERED);
            
    }

    public void restaurantModifyReservation(Long oldReservationId, NewReservationDTO dTO, Customer currentUser) {
        Reservation reservation = modifyReservation(oldReservationId, dTO);
        reservationDAO.save(reservation);
        reservationLogDAO.save(new ReservationLog(reservation, getCurrentCustomer()));
        //customerNotificationService.createReservationNotification(reservation, Type.ALTERED);
    }

    private boolean isCurrentUserAdmin() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof com.application.persistence.model.admin.Admin;
    }

    @Transactional
    public void requestModifyReservation(Long oldReservationId, CustomerNewReservationDTO dTO) {
        Customer currentUser = getCurrentCustomer();
        Reservation reservation = reservationDAO.findById(oldReservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        if (reservation.getDeleted() != null && reservation.getDeleted()) {
            throw new IllegalStateException("Cannot modify a deleted reservation");
        }
        if (reservation.getLockedByAdmin() != null && reservation.getLockedByAdmin() && !isCurrentUserAdmin()) {
            throw new IllegalStateException("Reservation is locked by admin and cannot be modified");
        }
        if (reservation.getRejected() == true || reservation.getSeated() == true || reservation.getDeleted() == true || reservation.getNoShow() == true) {
            throw new IllegalStateException("Cannot modify a reservation with status REJECTED, SEATED, DELETED, or NO SHOW");
        }
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setPax(dTO.getPax());
        reservationRequest.setKids(dTO.getKids());
        reservationRequest.setNotes(dTO.getNotes());
        reservationRequest.setDate(dTO.getReservationDay());
        reservationRequest.setSlot(entityManager.getReference(Slot.class, dTO.getIdSlot()));
        reservationRequest.setCustomer(currentUser);
        reservationRequest.setCreationDate(LocalDate.now());
        reservationRequest.setReservation(reservation);
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.MODIFICATION);
        reservationRequestDAO.save(reservationRequest);
    }

    @Transactional
    public void restaurantAcceptReservatioModifyRequest(Long reservationRequestId) {
        ReservationRequest reservationRequest = reservationRequestDAO.findById(reservationRequestId)
                .orElseThrow(() -> new NoSuchElementException("Reservation request not found"));
        Reservation reservation = reservationRequest.getReservation();

        // Log the current reservation details
        reservationLogDAO.save(new ReservationLog(reservation, getCurrentCustomer(), reservationRequest));

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

    @Transactional
    public Reservation adminMarkReservationNoShow(Long reservationId, Boolean noShow) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        if (reservation.getDeleted() !=null && reservation.getDeleted()) {
            throw new IllegalStateException("Cannot mark as no-show a deleted reservation");
        }
        reservation.setNoShow(noShow);
        if (noShow) {
            reservation.setSeated(false);
        }
        reservationDAO.save(reservation);
        //customerNotificationService.createReservationNotification(reservation, Type.NO_SHOW);
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.NO_SHOW);
        return reservation;
    }

    @Transactional
    public void markReservationNoShow(Long idRestaurant, Long reservationId, Boolean noShow) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        if (reservation.getDeleted() != null && reservation.getDeleted()) {
            throw new IllegalStateException("Cannot mark as no-show a deleted reservation");
        }
        if (!reservation.getRestaurant().getId().equals(idRestaurant)) {
            throw new IllegalArgumentException("Reservation does not belong to the specified restaurant");
        }
        LocalDateTime now = LocalDateTime.now();
        if (reservation.isAfterNoShowTimeLimit(now)) {
            reservation.setNoShow(noShow);
            if (noShow)
                reservation.setSeated(false);
            reservationDAO.save(reservation);
            //customerNotificationService.createReservationNotification(reservation, Type.NO_SHOW);
        } else {
            throw new IllegalStateException(
                    "The time limit for marking this reservation as no-show has not passed yet.");
        }
    }

    @Transactional
    public Reservation adminMarkReservationSeated(Long reservationId, Boolean seated) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        if (reservation.getDeleted() != null && reservation.getDeleted()) {
            throw new IllegalStateException("Cannot mark as seated a deleted reservation");
        }
        reservation.setSeated(seated);
        if (seated)
            reservation.setNoShow(false);
        reservationDAO.save(reservation);
        //customerNotificationService.createReservationNotification(reservation, Type.SEATED);
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.SEATED);
        return reservation;
    }

    @Transactional
    public Reservation markReservationSeated(Long idRestaurant, Long reservationId, Boolean seated) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        if (reservation.getDeleted() != null && reservation.getDeleted()) {
            throw new IllegalStateException("Cannot mark as seated a deleted reservation");
        }
        if (!reservation.getRestaurant().getId().equals(idRestaurant)) {
            throw new IllegalArgumentException("Reservation does not belong to the specified restaurant");
        }
        reservation.setSeated(seated);
        if (seated)
            reservation.setNoShow(false);
        reservationDAO.save(reservation);
        //customerNotificationService.createReservationNotification(reservation, Type.SEATED);
        return reservation;
    }

    @Transactional
    public Reservation adminMarkReservationAccepted(Long reservationId, Boolean accepted) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        if (reservation.getDeleted() != null && reservation.getDeleted()) {
            throw new IllegalStateException("Cannot accept a deleted reservation");
        }
        reservation.setAccepted(accepted);
        if (accepted) {
            reservation.setRejected(false);
        }
        reservationDAO.save(reservation);
        //customerNotificationService.createReservationNotification(reservation, Type.ACCEPTED);
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.ACCEPTED);
        return reservation;
    }

    @Transactional
    public Reservation markReservationAccepted(Long idRestaurant, Long reservationId, Boolean accepted) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        if (reservation.getDeleted() != null && reservation.getDeleted()) {
            throw new IllegalStateException("Cannot accept a deleted reservation");
        }
        if (!reservation.getRestaurant().getId().equals(idRestaurant)) {
            throw new IllegalArgumentException("Reservation does not belong to the specified restaurant");
        }
        reservation.setAccepted(accepted);
        if (accepted)
            reservation.setRejected(false);
        reservationDAO.save(reservation);
        //customerNotificationService.createReservationNotification(reservation, Type.SEATED);
        return reservation;
    }

    @Transactional
    public Reservation adminMarkReservationRejected(Long reservationId, Boolean rejected) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        if (reservation.getDeleted() != null && reservation.getDeleted()) {
            throw new IllegalStateException("Cannot reject a deleted reservation");
        }
        reservation.setRejected(rejected);
        if (rejected) {
            reservation.setAccepted(false);
        }
        reservationDAO.save(reservation);
        //customerNotificationService.createReservationNotification(reservation, Type.REJECTED);
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.REJECTED);
        return reservation;
    }

    @Transactional
    public Reservation markReservationRejected(Long idRestaurant, Long reservationId, Boolean rejected) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        if (reservation.getDeleted() != null && reservation.getDeleted()) {
            throw new IllegalStateException("Cannot reject a deleted reservation");
        }
        if (!reservation.getRestaurant().getId().equals(idRestaurant)) {
            throw new IllegalArgumentException("Reservation does not belong to the specified restaurant");
        }
        reservation.setRejected(rejected);
        if (rejected)
            reservation.setAccepted(false);
        reservationDAO.save(reservation);
        //customerNotificationService.createReservationNotification(reservation, Type.SEATED);
        return reservation;
    }

    private Customer getCurrentCustomer() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Customer) {
            return (Customer) principal;
        } else {
            logger.error("L'utente corrente non è un Customer. Principal: {}", principal.getClass().getName());
            throw new IllegalStateException("L'utente corrente non è un Customer");
        }
    }

    private RUser getCurrentRUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof RUser) {
            return ((RUser) principal);
        } else {
            logger.info("Questo non dovrebbe succedere");
            return null;
        }
    }

    public void rejectReservationCreatedByAdminOrRestaurant(Long reservationId) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        if (reservation.getDeleted() != null && reservation.getDeleted()) {
            throw new IllegalStateException("Cannot reject a deleted reservation");
        }
        reservationLogDAO.save(new ReservationLog(reservation, getCurrentCustomer()));
        // TODO scrivere il codice per dire se è stata creata o collegata dall'utente
        reservation.setCustomer(null);
        reservationDAO.save(reservation);
        //customerNotificationService.createReservationNotification(reservation, Type.REJECTED);
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.USERNOTACCEPTEDRESERVATION);
    }

    public Page<ReservationDTO> getReservationsPageable(Long idRestaurant, LocalDate start, LocalDate end,
            Pageable pageable) {
        return reservationDAO.findByRestaurantAndDateBetweenAndAccepted(idRestaurant, start, end, pageable)
                .map(ReservationDTO::new);
    }

    public Collection<ReservationDTO> getReservationsFromRUser(Long idRUser, LocalDate start,
            LocalDate end) {
        RUser ruser = RUserDAO.findById(idRUser)
                .orElseThrow(() -> new NoSuchElementException("Restaurant user not found"));
        return getReservations(ruser.getRestaurant().getId(), start, end);

    }

    public Page<ReservationDTO> getReservationsPageable(LocalDate start,
            LocalDate end, Pageable pageable) {

        return getReservationsPageable(getCurrentRUser().getRestaurant().getId(), start, end, pageable);
    }

    public Collection<ReservationDTO> getPendingReservationsFromRUser(Long idRUser, LocalDate start,
            LocalDate end) {
        RUser ruser = RUserDAO.findById(idRUser)
                .orElseThrow(() -> new NoSuchElementException("Restaurant user not found"));
        return getPendingReservations(ruser.getRestaurant().getId(), start, end);
    }

    public Collection<ReservationDTO> getPendingReservationsFromRUser(Long idRUser, LocalDate start) {
        RUser ruser = RUserDAO.findById(idRUser)
                .orElseThrow(() -> new NoSuchElementException("Restaurant user not found"));
        return getPendingReservations(ruser.getRestaurant().getId(), start);
    }

    public Collection<ReservationDTO> getPendingReservationsFromRUser(Long idRUser) {
        RUser ruser = RUserDAO.findById(idRUser)
                .orElseThrow(() -> new NoSuchElementException("Restaurant user not found"));
        return getPendingReservations(ruser.getRestaurant().getId());
    }

    public void markReservationAcceptedFromRestauantUser(Long idRUser, Long reservationId, Boolean accepted) {
        RUser ruser = RUserDAO.findById(idRUser)
                .orElseThrow(() -> new NoSuchElementException("Restaurant user not found"));
        markReservationAccepted(ruser.getRestaurant().getId(), reservationId, accepted);
    }

    public void markReservationSeatedFromRestauantUser(Long idRUser, Long reservationId, Boolean seated) {
        RUser ruser = RUserDAO.findById(idRUser)
                .orElseThrow(() -> new NoSuchElementException("Restaurant user not found"));
        markReservationSeated(ruser.getRestaurant().getId(), reservationId, seated);
    }

    public void markReservationNoShowFromRestauantUser(Long idRUser, Long reservationId, Boolean noShow) {
        RUser ruser = RUserDAO.findById(idRUser)
                .orElseThrow(() -> new NoSuchElementException("Restaurant user not found"));
        markReservationNoShow(ruser.getRestaurant().getId(), reservationId, noShow);
    }

    public void markReservationRejectedFromRestauantUser(Long idRUser, Long reservationId, Boolean rejected) {
        RUser ruser = RUserDAO.findById(idRUser)
                .orElseThrow(() -> new NoSuchElementException("Restaurant user not found"));
        markReservationRejected(ruser.getRestaurant().getId(), reservationId, rejected);
    }

    public ReservationDTO getReservation(Long reservationId) {

        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        return new ReservationDTO(reservation);
    }

    public List<ReservationDTO> getCustomerReservations(Long customerId) {
        return reservationDAO.findByCustomer(customerId).stream()
                .map(ReservationDTO::new)
                .collect(Collectors.toList());
    }

    public Page<ReservationDTO> getCustomerReservationsPaginated(Long customerId, Pageable pageable) {
        Page<Reservation> page = reservationDAO.findByCustomer(customerId, pageable);
        return page.map(ReservationDTO::new);
    }

    public Collection<ReservationDTO> getRestaurantReservations(LocalDate start, LocalDate end) {
        Restaurant restaurant = getCurrentRUser().getRestaurant();
        return reservationDAO.findByRestaurantAndDateBetween(restaurant.getId(), start, end).stream()
                .map(ReservationDTO::new)
                .collect(Collectors.toList());
    }

    public Collection<ReservationDTO> getAcceptedReservations(Long idRestaurant, LocalDate start, LocalDate end) {
        return reservationDAO.findByRestaurantAndDateBetweenAndAccepted(idRestaurant, start, end).stream()
                .map(ReservationDTO::new).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> getPendingReservations(LocalDate start, LocalDate end) {
        return getPendingReservations(getCurrentRUser().getRestaurant().getId(), start, end);
    }

    public Collection<ReservationDTO> getPendingReservations(LocalDate start) {
        return getPendingReservations(getCurrentRUser().getRestaurant().getId(), start);
    }

    public Page<ReservationDTO> getPendingReservationsPageable(Long idRestaurant, LocalDate start, LocalDate end,
            Pageable pageable) {
        return reservationDAO.findByRestaurantAndDateBetweenAndPending(idRestaurant, start, end, pageable)
                .map(ReservationDTO::new);
    }

    public Page<ReservationDTO> getPendingReservationsPageable(Long idRestaurant, LocalDate start, Pageable pageable) {
        return reservationDAO.findByRestaurantAndDateAndPending(idRestaurant, start, pageable)
                .map(ReservationDTO::new);
    }

    public Page<ReservationDTO> getPendingReservationsPageable(Long idRestaurant, Pageable pageable) {
        return reservationDAO.findByRestaurantIdAndPending(idRestaurant, pageable)
                .map(ReservationDTO::new);
    }

    public ReservationDTO findReservationById(Long reservationId) {
        Reservation reservation = reservationDAO.findById(reservationId)
            .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        return new ReservationDTO(reservation);
    }

    private Reservation buildReservationFromBaseDTO(NewBaseReservationDTO dto, Slot slot) {
        Reservation reservation = new Reservation();
        reservation.setPax(dto.getPax());
        reservation.setKids(dto.getKids());
        reservation.setNotes(dto.getNotes());
        reservation.setDate(dto.getReservationDay());
        reservation.setSlot(slot);
        reservation.setCreationDate(LocalDate.now());
        return reservation;
    }

}