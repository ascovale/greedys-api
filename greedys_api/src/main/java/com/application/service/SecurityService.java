package com.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.RestaurantUser;
import com.application.persistence.model.user.User;

@Service
public class SecurityService {
    @Autowired
    ReservationService reservationService;
    @Autowired
    RestaurantService restaurantService;

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
            user= ((User) principal);
            if(user.getRestaurantUser()!=null) {
                RestaurantUser restaurantUser = user.getRestaurantUser();
                Restaurant restaurant = reservation.getRestaurant();
                if (restaurant != null) {
                    for (RestaurantUser ruser : restaurant.getRestaurantUsers()) {
                        if (ruser.equals(restaurantUser)) return true;       
                    }
                }
            }
            return false;
            
        }   
        else throw new IllegalStateException("Illegal principal type");
    }
    @Transactional
    public boolean hasPermissionOnReservation(Long reservationId){
        Reservation reservation =reservationService.findById(reservationId);
        return hasUserPermissionOnReservation(reservation) || hasRestaurantUserPermissionOnReservation(reservation) || isUserAdmin();
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

    /*    @PreAuthorize("@SecurityService.hasPermission(#reservationId, T(org.springframework.security.acls.domain.BasePermission).WRITE)")
 

    public boolean hasPermission(Long reservationId, Permission permission) {
        User currentUser = getCurrentUser();
        Sid sid = new PrincipalSid(currentUser.getUsername());
        ObjectIdentity oi = new ObjectIdentityImpl(Reservation.class, reservationId);
        Acl acl = aclService.readAclById(oi);
        return acl.isGranted(List.of(permission), List.of(sid), false);
    } */

    @Transactional
    public boolean hasRestaurantUserPermissionOnRestaurantWithId(Long idRestaurant) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Restaurant restaurant = restaurantService.findById(idRestaurant);
        Object principal = authentication.getPrincipal();
        User user;
		if (principal instanceof User) {
            user= ((User) principal);
            if(user.getRestaurantUser()!=null) {
                RestaurantUser restaurantUser = user.getRestaurantUser();
                if (restaurant != null) {
                    for (RestaurantUser ruser : restaurant.getRestaurantUsers()) {
                        //DEVE ESSERE ACCETTATO E NON BLOCCATO
                        if (ruser.getAccepted() && !ruser.getBlocked()) {
                            if (ruser.equals(restaurantUser)) return true;  
                        }     
                    }
                }
            }
            return false;
            
        }   
        else throw new IllegalStateException("Illegal principal type");
    }
}
