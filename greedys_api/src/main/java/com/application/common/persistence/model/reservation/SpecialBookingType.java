package com.application.common.persistence.model.reservation;

/**
 * Tipo di prenotazione speciale.
 * Usato per identificare prenotazioni che non seguono il flusso standard (service + slot).
 */
public enum SpecialBookingType {

    /**
     * Prenotazione di gruppo (collegata a GroupBooking).
     * Può essere con o senza Service associato.
     */
    GROUP_BOOKING("Prenotazione Gruppo"),

    /**
     * Prenotazione fuori orario normale.
     * Creata per orari non coperti dai Service attivi.
     */
    OFF_HOURS("Fuori Orario"),

    /**
     * Evento privato (chiusura per evento).
     */
    PRIVATE_EVENT("Evento Privato"),

    /**
     * Prenotazione VIP/speciale.
     */
    VIP("VIP"),

    /**
     * Walk-in registrato manualmente.
     */
    WALK_IN("Walk-in");

    private final String displayName;

    SpecialBookingType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
