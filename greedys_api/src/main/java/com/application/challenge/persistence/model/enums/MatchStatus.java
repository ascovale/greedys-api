package com.application.challenge.persistence.model.enums;

/**
 * Stato di un Match nel torneo.
 */
public enum MatchStatus {

    /**
     * Match programmato, non ancora iniziato
     */
    SCHEDULED("Programmato"),

    /**
     * Votazione in corso
     */
    VOTING("Votazione Aperta"),

    /**
     * Match completato, risultato finale
     */
    COMPLETED("Completato"),

    /**
     * Match cancellato (es: ritiro di un partecipante)
     */
    CANCELLED("Cancellato");

    private final String displayName;

    MatchStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Verifica se il match accetta voti
     */
    public boolean acceptsVotes() {
        return this == VOTING;
    }

    /**
     * Verifica se il match è terminato
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
