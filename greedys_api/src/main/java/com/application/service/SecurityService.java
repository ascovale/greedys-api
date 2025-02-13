package com.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.reservation.Service;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.RestaurantUser;
import com.application.persistence.model.user.User;

@org.springframework.stereotype.Service
public class SecurityService {
    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);

    @Autowired
    ReservationService reservationService;
    @Autowired
    RestaurantService restaurantService;
    @Autowired
    ServiceService serviceService;

    @Transactional
    public boolean hasUserPermissionOnReservation(Long idReservation, @AuthenticationPrincipal User user) {
        logger.debug("Checking user permission on reservation with id: {}", idReservation);
        Reservation reservation = reservationService.findById(idReservation);
        return hasUserPermissionOnReservation(reservation, user);
    }

    @Transactional
    public boolean hasUserPermissionOnReservation(Reservation reservation, @AuthenticationPrincipal User user) {
        logger.debug("Checking user permission on reservation: {}", reservation);
        if (reservation.getUser().equals(user)) {
            return true;
        }
        return false;
    }

    @Transactional
    public boolean hasRestaurantUserPermissionOnReservation(Reservation reservation, @AuthenticationPrincipal User user) {
        logger.debug("Checking restaurant user permission on reservation: {}", reservation);
        if (user.getRestaurantUser() != null) {
            RestaurantUser restaurantUser = user.getRestaurantUser();
            Restaurant restaurant = reservation.getRestaurant();
            if (restaurant != null) {
                for (RestaurantUser ruser : restaurant.getRestaurantUsers()) {
                    if (ruser.equals(restaurantUser))
                        return true;
                }
            }
        }
        return false;
    }

    @Transactional
    public boolean hasPermissionOnReservation(Long reservationId, @AuthenticationPrincipal User user) {
        logger.debug("Checking permission on reservation with id: {}", reservationId);
        Reservation reservation = reservationService.findById(reservationId);
        return hasUserPermissionOnReservation(reservation, user) || hasRestaurantUserPermissionOnReservation(reservation, user)
                || isUserAdmin(user);
    }

    @Transactional
    public boolean isUserAdmin(@AuthenticationPrincipal UserDetails userDetails) {
        logger.debug("Checking if user is admin: {}", userDetails);
        return userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Transactional
    public boolean hasRestaurantUserPermissionOnRestaurantWithId(Long idRestaurant, @AuthenticationPrincipal User user) {
        logger.debug("Checking restaurant user {} permission on restaurant with id: {}", user.getId(), idRestaurant);
        Restaurant restaurant = restaurantService.findById(idRestaurant);
        if (restaurant == null || restaurant.getDeleted()) {
            return false;
        }
        if (user.getDeleted()) {
            return false;
        }
        if (user.getRestaurantUser() != null) {
            RestaurantUser restaurantUser = user.getRestaurantUser();
            if (restaurant != null) {
                for (RestaurantUser ruser : restaurant.getRestaurantUsers()) {
                    if (ruser.getAccepted() && !ruser.getBlocked() && !ruser.getDeleted()) {
                        if (ruser.equals(restaurantUser))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    @Transactional
    public boolean isRestaurantUser(Long idRestaurantUser, @AuthenticationPrincipal User user) {
        logger.debug("Checking if user is restaurant user with id: {}", idRestaurantUser);
        RestaurantUser restaurantUser = user.getRestaurantUser();
        if (restaurantUser != null && restaurantUser.getId().equals(idRestaurantUser)) {
            return true;
        }
        return false;
    }
    
    @Transactional
    public boolean hasUserRestaurantServicePermission(Long idService, @AuthenticationPrincipal User user) {
        logger.debug("Checking user restaurant service permission on service with id: {}", idService);
        Service service =  serviceService.getById(idService);
        if (service == null) {
            throw new IllegalArgumentException("Service not found with id: " + idService);
        }
        if (user.getRestaurantUser() != null) {
            RestaurantUser restaurantUser = user.getRestaurantUser();
            Restaurant restaurant = restaurantUser.getRestaurant();
            if (restaurant != null && !restaurant.getDeleted()) {
                if(restaurant.getServices().contains(service)) return true;
            }
        }
        return false;
    }
}
