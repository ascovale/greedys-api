package com.application.restaurant.service.security;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.restaurant.persistence.dao.RUserDAO;
import com.application.restaurant.persistence.dao.RUserHubDAO;
import com.application.restaurant.persistence.dao.SlotDAO;
import com.application.restaurant.persistence.dao.menu.MenuDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.model.user.RUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service("securityRUserService")
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RUserPermissionService {

    private final ReservationDAO reservationRepository;
    private final RUserDAO rUserDAO;
    private final RUserHubDAO RUserHubDAO;
    private final MenuDAO menuDAO;
    private final SlotDAO slotDAO;

    public boolean hasPermissionOnReservation(Long idReservation) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        RUser rUser = null;

        if (principal instanceof RUser) {
            rUser = (RUser) principal;
            // Ricarica l'RUser con il restaurant per evitare LazyInitializationException
            rUser = rUserDAO.findByIdWithRestaurantAndHub(rUser.getId());
            if (rUser == null || !isRestaurantEnabled(rUser)) {
                return false;
            }
        } else {
            return false;
        }
        // Check if the reservation exists and if the RUser is of the same
        // restaurant of the reservation
        // and if the customer is enabled
        Optional<Reservation> reservation = reservationRepository.findByIdWithRestaurant(idReservation);
        if (!reservation.isPresent()) {
            return false;
        }

        Long restaurantIdFromReservation = reservation.get().getRestaurant().getId();
        Long restaurantIdFromUser = rUser.getRestaurant().getId();

        if (!restaurantIdFromReservation.equals(restaurantIdFromUser)) {
            return false;
        }
        if (!rUser.isEnabled()) {
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
        RUser rUser = null;

        if (principal instanceof RUser) {
            rUser = (RUser) principal;
            // Ricarica l'RUser con il restaurant per evitare LazyInitializationException
            rUser = rUserDAO.findByIdWithRestaurantAndHub(rUser.getId());
            if (rUser == null) {
                return false;
            }
        } else {
            return false;
        }

        if (rUser.getRestaurant() == null) {
            return false;
        }
        return rUser.getRestaurant().getStatus() == Restaurant.Status.ENABLED;
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

        RUser rUser = (RUser) principal;
        // Ricarica l'RUser con il restaurant e RUserHub per evitare LazyInitializationException
        rUser = rUserDAO.findByIdWithRestaurantAndHub(rUser.getId());
        if (rUser == null || rUser.getRUserHub() == null) {
            return false;
        }

        return RUserHubDAO.hasPermissionForRestaurant(rUser.getRUserHub().getId(),
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

    /**
     * Verifica se un menu appartiene al ristorante specificato
     */
    public boolean isMenuOwnedByRestaurant(Long menuId, Long restaurantId) {
        log.debug("Checking menu ownership: menuId={}, restaurantId={}", menuId, restaurantId);
        
        if (menuId == null || restaurantId == null) {
            log.debug("isMenuOwnedByRestaurant: null parameters - menuId={}, restaurantId={}", menuId, restaurantId);
            return false;
        }
        
        try {
            return menuDAO.findById(menuId)
                .map(menu -> {
                    // Il menu è collegato al ristorante tramite i servizi
                    boolean isOwned = menu.getServices().stream()
                        .anyMatch(service -> service.getRestaurant().getId().equals(restaurantId));
                    log.debug("Menu {} ownership check result: {}", menuId, isOwned);
                    return isOwned;
                })
                .orElse(false);
        } catch (Exception e) {
            log.error("Error checking menu ownership: menuId={}, restaurantId={}", menuId, restaurantId, e);
            return false;
        }
    }

    /**
     * Verifica se uno slot appartiene al ristorante specificato
     */
    public boolean isSlotOwnedByRestaurant(Long slotId, Long restaurantId) {
        log.debug("Checking slot ownership: slotId={}, restaurantId={}", slotId, restaurantId);
        
        if (slotId == null || restaurantId == null) {
            log.debug("isSlotOwnedByRestaurant: null parameters - slotId={}, restaurantId={}", slotId, restaurantId);
            return false;
        }
        
        try {
            return slotDAO.findById(slotId)
                .map(slot -> {
                    // Lo slot è collegato al ristorante tramite il servizio
                    Long slotRestaurantId = slot.getService().getRestaurant().getId();
                    boolean isOwned = slotRestaurantId.equals(restaurantId);
                    log.debug("Slot {} belongs to restaurant {}, checking against restaurant {} - result: {}", 
                             slotId, slotRestaurantId, restaurantId, isOwned);
                    return isOwned;
                })
                .orElse(false);
        } catch (Exception e) {
            log.error("Error checking slot ownership: slotId={}, restaurantId={}", slotId, restaurantId, e);
            return false;
        }
    }

    /**
     * Verifica se un menu appartiene al ristorante dell'utente autenticato
     */
    public boolean isMenuOwnedByAuthenticatedUser(Long menuId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("isMenuOwnedByAuthenticatedUser: no authentication");
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof RUser)) {
            log.debug("isMenuOwnedByAuthenticatedUser: principal is not RUser");
            return false;
        }

        RUser rUser = (RUser) principal;
        // Ricarica l'RUser con il restaurant per evitare LazyInitializationException
        rUser = rUserDAO.findByIdWithRestaurantAndHub(rUser.getId());
        if (rUser == null || rUser.getRestaurant() == null) {
            log.debug("isMenuOwnedByAuthenticatedUser: user has no restaurant");
            return false;
        }

        Long restaurantId = rUser.getRestaurant().getId();
        log.debug("isMenuOwnedByAuthenticatedUser: checking menuId={} against restaurantId={}", menuId, restaurantId);
        
        return isMenuOwnedByRestaurant(menuId, restaurantId);
    }

    /**
     * Verifica se uno slot appartiene al ristorante dell'utente autenticato
     */
    public boolean isSlotOwnedByAuthenticatedUser(Long slotId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("isSlotOwnedByAuthenticatedUser: no authentication");
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof RUser)) {
            log.debug("isSlotOwnedByAuthenticatedUser: principal is not RUser");
            return false;
        }

        RUser rUser = (RUser) principal;
        // Ricarica l'RUser con il restaurant per evitare LazyInitializationException
        rUser = rUserDAO.findByIdWithRestaurantAndHub(rUser.getId());
        if (rUser == null || rUser.getRestaurant() == null) {
            log.debug("isSlotOwnedByAuthenticatedUser: user has no restaurant");
            return false;
        }

        Long restaurantId = rUser.getRestaurant().getId();
        log.debug("isSlotOwnedByAuthenticatedUser: checking slotId={} against restaurantId={}", slotId, restaurantId);
        
        return isSlotOwnedByRestaurant(slotId, restaurantId);
    }


}
