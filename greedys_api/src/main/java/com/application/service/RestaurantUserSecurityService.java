package com.application.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.application.persistence.dao.customer.ReservationDAO;
import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RestaurantUser;

@Service
public class RestaurantUserSecurityService {

    @Autowired 
    private ReservationDAO reservationRepository;
    @Autowired 
    private RestaurantUserDAO restaurantUserDAO;
    
    public boolean hasPermissionOnReservation(Long idReservation) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        RestaurantUser restaurantUser =null;

        if (principal instanceof RestaurantUser) {
            restaurantUser = (RestaurantUser) principal;
            if (!isRestaurantEnabled(restaurantUser)) {
                return false;
            }
        } else {
            return false;
        }
        // Check if the reservation exists and if the restaurantUser is of the same restaurant of the reservation
        // and if the customer is enabled
        Optional<Reservation> reservation = reservationRepository.findById(idReservation);
        if (!reservation.isPresent()) {
            return false;
        }

        Long restaurantIdFromReservation = reservation.get().getRestaurant().getId();
        Long restaurantIdFromUser = restaurantUser.getRestaurant().getId();

        if (!restaurantIdFromReservation.equals(restaurantIdFromUser)) {
            return false;
        }
        if (!restaurantUser.isEnabled()) {
            return false;
        }
        
        return true;
    }

    public boolean isRestaurantEnabled(RestaurantUser restaurantUser) {
        if (restaurantUser == null || restaurantUser.getRestaurant() == null) {
            return false;
        }
        return restaurantUser.getRestaurant().getStatus() == Restaurant.Status.ENABLED;
    }

    public boolean isRestaurantEnabled() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        RestaurantUser restaurantUser = null;

        if (principal instanceof RestaurantUser) {
            restaurantUser = (RestaurantUser) principal;
        } else {
            return false;
        }

        if (restaurantUser.getRestaurant() == null) {
            return false;
        }
        return restaurantUser.getRestaurant().getStatus() == Restaurant.Status.ENABLED;
    }

    public boolean hasRestaurantUserId(Long userId, String email) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        RestaurantUser restaurantUser = null;

        if (principal instanceof RestaurantUser) {
            restaurantUser = (RestaurantUser) principal;
        } else {
            return false;
        }

        if (!restaurantUser.isEnabled() || !isRestaurantEnabled(restaurantUser)) {
            return false;
        }

        Optional<RestaurantUser> userOptional = restaurantUserDAO.findRestaurantUserByIdAndEmail(userId, email);
        if (!userOptional.isPresent()) {
            return false;
        }

        RestaurantUser foundUser = userOptional.get();
        return foundUser.isEnabled() && isRestaurantEnabled(foundUser);
    }
}