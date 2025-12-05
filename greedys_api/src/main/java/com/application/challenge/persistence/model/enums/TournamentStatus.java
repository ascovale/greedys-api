package com.application.challenge.persistence.model.enums;

/**
 * Stato di un Tournament.
 */
public enum TournamentStatus {

    /**
     * Bozza, non ancora pubblicato
     */
    DRAFT("Bozza"),

    /**
     * Fase di registrazione aperta
     */
    REGISTRATION("Registrazione"),

    /**
     * Torneo in corso
     */
    ONGOING("In Corso"),

    /**
     * Torneo completato
     */
    COMPLETED("Completato"),

    /**
     * Torneo cancellato
     */
    CANCELLED("Cancellato");

    private final String displayName;

    TournamentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Verifica se il torneo accetta nuove iscrizioni
     */
    public boolean acceptsRegistration() {
        return this == REGISTRATION;
    }

    /**
     * Verifica se il torneo è attivo (in corso)
     */
    public boolean isActive() {
        return this == ONGOING;
    }

    /**
     * Verifica se il torneo è terminato
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
