package com.application.customer.controller.utils;

import org.springframework.security.core.context.SecurityContextHolder;

import com.application.customer.persistence.model.Customer;

public class CustomerControllerUtils {
    public static Customer getCurrentCustomer() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Customer) {
            return ((Customer) principal);
        } else {
            System.out.println("Questo non dovrebbe succedere");
            return null;
        }
    }
}
