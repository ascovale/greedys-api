package com.application.common.persistence.model.group.enums;

/**
 * Event categories for group bookings.
 * Unified enum for both Agency and Customer events.
 */
public enum EventCategory {
    
    // ═══════════════════════════════════════════════════════════════
    // AGENCY EVENTS (B2B)
    // ═══════════════════════════════════════════════════════════════
    
    TOUR_GROUP("Gruppo Turistico", BookerTypeHint.AGENCY),
    CORPORATE_LUNCH("Pranzo Aziendale", BookerTypeHint.AGENCY),
    CORPORATE_DINNER("Cena Aziendale", BookerTypeHint.AGENCY),
    CONFERENCE("Conferenza/Meeting", BookerTypeHint.AGENCY),
    INCENTIVE("Viaggio Incentive", BookerTypeHint.AGENCY),
    SCHOOL_TRIP("Gita Scolastica", BookerTypeHint.AGENCY),
    WEDDING_AGENCY("Matrimonio (via Agenzia)", BookerTypeHint.AGENCY),
    TEAM_BUILDING("Team Building", BookerTypeHint.AGENCY),
    
    // ═══════════════════════════════════════════════════════════════
    // CUSTOMER EVENTS (B2C)
    // ═══════════════════════════════════════════════════════════════
    
    GRADUATION("Laurea", BookerTypeHint.CUSTOMER),
    BIRTHDAY("Compleanno", BookerTypeHint.CUSTOMER),
    ANNIVERSARY("Anniversario", BookerTypeHint.CUSTOMER),
    COMMUNION("Comunione", BookerTypeHint.CUSTOMER),
    CONFIRMATION("Cresima", BookerTypeHint.CUSTOMER),
    BAPTISM("Battesimo", BookerTypeHint.CUSTOMER),
    ENGAGEMENT("Fidanzamento", BookerTypeHint.CUSTOMER),
    RETIREMENT("Pensionamento", BookerTypeHint.CUSTOMER),
    REUNION("Reunion", BookerTypeHint.CUSTOMER),
    WEDDING_PRIVATE("Matrimonio (Privato)", BookerTypeHint.CUSTOMER),
    BABY_SHOWER("Baby Shower", BookerTypeHint.CUSTOMER),
    
    // ═══════════════════════════════════════════════════════════════
    // SHARED (Both Agency and Customer)
    // ═══════════════════════════════════════════════════════════════
    
    TASTING("Degustazione", BookerTypeHint.BOTH),
    CELEBRATION_OTHER("Altra Celebrazione", BookerTypeHint.BOTH),
    PRIVATE_EVENT("Evento Privato", BookerTypeHint.BOTH);
    
    private final String displayName;
    private final BookerTypeHint bookerTypeHint;
    
    EventCategory(String displayName, BookerTypeHint bookerTypeHint) {
        this.displayName = displayName;
        this.bookerTypeHint = bookerTypeHint;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public BookerTypeHint getBookerTypeHint() {
        return bookerTypeHint;
    }
    
    /**
     * Hint for which booker type typically uses this category.
     */
    public enum BookerTypeHint {
        AGENCY,
        CUSTOMER,
        BOTH
    }
}
