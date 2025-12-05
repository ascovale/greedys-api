package com.application.common.persistence.model.group.enums;

/**
 * Defines visibility of a FixedPriceMenu.
 * Controls which user types can see and book the menu.
 */
public enum MenuVisibility {
    
    /**
     * Only visible to registered agencies (B2B)
     */
    AGENCY_ONLY,
    
    /**
     * Only visible to individual customers (B2C)
     */
    CUSTOMER_ONLY,
    
    /**
     * Visible to both agencies and customers
     * (may have different pricing for each)
     */
    BOTH
}
