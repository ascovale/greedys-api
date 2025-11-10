package com.application.common.config;

import java.time.LocalDateTime;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.application.admin.persistence.model.Admin;
import com.application.agency.persistence.model.user.AgencyUser;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.user.AbstractUser;
import com.application.customer.persistence.model.Customer;
import com.application.restaurant.persistence.model.user.RUser;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * Custom Entity Listener that automatically handles complete auditing:
 * - Creation and modification timestamps
 * - User reference and user type discrimination
 * This replaces Spring Data JPA's default auditing for entities requiring user type tracking.
 */
@Component
public class CustomAuditingEntityListener {

    @PrePersist
    public void prePersist(Object target) {
        if (target instanceof Reservation) {
            setCreationAuditingFields((Reservation) target);
        }
    }

    @PreUpdate
    public void preUpdate(Object target) {
        if (target instanceof Reservation) {
            setModificationAuditingFields((Reservation) target);
        }
    }

    /**
     * Sets creation auditing fields when entity is first persisted
     */
    private void setCreationAuditingFields(Reservation reservation) {
        LocalDateTime now = LocalDateTime.now();
        AbstractUser currentUser = getCurrentUser();
        
        // Set creation timestamp
        reservation.setCreatedAt(now);
        
        // Set creation user info
        if (currentUser != null) {
            reservation.setCreatedBy(currentUser);
            reservation.setCreatedByUserType(determineUserType(currentUser));
        }
    }

    /**
     * Sets modification auditing fields when entity is updated
     */
    private void setModificationAuditingFields(Reservation reservation) {
        LocalDateTime now = LocalDateTime.now();
        AbstractUser currentUser = getCurrentUser();
        
        // Set modification timestamp
        reservation.setModifiedAt(now);
        
        // Set modification user info
        if (currentUser != null) {
            reservation.setModifiedBy(currentUser);
            reservation.setModifiedByUserType(determineUserType(currentUser));
        }
    }

    /**
     * Retrieves the current authenticated user from Spring Security context
     */
    private AbstractUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        
        if (principal instanceof AbstractUser) {
            return (AbstractUser) principal;
        }
        
        // If principal is UserDetails but not AbstractUser, you might need additional logic
        // depending on your security configuration
        return null;
    }

    /**
     * Determines the user type based on the concrete class of AbstractUser
     */
    private Reservation.UserType determineUserType(AbstractUser user) {
        if (user instanceof Customer) {
            return Reservation.UserType.CUSTOMER;
        } else if (user instanceof Admin) {
            return Reservation.UserType.ADMIN;
        } else if (user instanceof RUser) {
            return Reservation.UserType.RESTAURANT_USER;
        } else if (user instanceof AgencyUser) {
            return Reservation.UserType.AGENCY_USER;
        }
        
        // Fallback - you might want to throw an exception or log a warning
        return null;
    }
}
