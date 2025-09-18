package com.application.admin.controller.utils;

import org.springframework.security.core.context.SecurityContextHolder;

import com.application.admin.persistence.model.Admin;

public class AdminControllerUtils {
        public static Admin getCurrentAdmin() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Admin) {
            return (Admin) principal;
        } else {
            System.out.println("Questo non dovrebbe succedere");
            return null;
        }
    }
}
