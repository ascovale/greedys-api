package com.application.common.persistence.model.group.enums;

/**
 * Status of a group booking.
 */
public enum GroupBookingStatus {
    
    /**
     * Initial inquiry, not yet a formal booking
     */
    INQUIRY("Richiesta Info"),
    
    /**
     * Quote/proposal has been sent
     */
    QUOTE_SENT("Preventivo Inviato"),
    
    /**
     * Booking created but not yet confirmed by restaurant
     */
    PENDING("In Attesa Conferma"),
    
    /**
     * Booking confirmed by restaurant
     */
    CONFIRMED("Confermata"),
    
    /**
     * Deposit has been paid
     */
    DEPOSIT_PAID("Caparra Versata"),
    
    /**
     * Full payment received
     */
    FULLY_PAID("Saldo Pagato"),
    
    /**
     * Event has been completed
     */
    COMPLETED("Completata"),
    
    /**
     * Booking was cancelled
     */
    CANCELLED("Cancellata"),
    
    /**
     * Group did not show up
     */
    NO_SHOW("No Show");
    
    private final String displayName;
    
    GroupBookingStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if booking is in a modifiable state
     */
    public boolean isModifiable() {
        return this == INQUIRY || this == QUOTE_SENT || this == PENDING || this == CONFIRMED;
    }
    
    /**
     * Check if booking is in a final state
     */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED || this == NO_SHOW;
    }
}
