package com.application.service.security;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.customer.ReservationDAO;
import com.application.persistence.dao.restaurant.RestaurantUserHubDAO;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RestaurantUser;

@Service("securityRestaurantUserService")
@Transactional
public class SecurityRestaurantUserService {
    
    @Autowired 
    private ReservationDAO reservationRepository;
    @Autowired
    private RestaurantUserHubDAO restaurantUserHubDAO;
    
    public boolean hasPermissionOnReservation(Long idReservation) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        RestaurantUser restaurantUser = null;

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

        throw new IllegalArgumentException("User not found with id: " + userId + " and email: " + email);
       
    }

    public boolean hasPermissionForRestaurant(Long restaurantId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof RestaurantUser)) {
            return false;
        }

        RestaurantUser restaurantUser = (RestaurantUser) principal;
        
        return restaurantUserHubDAO.hasPermissionForRestaurant(restaurantUser.getRestaurantUserHub().getId(), restaurantId);
    }
}