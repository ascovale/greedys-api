package com.application.common.persistence.model.group.enums;

/**
 * Type of booker making the group booking.
 */
public enum BookerType {
    
    /**
     * Booking made by a registered agency (B2B)
     */
    AGENCY("Agenzia"),
    
    /**
     * Booking made by an individual customer (B2C)
     */
    CUSTOMER("Privato");
    
    private final String displayName;
    
    BookerType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
