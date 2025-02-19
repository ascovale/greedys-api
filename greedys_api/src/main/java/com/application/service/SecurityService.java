package com.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.persistence.model.user.Customer;

@org.springframework.stereotype.Service
public class SecurityService {
    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);

    @Autowired
    ReservationService reservationService;
    @Autowired
    RestaurantService restaurantService;
    @Autowired
    RestaurantUserDAO restaurantUserDAO;
    @Autowired
    ServiceService serviceService;


    @Transactional
    public boolean hasPermissionOnReservation(Long restaurantUserId, Long reservationId) {
        Reservation reservation = reservationService.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found");
        }
        RestaurantUser restaurantUser = restaurantUserDAO.findById(restaurantUserId).orElse(null);
        if (restaurantUser != null) {
            Restaurant restaurant = restaurantUser.getRestaurant();
            return restaurant.equals(reservation.getRestaurant());
        }
        return false;
    }

    @Transactional
    public boolean isUserAdmin(@AuthenticationPrincipal UserDetails userDetails) {
        return userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    public boolean hasUserPermissionOnReservation(Long idReservation, @AuthenticationPrincipal User user) {
        logger.debug("Checking user permission on reservation with id: {}", idReservation);
        Reservation reservation = reservationService.findById(idReservation);
        return hasUserPermissionOnReservation(reservation, user);
    }

    @Transactional
    public boolean isRestaurantUser(Long idRestaurantUser, @AuthenticationPrincipal Customer user) {
        RestaurantUser restaurantUser = user.getRestaurantUsers().stream()
                .filter(ru -> ru.getId().equals(idRestaurantUser))
                .findFirst().orElse(null);
        if (restaurantUser != null && restaurantUser.getId().equals(idRestaurantUser)) {
            return true;
        }
        return false;
    }

    @Transactional
    public boolean existsRestaurantUserWithId(Long idRestaurantUser, @AuthenticationPrincipal Customer user) {
        RestaurantUser restaurantUser = restaurantUserDAO.findById(idRestaurantUser).get();
        for (RestaurantUser ru : user.getRestaurantUsers()) {
            if (ru.equals(restaurantUser)) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public boolean hasRestaurantUserRole(Long idRestaurantUser, String roleName, @AuthenticationPrincipal Customer user) {
        RestaurantUser restaurantUser = restaurantUserDAO.findById(idRestaurantUser).get();
        if (restaurantUser != null) {
            if (restaurantUser.hasRestaurantRole(roleName)) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public boolean isRestaurantUserPermission(Long idRestaurantUser, @AuthenticationPrincipal Customer user) {
        RestaurantUser restaurantUser = restaurantUserDAO.findById(idRestaurantUser).orElse(null);
        if (restaurantUser != null) {
            return user.getRestaurantUsers().stream()
                    .anyMatch(ru -> ru.equals(restaurantUser) && ru.getStatus().equals(RestaurantUser.Status.ENABLED));
        }
        return false;
    }

    @Transactional
    public boolean hasRestaurantUserPermission(Long idRestaurantUser, String permission, @AuthenticationPrincipal Customer user) {
        RestaurantUser restaurantUser = restaurantUserDAO.findById(idRestaurantUser).orElse(null);
        if (restaurantUser != null) {
            return user.getRestaurantUsers().stream()
                    .anyMatch(ru -> ru.equals(restaurantUser) && ru.getPrivileges().stream().anyMatch(privilege -> privilege.getName().equals(permission)) && ru.getStatus().equals(RestaurantUser.Status.ENABLED));
        }
        //qua potrei prenderei i ruoli del ristorante e vedere quali ha oppure prendere quelli dell'utente
        return false;
    }

}
