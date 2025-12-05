package com.application.challenge.persistence.model.enums;

/**
 * Stato di partecipazione a una Challenge o Tournament.
 */
public enum ParticipationStatus {

    /**
     * Registrato, in attesa di inizio
     */
    REGISTERED("Registrato"),

    /**
     * Qualificato per la fase successiva
     */
    QUALIFIED("Qualificato"),

    /**
     * Partecipazione attiva
     */
    ACTIVE("Attivo"),

    /**
     * Eliminato (torneo)
     */
    ELIMINATED("Eliminato"),

    /**
     * Vincitore
     */
    WINNER("Vincitore"),

    /**
     * Secondo classificato
     */
    RUNNER_UP("Secondo Posto"),

    /**
     * Terzo classificato
     */
    THIRD_PLACE("Terzo Posto"),

    /**
     * Ritirato volontariamente
     */
    WITHDRAWN("Ritirato"),

    /**
     * Squalificato
     */
    DISQUALIFIED("Squalificato");

    private final String displayName;

    ParticipationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Verifica se è ancora in competizione
     */
    public boolean isInCompetition() {
        return this == REGISTERED || this == QUALIFIED || this == ACTIVE;
    }

    /**
     * Verifica se è un piazzamento finale (podio)
     */
    public boolean isPodium() {
        return this == WINNER || this == RUNNER_UP || this == THIRD_PLACE;
    }

    /**
     * Verifica se è stato eliminato/uscito
     */
    public boolean isOut() {
        return this == ELIMINATED || this == WITHDRAWN || this == DISQUALIFIED;
    }
}
