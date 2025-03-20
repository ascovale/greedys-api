package com.application.controller.utils;

import org.springframework.security.core.context.SecurityContextHolder;

import com.application.persistence.model.admin.Admin;
import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RestaurantUser;

public class ControllerUtils {
    public static Customer getCurrentCustomer() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Customer) {
            return ((Customer) principal);
        } else {
            System.out.println("Questo non dovrebbe succedere");
            return null;
        }
    }

    public static RestaurantUser getCurrentRestaurantUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Customer) {
            return ((RestaurantUser) principal);
        } else {
            System.out.println("Questo non dovrebbe succedere");
            return null;
        }
    }

    public static Admin getCurrentAdmin() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Customer) {
            return ((Admin) principal);
        } else {
            System.out.println("Questo non dovrebbe succedere");
            return null;
        }
    }

    public static Restaurant getCurrentRestaurant() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Customer) {
            return ((RestaurantUser) principal).getRestaurant();
        } else {
            System.out.println("Questo non dovrebbe succedere");
            return null;
        }
    }
}
