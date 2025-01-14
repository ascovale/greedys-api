package com.application.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.restaurant.ClosedDayDAO;
import com.application.persistence.dao.restaurant.ServiceDAO;
import com.application.persistence.dao.user.ReservationDAO;
import com.application.persistence.dao.user.UserDAO;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.reservation.Slot;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.RestaurantNotification;
import com.application.persistence.model.user.User;
import com.application.web.dto.post.NewReservationDTO;
import com.application.web.dto.get.ReservationDTO;

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
    private ServiceDAO serviceDAO;
    @Autowired
    private ClosedDayDAO closedDaysDAO;
    @Autowired
    private RestaurantNotificationService restaurantNotificationService;
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
    public Reservation createReservation(NewReservationDTO reservationDto) throws NoSuchElementException {
        Reservation reservation = new Reservation();
        reservation.setRestaurant(entityManager.getReference(Restaurant.class, reservationDto.getRestaurant_id()));
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
            // set User id
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
        //anche qui come verifichiamo che non sia uno iscritto come
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

}