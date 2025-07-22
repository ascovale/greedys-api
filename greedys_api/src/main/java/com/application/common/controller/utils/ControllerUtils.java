package com.application.common.controller.utils;

import org.springframework.security.core.context.SecurityContextHolder;

import com.application.admin.model.Admin;
import com.application.customer.model.Customer;
import com.application.restaurant.model.Restaurant;
import com.application.restaurant.model.user.RUser;

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

    public static RUser getCurrentRUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof RUser) {
            return (RUser) principal;
        } else {
            System.out.println("Questo non dovrebbe succedere");
            return null;
        }
    }

    public static Admin getCurrentAdmin() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Admin) {
            return (Admin) principal;
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
