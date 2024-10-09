package com.application.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.Restaurant.ClosedDayDAO;
import com.application.persistence.dao.Restaurant.ServiceDAO;
import com.application.persistence.dao.user.ReservationDAO;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.reservation.Slot;
import com.application.persistence.model.restaurant.Restaurant;
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

	/*
	@Autowired
	private SlotDAO slotDAO;

	@Autowired
	private NotificationService notificationService;
	*/
	@Autowired
	private ClosedDayDAO closedDaysDAO;

	@Transactional
	public void save(Reservation reservation) {
		reservationDAO.save(reservation);
	}

	@Transactional
	public Reservation findAll(long id) {
		Optional<Reservation> opt = reservationDAO.findById(id);
		if (!opt.isPresent())
			return null;
		return opt.get();
	}

	 
	public Reservation findById(Long id) {
		return reservationDAO.findById(id).get();
	}

	 
	@Transactional
	public Reservation createReservation(NewReservationDTO reservationDto)
			throws NoSuchElementException {

		Reservation reservation = new Reservation();
		reservation.setRestaurant(entityManager.getReference(Restaurant.class, reservationDto.getRestaurant_id()));
		reservation.setPax(reservationDto.getPax());
		reservation.setKids(reservationDto.getKids());
		reservation.setNotes(reservationDto.getNotes());
		reservation.setKids(reservationDto.getKids());
		reservation.setDate(reservationDto.getReservationDay());
		reservation.setSlot(entityManager.getReference(Slot.class, reservationDto.getIdSlot()));
		reservation.setRejected(false);
		reservation.setAccepted(true);
		reservation.setNoShow(false);
		reservation.setCreationDate(LocalDate.now());
		if (reservationDto.isAnonymous()) {
			//set anonymous user
			reservation.set_user_info(reservationDto.getClientUser());
		}
		else {
			//set User id
			
		}
		// QUA PRESUME CHE IL CLIENTE SIA QUELLO DELLA EMAIL
		// ClientUser user = clientUserDAO.findByEmail(userEmail);
		// reservation.setClientUser(user);
		// Notification rn = notificationService.createNotification();
		return reservationDAO.save(reservation);
	}

	@Transactional
	public Reservation askForReservation(NewReservationDTO reservationDto)
			throws NoSuchElementException {

		Reservation reservation = new Reservation();
		reservation.setRestaurant(entityManager.getReference(Restaurant.class, reservationDto.getRestaurant_id()));
		reservation.setPax(reservationDto.getPax());
		reservation.setKids(reservationDto.getKids());
		reservation.setNotes(reservationDto.getNotes());
		reservation.setKids(reservationDto.getKids());
		reservation.setDate(reservationDto.getReservationDay());
		reservation.setSlot(entityManager.getReference(Slot.class, reservationDto.getIdSlot()));
		reservation.setRejected(false);
		reservation.setAccepted(false);
		reservation.setNoShow(false);
		reservation.setCreationDate(LocalDate.now());
		if (reservationDto.isAnonymous()) {
			//set anonymous user
			reservation.set_user_info(reservationDto.getClientUser());
		}
		else {
			//set User id
			
		}
		// QUA PRESUME CHE IL CLIENTE SIA QUELLO DELLA EMAIL
		// ClientUser user = clientUserDAO.findByEmail(userEmail);
		// reservation.setClientUser(user);
		// Notification rn = notificationService.createNotification();
		return reservationDAO.save(reservation);
	}

	 
	public List<LocalDate> findNotAvailableDays(Long idRestaurant) {
		List<LocalDate> days = serviceDAO.findClosedOrFullDays(idRestaurant);
		if (days == null)
			days = new ArrayList<LocalDate>();
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
						.map(res -> new ReservationDTO(res)).collect(Collectors.toList());
    }

	public Collection<ReservationDTO> getAcceptedReservations(Long restaurant_id, LocalDate start, LocalDate end) {
		return reservationDAO.findByRestaurantAndDateBetweenAndAccepted(restaurant_id, start, end).stream()
				.map(res -> new ReservationDTO(res)).collect(Collectors.toList());
	}

	public Collection<ReservationDTO> getPendingReservations(Long restaurant_id, LocalDate start) {
		return reservationDAO.findByRestaurantAndDateAndPending(restaurant_id, start).stream()
				.map(res -> new ReservationDTO(res)).collect(Collectors.toList());
	}

	public Collection<ReservationDTO> getPendingReservations(Long restaurant_id, LocalDate start, LocalDate end) {
		return reservationDAO.findByRestaurantAndDateBetweenAndPending(restaurant_id, start, end).stream()
				.map(res -> new ReservationDTO(res)).collect(Collectors.toList());
	}

	public Collection<ReservationDTO> getPendingReservations(Long restaurant_id) {
		return reservationDAO.findByRestaurantIdAndPending(restaurant_id).stream()
				.map(res -> new ReservationDTO(res)).collect(Collectors.toList());
	}

}
