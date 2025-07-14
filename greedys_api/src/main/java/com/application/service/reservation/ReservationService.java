package com.application.service.reservation;

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
import com.application.persistence.dao.customer.ReservationRequestDAO;
import com.application.persistence.dao.restaurant.ClosedDayDAO;
import com.application.persistence.dao.restaurant.RUserDAO;
import com.application.persistence.dao.restaurant.RestaurantDAO;
import com.application.persistence.dao.restaurant.ServiceDAO;
import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.reservation.ReservationRequest;
import com.application.persistence.model.reservation.Slot;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RUser;
import com.application.service.RestaurantNotificationService;
import com.application.service.notification.CustomerNotificationService;
import com.application.web.dto.get.ReservationDTO;
import com.application.web.dto.post.NewBaseReservationDTO;
import com.application.web.dto.post.NewReservationDTO;
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
    private final ServiceDAO serviceDAO;
    private final ClosedDayDAO closedDaysDAO;
    private final RestaurantNotificationService restaurantNotificationService;
    private final CustomerNotificationService customerNotificationService;
    private final RestaurantDAO restaurantDAO;
    private final RUserDAO RUserDAO;
    private final CustomerDAO customerDAO;
    Logger logger = LoggerFactory.getLogger(ReservationService.class);

    public ReservationService(ReservationDAO reservationDAO, ReservationRequestDAO reservationRequestDAO,
             ServiceDAO serviceDAO, ClosedDayDAO closedDaysDAO,
            RestaurantNotificationService restaurantNotificationService,
            CustomerNotificationService customerNotificationService, RestaurantDAO restaurantDAO,
            RUserDAO RUserDAO,
            CustomerDAO customerDAO) {
        this.reservationDAO = reservationDAO;
        this.reservationRequestDAO = reservationRequestDAO;
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
    public List<Reservation> findAll(long id) {
        return reservationDAO.findAll();
    }

    public Reservation findById(Long id) {
        return reservationDAO.findById(id).orElse(null);
    }
}