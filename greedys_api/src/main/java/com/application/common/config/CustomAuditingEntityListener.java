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
 * Custom Entity Listener that automatically handles auditing for AbstractUser and Reservation.
 * 
 * With JOINED inheritance:
 * - createdBy/modifiedBy/acceptedBy now reference AbstractUser directly (unified ID)
 * - No more need for createdByUserType/modifiedByUserType - use instanceof to determine type
 * 
 * ✅ REFACTORING BENEFITS:
 * - Type-safe user references: No ambiguous userId
 * - Cleaner code: No separate user_type columns
 * - Polymorphic queries: Can query AbstractUser directly
 */
@Component
public class CustomAuditingEntityListener {

    @PrePersist
    public void prePersist(Object target) {
        if (target instanceof Reservation) {
            setCreationAuditingFields((Reservation) target);
        }
        if (target instanceof AbstractUser) {
            setCreationAuditingFields((AbstractUser) target);
        }
    }

    @PreUpdate
    public void preUpdate(Object target) {
        if (target instanceof Reservation) {
            setModificationAuditingFields((Reservation) target);
        }
        if (target instanceof AbstractUser) {
            setModificationAuditingFields((AbstractUser) target);
        }
    }

    // ========== RESERVATION AUDITING ========== //

    /**
     * Sets creation auditing fields when Reservation is first persisted
     */
    private void setCreationAuditingFields(Reservation reservation) {
        LocalDateTime now = LocalDateTime.now();
        AbstractUser currentUser = getCurrentUser();
        
        // Set creation timestamp
        reservation.setCreatedAt(now);
        
        // Set creation user reference (no need for createdByUserType - use instanceof if needed)
        if (currentUser != null) {
            reservation.setCreatedBy(currentUser);
        }
    }

    /**
     * Sets modification auditing fields when Reservation is updated
     */
    private void setModificationAuditingFields(Reservation reservation) {
        LocalDateTime now = LocalDateTime.now();
        AbstractUser currentUser = getCurrentUser();
        
        // Set modification timestamp
        reservation.setModifiedAt(now);
        
        // Set modification user reference
        if (currentUser != null) {
            reservation.setModifiedBy(currentUser);
        }
    }

    // ========== ABSTRACTUSER AUDITING ========== //

    /**
     * Sets creation auditing fields when AbstractUser is first persisted
     */
    private void setCreationAuditingFields(AbstractUser user) {
        LocalDateTime now = LocalDateTime.now();
        AbstractUser currentUser = getCurrentUser();
        
        // Set creation timestamp
        user.setCreatedAt(now);
        
        // Set creation user reference (typically null for user registration)
        if (currentUser != null) {
            user.setCreatedBy(currentUser);
        }
    }

    /**
     * Sets modification auditing fields when AbstractUser is updated
     */
    private void setModificationAuditingFields(AbstractUser user) {
        LocalDateTime now = LocalDateTime.now();
        AbstractUser currentUser = getCurrentUser();
        
        // Set modification timestamp
        user.setModifiedAt(now);
        
        // Set modification user reference
        if (currentUser != null) {
            user.setModifiedBy(currentUser);
        }
    }

    // ========== HELPER METHODS ========== //

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
        
        return null;
    }

    /**
     * Determines the user type based on concrete class (for logging/reference purposes)
     * ⭐ Note: With JOINED inheritance, this is optional - use instanceof when needed
     */
    public static String determineUserTypeName(AbstractUser user) {
        if (user instanceof Customer) {
            return "CUSTOMER";
        } else if (user instanceof Admin) {
            return "ADMIN";
        } else if (user instanceof RUser) {
            return "RESTAURANT_USER";
        } else if (user instanceof AgencyUser) {
            return "AGENCY_USER";
        }
        return "UNKNOWN";
    }
}
