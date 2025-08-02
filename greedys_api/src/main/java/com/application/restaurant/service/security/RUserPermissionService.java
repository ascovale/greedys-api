package com.application.restaurant.service.security;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.restaurant.persistence.dao.RUserHubDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.model.user.RUser;

import lombok.RequiredArgsConstructor;

@Service("securityRUserService")
@Transactional
@RequiredArgsConstructor
public class RUserPermissionService {

    private final ReservationDAO reservationRepository;
    private final RUserHubDAO RUserHubDAO;

    public boolean hasPermissionOnReservation(Long idReservation) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        RUser RUser = null;

        if (principal instanceof RUser) {
            RUser = (RUser) principal;
            if (!isRestaurantEnabled(RUser)) {
                return false;
            }
        } else {
            return false;
        }
        // Check if the reservation exists and if the RUser is of the same
        // restaurant of the reservation
        // and if the customer is enabled
        Optional<Reservation> reservation = reservationRepository.findById(idReservation);
        if (!reservation.isPresent()) {
            return false;
        }

        Long restaurantIdFromReservation = reservation.get().getRestaurant().getId();
        Long restaurantIdFromUser = RUser.getRestaurant().getId();

        if (!restaurantIdFromReservation.equals(restaurantIdFromUser)) {
            return false;
        }
        if (!RUser.isEnabled()) {
            return false;
        }

        return true;
    }

    public boolean isRestaurantEnabled(RUser RUser) {
        if (RUser == null || RUser.getRestaurant() == null) {
            return false;
        }
        return RUser.getRestaurant().getStatus() == Restaurant.Status.ENABLED;
    }

    public boolean isRestaurantEnabled() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        RUser RUser = null;

        if (principal instanceof RUser) {
            RUser = (RUser) principal;
        } else {
            return false;
        }

        if (RUser.getRestaurant() == null) {
            return false;
        }
        return RUser.getRestaurant().getStatus() == Restaurant.Status.ENABLED;
    }

    public boolean hasRUserId(Long userId, String email) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        RUser RUser = null;

        if (principal instanceof RUser) {
            RUser = (RUser) principal;
        } else {
            return false;
        }

        if (!RUser.isEnabled() || !isRestaurantEnabled(RUser)) {
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
        if (!(principal instanceof RUser)) {
            return false;
        }

        RUser RUser = (RUser) principal;

        return RUserHubDAO.hasPermissionForRestaurant(RUser.getRUserHub().getId(),
                restaurantId);
    }

    public boolean hasHubPermissionForRestaurant(Long restaurantId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.User) {
            org.springframework.security.core.userdetails.User userDetails = (org.springframework.security.core.userdetails.User) principal;
            String email = userDetails.getUsername();
            if (email == null || email.isEmpty()) {
                return false;
            }
            List<Restaurant> restaurants = RUserHubDAO.findAllRestaurantsByHubEmail(email);
            if (restaurants == null) {
                return false;
            }
            return restaurants.stream().anyMatch(r -> r.getId().equals(restaurantId));
        }
        return false;
      
    }


}
