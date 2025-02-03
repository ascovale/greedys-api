package com.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.reservation.Service;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.RestaurantUser;
import com.application.persistence.model.user.User;

@org.springframework.stereotype.Service
public class SecurityService {
    @Autowired
    ReservationService reservationService;
    @Autowired
    RestaurantService restaurantService;
    @Autowired
    ServiceService serviceService;

    @Transactional
    public boolean hasUserPermissionOnReservation(Long idReservation) {
        Reservation reservation = reservationService.findById(idReservation);
        return hasUserPermissionOnReservation(reservation);
    }

    @Transactional
    public boolean hasUserPermissionOnReservation(Reservation reservation) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            User user = ((User) principal);
            if (reservation.getUser().equals(user)) {
                return true;
            }
        } else {
            throw new IllegalStateException("Illegal principal type");
        }
        return false;
    }

    @Transactional
    public boolean hasRestaurantUserPermissionOnReservation(Reservation reservation) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        User user;
        if (principal instanceof User) {
            user = ((User) principal);
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

        } else
            throw new IllegalStateException("Illegal principal type");
    }

    @Transactional
    public boolean hasPermissionOnReservation(Long reservationId) {
        Reservation reservation = reservationService.findById(reservationId);
        return hasUserPermissionOnReservation(reservation) || hasRestaurantUserPermissionOnReservation(reservation)
                || isUserAdmin();
    }

    @Transactional
    public boolean isUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            return userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }
        return false;
    }

    /*
     * @PreAuthorize("@SecurityService.hasPermission(#reservationId, T(org.springframework.security.acls.domain.BasePermission).WRITE)"
     * )
     * 
     * 
     * public boolean hasPermission(Long reservationId, Permission permission) {
     * User currentUser = getCurrentUser();
     * Sid sid = new PrincipalSid(currentUser.getUsername());
     * ObjectIdentity oi = new ObjectIdentityImpl(Reservation.class, reservationId);
     * Acl acl = aclService.readAclById(oi);
     * return acl.isGranted(List.of(permission), List.of(sid), false);
     * }
     */

    @Transactional
    public boolean hasRestaurantUserPermissionOnRestaurantWithId(Long idRestaurant) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Restaurant restaurant = restaurantService.findById(idRestaurant);
        if (restaurant == null || restaurant.getDeleted()) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        User user;
        if (principal instanceof User) {
            user = ((User) principal);
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

        } else
            throw new IllegalStateException("Illegal principal type");
    }

    @Transactional
    public boolean isRestaurantUser(Long idRestaurantUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            User user = ((User) principal);
            RestaurantUser restaurantUser = user.getRestaurantUser();
            if (restaurantUser != null && restaurantUser.getId().equals(idRestaurantUser)) {
                return true;
            }
        } else {
            throw new IllegalStateException("Illegal principal type");
        }
        return false;
    }
    
    @Transactional
    public boolean hasUserRestaurantServicePermission(Long idService) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        Service service =  serviceService.getById(idService);
        if (service == null) {
            throw new IllegalArgumentException("Service not found with id: " + idService);
        }
        if (principal instanceof User) {
            User user = ((User) principal);
            if (user.getRestaurantUser() != null) {
                RestaurantUser restaurantUser = user.getRestaurantUser();
                Restaurant restaurant = restaurantUser.getRestaurant();
                if (restaurant != null && !restaurant.getDeleted()) {
                     
                    if(restaurant.getServices().contains(service)) return true;
                }
            }
        } else {
            throw new IllegalStateException("Illegal principal type");
        }
        return false;
    }
}
