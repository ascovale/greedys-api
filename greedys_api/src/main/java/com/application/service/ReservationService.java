package com.application.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.restaurant.ClosedDayDAO;
import com.application.persistence.dao.restaurant.ServiceDAO;
import com.application.persistence.dao.user.ReservationDAO;
import com.application.persistence.dao.user.ReservationLogDAO;
import com.application.persistence.dao.user.ReservationRequestDAO;
import com.application.persistence.dao.user.UserDAO;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.reservation.ReservationLog;
import com.application.persistence.model.reservation.ReservationRequest;
import com.application.persistence.model.reservation.Slot;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.RestaurantNotification;
import com.application.persistence.model.user.User;
import com.application.web.dto.get.ReservationDTO;
import com.application.web.dto.post.NewReservationDTO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service("reservationService")
@Transactional
public class ReservationService {

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private ReservationDAO reservationDAO;
    @Autowired
    private ReservationRequestDAO reservationRequestDAO;
    @Autowired
    private ReservationLogDAO reservationLogDAO;
    @Autowired
    private ServiceDAO serviceDAO;
    @Autowired
    private ClosedDayDAO closedDaysDAO;
    @Autowired
    private RestaurantNotificationService restaurantNotificationService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private UserDAO userDAO;

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
    public Reservation createReservation(NewReservationDTO reservationDto, Restaurant restaurant)
            throws NoSuchElementException {
        Reservation reservation = new Reservation();
        reservation.setRestaurant(restaurant);
        reservation.setPax(reservationDto.getPax());
        reservation.setKids(reservationDto.getKids());
        reservation.setNotes(reservationDto.getNotes());
        reservation.setDate(reservationDto.getReservationDay());
        reservation.setSlot(entityManager.getReference(Slot.class, reservationDto.getIdSlot()));
        reservation.setRejected(false);
        reservation.setAccepted(true);
        reservation.setNoShow(false);
        reservation.setCreationDate(LocalDate.now());
        if (reservationDto.isAnonymous()) {
            reservation.set_user_info(reservationDto.getClientUser());
        } else {
            // TODO: creare NewReservationDTO senza passare restaurant_id ma mettendo id
            // dell'utente che non c'è
            User user = userDAO.findByEmail(reservationDto.getClientUser().email());
            // securityService.hasRestaurantUserPermissionOnReservation(user.getRestaurantUser(),
            // reservation);
            
                notificationService.newReservation(user, reservation); // TODO creare la notifica per nuova prenotazione
                                                                       // inserita dal ri
        }
        return reservationDAO.save(reservation);
    }

    @Transactional
    public Reservation askForReservation(NewReservationDTO reservationDto, User user) throws NoSuchElementException {
        Reservation reservation = new Reservation();
        reservation.setRestaurant(entityManager.getReference(Restaurant.class, reservationDto.getRestaurant_id()));
        reservation.setPax(reservationDto.getPax());
        reservation.setKids(reservationDto.getKids());
        reservation.setNotes(reservationDto.getNotes());
        reservation.setDate(reservationDto.getReservationDay());
        reservation.setSlot(entityManager.getReference(Slot.class, reservationDto.getIdSlot()));
        reservation.setRejected(false);
        reservation.setAccepted(false);
        reservation.setNoShow(false);
        reservation.setCreationDate(LocalDate.now());
        restaurantNotificationService.createNotificationsForRestaurant(reservation.getRestaurant(),
                RestaurantNotification.Type.REQUEST);
        reservation.setUser(user);
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

    public Collection<ReservationDTO> findAllUserReservations(Long userId) {
        return reservationDAO.findByUser(userId).stream()
                .map(ReservationDTO::new).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> findAcceptedUserReservations(Long userId) {
        return reservationDAO.findByUserAndAccepted(userId).stream()
                .map(ReservationDTO::new).collect(Collectors.toList());
    }

    public Collection<ReservationDTO> findPendingUserReservations(Long userId) {
        return reservationDAO.findByUserAndPending(userId).stream()
                .map(ReservationDTO::new).collect(Collectors.toList());
    }

    @Transactional
    public Reservation modifyReservation(ReservationDTO dto) {
        // Logica per modificare la prenotazione
        Reservation reservation = reservationDAO.findById(dto.getId()).get();
        // Modifica i dettagli della prenotazione
        reservation.setPax(dto.getPax());
        reservation.setKids(dto.getKids());
        reservation.setNotes(dto.getNotes());
        // TODO: aggiungere la modifica della data
        // Salva la prenotazione modificata nel database
        return reservationDAO.save(reservation);
    }

    @Transactional
    @PreAuthorize("@securityService.hasPermissionOnReservation(#reservationId)")
    public void cancelReservation(Long reservationId) {
        Reservation reservation = reservationDAO.findById(reservationId).get();
        // TODO inserire cancellata la prenotazione

        reservation.setRejected(true);
        reservationDAO.save(reservation);
        // TODO DIRE CHE SE IL RUOLO è utente allora invia notifica al ristorante
        // TODO DIRE CHE SE IL RUOLO è ristoratore allora invia notifica all'utente
        notificationService.createdCancelNotification(reservation.getUser(), reservation);
    }

    @Transactional
    @PreAuthorize("@securityService.hasPermissionOnReservation(#reservationId)")
    public void modifyReservation(Long oldReservationId, NewReservationDTO dTO, User currentUser) {
        Reservation reservation = reservationDAO.findById(oldReservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        reservation.setPax(dTO.getPax());
        reservation.setKids(dTO.getKids());
        reservation.setNotes(dTO.getNotes());
        reservation.setDate(dTO.getReservationDay());
        reservation.setSlot(entityManager.getReference(Slot.class, dTO.getIdSlot()));
        reservationDAO.save(reservation);
        notificationService.createdCancelNotification(currentUser, reservation);
    }

    @Transactional
    @PreAuthorize("@securityService.hasPermissionOnReservation(#reservationId)")
    public void requestModifyReservation(Long oldReservationId, NewReservationDTO dTO, User currentUser) {
        // TODO Verificare che lo user abbia i permessi
        Reservation reservation = reservationDAO.findById(oldReservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found"));
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setPax(dTO.getPax());
        reservationRequest.setKids(dTO.getKids());
        reservationRequest.setNotes(dTO.getNotes());
        reservationRequest.setDate(dTO.getReservationDay());
        reservationRequest.setSlot(entityManager.getReference(Slot.class, dTO.getIdSlot()));
        reservationRequest.setUser(currentUser);
        reservationRequest.setCreationDate(LocalDate.now());
        reservationRequest.setReservation(reservation);
        reservationRequestDAO.save(reservationRequest);
    }

    @Transactional
    public void acceptReservationRequest(Long reservationRequestId, User user) {
        // TODO Verificare che lo user abbia i permessi
        ReservationRequest reservationRequest = reservationRequestDAO.findById(reservationRequestId)
                .orElseThrow(() -> new NoSuchElementException("Reservation request not found"));
        Reservation reservation = reservationRequest.getReservation();

        // Log the current reservation details
        reservationLogDAO.save(new ReservationLog(reservation));

        // Update reservation with the details from the request
        reservation.setPax(reservationRequest.getPax());
        reservation.setKids(reservationRequest.getKids());
        reservation.setNotes(reservationRequest.getNotes());
        reservation.setDate(reservationRequest.getDate());
        reservation.setSlot(reservationRequest.getSlot());

        // Save the updated reservation
        reservationDAO.save(reservation);

        // Delete the reservation request after accepting it
        reservationRequestDAO.delete(reservationRequest);
    }
}