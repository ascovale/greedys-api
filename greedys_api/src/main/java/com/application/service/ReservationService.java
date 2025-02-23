package com.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.customer.ReservationDAO;
import com.application.persistence.dao.customer.ReservationLogDAO;
import com.application.persistence.dao.customer.ReservationRequestDAO;
import com.application.persistence.dao.restaurant.ClosedDayDAO;
import com.application.persistence.dao.restaurant.RestaurantDAO;
import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.application.persistence.dao.restaurant.ServiceDAO;
import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.customer.Notification.Type;
import com.application.persistence.model.reservation.ClientInfo;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.reservation.ReservationLog;
import com.application.persistence.model.reservation.ReservationRequest;
import com.application.persistence.model.reservation.Slot;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RestaurantNotification;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.web.dto.get.ReservationDTO;
import com.application.web.dto.post.NewReservationDTO;
import com.application.web.dto.post.admin.AdminNewReservationDTO;
import com.application.web.dto.post.customer.CustomerNewReservationDTO;
import com.application.web.dto.post.restaurant.RestaurantNewReservationDTO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service("reservationService")
@Transactional
public class ReservationService {
    // TODO aggiungere controlli che la reservation è stata cancellata
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
    private final NotificationService customerNotificationService;
    private final RestaurantDAO restaurantDAO;
    private RestaurantUserDAO restaurantUserDAO;

    public ReservationService(ReservationDAO reservationDAO, ReservationRequestDAO reservationRequestDAO,
            ReservationLogDAO reservationLogDAO, ServiceDAO serviceDAO, ClosedDayDAO closedDaysDAO,
            RestaurantNotificationService restaurantNotificationService,
            NotificationService customerNotificationService, RestaurantDAO restaurantDAO,
            RestaurantUserDAO restaurantUserDAO) {
        this.reservationDAO = reservationDAO;
        this.reservationRequestDAO = reservationRequestDAO;
        this.reservationLogDAO = reservationLogDAO;
        this.serviceDAO = serviceDAO;
        this.closedDaysDAO = closedDaysDAO;
        this.restaurantNotificationService = restaurantNotificationService;
        this.customerNotificationService = customerNotificationService;
        this.restaurantDAO = restaurantDAO;
        this.restaurantUserDAO = restaurantUserDAO;
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
    public Reservation createRestaurantReservation(RestaurantNewReservationDTO reservationDto)
            throws NoSuchElementException {
        Restaurant restaurant = restaurantDAO.findById(reservationDto.getRestaurant_id())
                .orElseThrow(() -> new NoSuchElementException("Restaurant not found"));
        Slot slot = entityManager.getReference(Slot.class, reservationDto.getIdSlot());
        if (slot == null || slot.getDeleted()) {
            throw new IllegalArgumentException("Slot is either null or deleted");
        }
        Reservation reservation = new Reservation();
        reservation.setRestaurant(restaurant);
        reservation.setPax(reservationDto.getPax());
        reservation.setKids(reservationDto.getKids());
        reservation.setNotes(reservationDto.getNotes());
        reservation.setDate(reservationDto.getReservationDay());
        reservation.setSlot(slot);
        reservation.setRejected(false);
        reservation.setAccepted(true);
        reservation.setNoShow(false);
        reservation.setCreationDate(LocalDate.now());
        // TODO forse deve essere restaurantUser
        reservation.setCreator(getCurrentUser());
        if (reservationDto.isAnonymous()) {
            reservation.set_user_info(reservationDto.getClientUser());
        } else {
            Customer user = entityManager.getReference(Customer.class, reservationDto.getUser_id());
            reservation.setCustomer(user);
            customerNotificationService.createReservationNotification(reservation, Type.NEW_RESERVATION);
        }
        reservation = reservationDAO.save(reservation);

        return reservation;
    }

    @Transactional
    public Reservation createAdminReservation(AdminNewReservationDTO reservationDto) {
        Restaurant restaurant = restaurantDAO.findById(reservationDto.getRestaurant_id())
                .orElseThrow(() -> new NoSuchElementException("Restaurant not found"));
        Slot slot = entityManager.getReference(Slot.class, reservationDto.getIdSlot());
        if (slot == null || slot.getDeleted()) {
            throw new IllegalArgumentException("Slot is either null or deleted");
        }
        Reservation reservation = new Reservation();
        reservation.setRestaurant(restaurant);
        reservation.setPax(reservationDto.getPax());
        reservation.setKids(reservationDto.getKids());
        reservation.setNotes(reservationDto.getNotes());
        reservation.setDate(reservationDto.getReservationDay());
        reservation.setSlot(slot);
        reservation.setRejected(reservationDto.getRejected());
        reservation.setAccepted(reservationDto.getAccept());
        reservation.setNoShow(reservationDto.getNoShow());
        reservation.setCreationDate(LocalDate.now());
        reservationDAO.save(reservation);
        reservation.setCreator(getCurrentUser());
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.NEW_RESERVATION);
        if (reservationDto.isAnonymous()) {
            reservation.set_user_info(reservationDto.getClientUser());
        } else
            customerNotificationService.createReservationNotification(reservation, Type.NEW_RESERVATION);
        return reservation;
    }

    @Transactional
    public Reservation askForReservation(CustomerNewReservationDTO dTO) throws NoSuchElementException {
        Restaurant restaurant = restaurantDAO.findById(dTO.getRestaurant_id())
                .orElseThrow(() -> new NoSuchElementException("Restaurant not found"));
        Slot slot = entityManager.getReference(Slot.class, dTO.getIdSlot());
        if (slot == null || slot.getDeleted()) {
            throw new IllegalArgumentException("Slot is either null or deleted");
        }
        Reservation reservation = new Reservation();
        reservation.setRestaurant(entityManager.getReference(Restaurant.class, dTO.getRestaurant_id()));
        reservation.setPax(dTO.getPax());
        reservation.setKids(dTO.getKids());
        reservation.setNotes(dTO.getNotes());
        reservation.setDate(dTO.getReservationDay());
        reservation.setSlot(entityManager.getReference(Slot.class, dTO.getIdSlot()));
        reservation.setRejected(false);
        reservation.setAccepted(false);
        reservation.setRestaurant(restaurant);
        reservation.setNoShow(false);
        reservation.setCreationDate(LocalDate.now());
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.REQUEST);
        Customer user = getCurrentUser();
        reservation.setCreator(getCurrentUser());
        reservation.setCustomer(user);
        user.getReservations().add(reservation);
        return reservationDAO.save(reservation);
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

    public Collection<ReservationDTO> getAcceptedReservations(Long restaurant_id, LocalDate start, LocalDate end) {
        return reservationDAO.findByRestaurantAndDateBetweenAndAccepted(restaurant_id, start, end).stream()
                .map(ReservationDTO::new).collect(Collectors.toList());
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

    public Collection<ReservationDTO> findAllUserReservations(Long customerId) {
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
        // Logica per modificare la prenotazione
        Reservation reservation = reservationDAO.findById(dto.getId()).get();
        // Modifica i dettagli della prenotazione
        // M
        reservation.setPax(dto.getPax());
        reservation.setKids(dto.getKids());
        reservation.setNotes(dto.getNotes());
        reservation.setDate(dto.getReservationDay());
        // reservation.setSlot(entityManager.getReference(Slot.class, dto.getIdSlot()));
        // TODO: aggiungere la modifica della data
        // Salva la prenotazione modificata nel database
        return reservationDAO.save(reservation);
    }

    @Transactional
    public void adminDeleteReservation(Long reservationId) {
        Reservation reservation = reservationDAO.findById(reservationId).get();
        reservation.setDeleted(true);
        reservation.setCancelUser(getCurrentUser());
        reservationDAO.save(reservation);
        customerNotificationService.createReservationNotification(reservation, Type.CANCEL);
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.CANCEL);
    }

    @Transactional
    public void customerDeleteReservation(Long reservationId) {
        Reservation reservation = reservationDAO.findById(reservationId).get();
        reservation.setDeleted(true);
        reservation.setCancelUser(getCurrentUser());
        reservationDAO.save(reservation);
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.CANCEL);
    }

    @Transactional
    public void adminModifyReservation(Long oldReservationId, NewReservationDTO dTO, Customer currentUser) {
        Reservation reservation = reservationDAO.findById(oldReservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        reservationLogDAO.save(new ReservationLog(reservation, getCurrentUser()));
        reservation.setPax(dTO.getPax());
        reservation.setKids(dTO.getKids());
        reservation.setNotes(dTO.getNotes());
        reservation.setDate(dTO.getReservationDay());
        reservation.setSlot(entityManager.getReference(Slot.class, dTO.getIdSlot()));
        reservationLogDAO.save(new ReservationLog(reservation, getCurrentUser()));
        reservationDAO.save(reservation);
        customerNotificationService.createReservationNotification(reservation, Type.ALTERED);
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.ALTERED);
    }

    @Transactional
    public void restaurantModifyReservation(Long oldReservationId, NewReservationDTO dTO, Customer currentUser) {
        Reservation reservation = reservationDAO.findById(oldReservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        reservationLogDAO.save(new ReservationLog(reservation, getCurrentUser()));
        reservation.setPax(dTO.getPax());
        reservation.setKids(dTO.getKids());
        reservation.setNotes(dTO.getNotes());
        reservation.setDate(dTO.getReservationDay());
        reservation.setSlot(entityManager.getReference(Slot.class, dTO.getIdSlot()));
        reservationLogDAO.save(new ReservationLog(reservation, getCurrentUser()));
        reservationDAO.save(reservation);
        customerNotificationService.createReservationNotification(reservation, Type.ALTERED);
    }

    @Transactional
    public void requestModifyReservation(Long oldReservationId, CustomerNewReservationDTO dTO) {
        Customer currentUser = getCurrentUser();
        Reservation reservation = reservationDAO.findById(oldReservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
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
        reservationLogDAO.save(new ReservationLog(reservation, getCurrentUser(), reservationRequest));

        // Update reservation with the details from the request
        reservation.setPax(reservationRequest.getPax());
        reservation.setKids(reservationRequest.getKids());
        reservation.setNotes(reservationRequest.getNotes());
        reservation.setDate(reservationRequest.getDate());
        reservation.setSlot(reservationRequest.getSlot());

        // Save the updated reservation
        reservationDAO.save(reservation);

        // Notify the customer of the accepted reservation modification
        customerNotificationService.createReservationNotification(reservation, Type.ALTERED);

        // Delete the reservation request after accepting it
        reservationRequestDAO.delete(reservationRequest);
    }

    @Transactional
    public Reservation adminMarkReservationNoShow(Long reservationId, Boolean noShow) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        reservation.setNoShow(noShow);
        if (noShow) {
            reservation.setSeated(false);
        }
        reservationDAO.save(reservation);
        customerNotificationService.createReservationNotification(reservation, Type.NO_SHOW);
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.NO_SHOW);
        return reservation;
    }

    @Transactional
    public void markReservationNoShow(Long idRestaurant, Long reservationId, Boolean noShow) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        if (!reservation.getRestaurant().getId().equals(idRestaurant)) {
            throw new IllegalArgumentException("Reservation does not belong to the specified restaurant");
        }
        LocalDateTime now = LocalDateTime.now();
        if (reservation.isAfterNoShowTimeLimit(now)) {
            reservation.setNoShow(noShow);
            if (noShow)
                reservation.setSeated(false);
            reservationDAO.save(reservation);
            customerNotificationService.createReservationNotification(reservation, Type.NO_SHOW);
        } else {
            throw new IllegalStateException(
                    "The time limit for marking this reservation as no-show has not passed yet.");
        }
    }

    @Transactional
    public Reservation adminMarkReservationSeated(Long reservationId, Boolean seated) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        reservation.setSeated(seated);
        if (seated)
            reservation.setNoShow(false);
        reservationDAO.save(reservation);
        customerNotificationService.createReservationNotification(reservation, Type.SEATED);
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.SEATED);
        return reservation;
    }

    @Transactional
    public Reservation markReservationSeated(Long idRestaurant, Long reservationId, Boolean seated) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        if (!reservation.getRestaurant().getId().equals(idRestaurant)) {
            throw new IllegalArgumentException("Reservation does not belong to the specified restaurant");
        }
        reservation.setSeated(seated);
        if (seated)
            reservation.setNoShow(false);
        reservationDAO.save(reservation);
        customerNotificationService.createReservationNotification(reservation, Type.SEATED);
        return reservation;
    }

    @Transactional
    public Reservation adminMarkReservationAccepted(Long reservationId, Boolean accepted) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        reservation.setAccepted(accepted);
        if (accepted) {
            reservation.setRejected(false);
        }
        reservationDAO.save(reservation);
        customerNotificationService.createReservationNotification(reservation, Type.ACCEPTED);
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.ACCEPTED);
        return reservation;
    }

    @Transactional
    public Reservation markReservationAccepted(Long idRestaurant, Long reservationId, Boolean accepted) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        if (!reservation.getRestaurant().getId().equals(idRestaurant)) {
            throw new IllegalArgumentException("Reservation does not belong to the specified restaurant");
        }
        reservation.setAccepted(accepted);
        if (accepted)
            reservation.setRejected(false);
        reservationDAO.save(reservation);
        customerNotificationService.createReservationNotification(reservation, Type.SEATED);
        return reservation;
    }

    @Transactional
    public Reservation adminMarkReservationRejected(Long reservationId, Boolean rejected) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        reservation.setRejected(rejected);
        if (rejected) {
            reservation.setAccepted(false);
        }
        reservationDAO.save(reservation);
        customerNotificationService.createReservationNotification(reservation, Type.REJECTED);
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.REJECTED);
        return reservation;
    }

    @Transactional
    public Reservation markReservationRejected(Long idRestaurant, Long reservationId, Boolean rejected) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        if (!reservation.getRestaurant().getId().equals(idRestaurant)) {
            throw new IllegalArgumentException("Reservation does not belong to the specified restaurant");
        }
        reservation.setRejected(rejected);
        if (rejected)
            reservation.setAccepted(false);
        reservationDAO.save(reservation);
        customerNotificationService.createReservationNotification(reservation, Type.SEATED);
        return reservation;
    }

    private Customer getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Customer) {
            return ((Customer) principal);
        } else {
            System.out.println("Questo non dovrebbe succedere");
            return null;
        }
    }

    private RestaurantUser getCurrentRestaurantUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof RestaurantUser) {
            return ((RestaurantUser) principal);
        } else {
            System.out.println("Questo non dovrebbe succedere");
            return null;
        }
    }

    public void rejectReservationCreatedByAdminOrRestaurant(Long reservationId) {
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        reservationLogDAO.save(new ReservationLog(reservation, getCurrentUser()));
        // TODO scrivere il codice per dire se è stata creata o collegata dall'utente
        reservation.setCustomer(null);
        ClientInfo anonymousClientInfo = new ClientInfo("Anonymous", null, null);
        reservation.set_user_info(anonymousClientInfo);
        reservationDAO.save(reservation);
        customerNotificationService.createReservationNotification(reservation, Type.REJECTED);
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.USERNOTACCEPTEDRESERVATION);
    }

    public Page<ReservationDTO> getReservationsPageable(Long idRestaurant, LocalDate start, LocalDate end,
            Pageable pageable) {
        return reservationDAO.findByRestaurantAndDateBetweenAndAccepted(idRestaurant, start, end, pageable)
                .map(ReservationDTO::new);
    }

    public Collection<ReservationDTO> getReservationsFromRestaurantUser(Long idRestaurantUser, LocalDate start,
            LocalDate end) {
        RestaurantUser ruser = restaurantUserDAO.findById(idRestaurantUser)
                .orElseThrow(() -> new NoSuchElementException("Restaurant user not found"));
        return getReservations(ruser.getRestaurant().getId(), start, end);

    }

    public Collection<ReservationDTO> getAcceptedReservationsFromRestaurantUser(Long idRestaurantUser, LocalDate start,
            LocalDate end) {
        RestaurantUser ruser = restaurantUserDAO.findById(idRestaurantUser)
                .orElseThrow(() -> new NoSuchElementException("Restaurant user not found"));
        return getAcceptedReservations(ruser.getRestaurant().getId(), start, end);
    }

    public Page<ReservationDTO> getReservationsPageableFromRestaurantUser(Long idRestaurantUser, LocalDate start,
            LocalDate end, Pageable pageable) {

        RestaurantUser ruser = restaurantUserDAO.findById(idRestaurantUser)
                .orElseThrow(() -> new NoSuchElementException("Restaurant user not found"));
        return getReservationsPageable(ruser.getRestaurant().getId(), start, end, pageable);
    }

    public Collection<ReservationDTO> getPendingReservationsFromRestaurantUser(Long idRestaurantUser, LocalDate start,
            LocalDate end) {
        RestaurantUser ruser = restaurantUserDAO.findById(idRestaurantUser)
                .orElseThrow(() -> new NoSuchElementException("Restaurant user not found"));
        return getPendingReservations(ruser.getRestaurant().getId(), start, end);
    }

    public Collection<ReservationDTO> getPendingReservationsFromRestaurantUser(Long idRestaurantUser, LocalDate start) {
        RestaurantUser ruser = restaurantUserDAO.findById(idRestaurantUser)
                .orElseThrow(() -> new NoSuchElementException("Restaurant user not found"));
        return getPendingReservations(ruser.getRestaurant().getId(), start);
    }

    public Collection<ReservationDTO> getPendingReservationsFromRestaurantUser(Long idRestaurantUser) {
        RestaurantUser ruser = restaurantUserDAO.findById(idRestaurantUser)
                .orElseThrow(() -> new NoSuchElementException("Restaurant user not found"));
        return getPendingReservations(ruser.getRestaurant().getId());
    }

    public void markReservationAcceptedFromRestauantUser(Long idRestaurantUser, Long reservationId, Boolean accepted) {
        RestaurantUser ruser = restaurantUserDAO.findById(idRestaurantUser)
                .orElseThrow(() -> new NoSuchElementException("Restaurant user not found"));
        markReservationAccepted(ruser.getRestaurant().getId(), reservationId, accepted);
    }

    public void markReservationSeatedFromRestauantUser(Long idRestaurantUser, Long reservationId, Boolean seated) {
        RestaurantUser ruser = restaurantUserDAO.findById(idRestaurantUser)
                .orElseThrow(() -> new NoSuchElementException("Restaurant user not found"));
        markReservationSeated(ruser.getRestaurant().getId(), reservationId, seated);
    }

    public void markReservationNoShowFromRestauantUser(Long idRestaurantUser, Long reservationId, Boolean noShow) {
        RestaurantUser ruser = restaurantUserDAO.findById(idRestaurantUser)
                .orElseThrow(() -> new NoSuchElementException("Restaurant user not found"));
        markReservationNoShow(ruser.getRestaurant().getId(), reservationId, noShow);
    }

    public void markReservationRejectedFromRestauantUser(Long idRestaurantUser, Long reservationId, Boolean rejected) {
        RestaurantUser ruser = restaurantUserDAO.findById(idRestaurantUser)
                .orElseThrow(() -> new NoSuchElementException("Restaurant user not found"));
        markReservationRejected(ruser.getRestaurant().getId(), reservationId, rejected);
    }

    public ReservationDTO getReservation(Long reservationId) {
        
        Reservation reservation = reservationDAO.findById(reservationId).orElseThrow(() -> new NoSuchElementException("Reservation not found"));
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
        Restaurant restaurant = getCurrentRestaurantUser().getRestaurant();
        return reservationDAO.findByRestaurantAndDateBetween(restaurant.getId(), start, end).stream()
            .map(ReservationDTO::new)
            .collect(Collectors.toList());
    }

}