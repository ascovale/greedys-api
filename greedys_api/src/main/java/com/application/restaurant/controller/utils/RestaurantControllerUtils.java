package com.application.restaurant.controller.utils;

import org.springframework.security.core.context.SecurityContextHolder;

import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.model.user.RUser;

public class RestaurantControllerUtils {
    public static RUser getCurrentRUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof RUser) {
            return (RUser) principal;
        } else {
            System.out.println("Questo non dovrebbe succedere");
            return null;
        }
    }

    public static Restaurant getCurrentRestaurant() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof RUser) {
            return ((RUser) principal).getRestaurant();
        } else {
            System.out.println("Questo non dovrebbe succedere");
            return null;
        }
    }
}
