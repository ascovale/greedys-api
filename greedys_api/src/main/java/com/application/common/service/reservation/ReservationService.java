package com.application.common.service.reservation;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.customer.dao.CustomerDAO;
import com.application.customer.dao.ReservationDAO;
import com.application.customer.dao.ReservationRequestDAO;
import com.application.customer.service.notification.CustomerNotificationService;
import com.application.restaurant.dao.ClosedDayDAO;
import com.application.restaurant.dao.RUserDAO;
import com.application.restaurant.dao.RestaurantDAO;
import com.application.restaurant.dao.ServiceDAO;
import com.application.restaurant.service.RestaurantNotificationService;

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