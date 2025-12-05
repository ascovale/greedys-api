package com.application.challenge.persistence.model.enums;

/**
 * Stato di una Challenge.
 * <p>
 * Flusso: DRAFT → UPCOMING → REGISTRATION → PRELIMINARY → ACTIVE → BRACKETS → VOTING → COMPLETED
 */
public enum ChallengeStatus {

    /**
     * Bozza, non ancora pubblicata
     */
    DRAFT("Bozza"),

    /**
     * Pubblicata ma non ancora iniziata
     */
    UPCOMING("In arrivo"),

    /**
     * Fase di registrazione aperta
     */
    REGISTRATION("Registrazione"),

    /**
     * Fase preliminare (qualificazione)
     */
    PRELIMINARY("Fase preliminare"),

    /**
     * Challenge attiva, ristoranti competono
     */
    ACTIVE("Attiva"),

    /**
     * Fase brackets/eliminazione diretta (se applicabile)
     */
    BRACKETS("Eliminazione diretta"),

    /**
     * Fase di votazione finale
     */
    VOTING("Votazione"),

    /**
     * Challenge completata, risultati finali
     */
    COMPLETED("Completata"),

    /**
     * Challenge cancellata
     */
    CANCELLED("Cancellata");

    private final String displayName;

    ChallengeStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Verifica se la challenge accetta nuove partecipazioni
     */
    public boolean acceptsParticipation() {
        return this == REGISTRATION;
    }

    /**
     * Verifica se la challenge accetta voti
     */
    public boolean acceptsVotes() {
        return this == VOTING || this == ACTIVE || this == PRELIMINARY || this == BRACKETS;
    }

    /**
     * Verifica se la challenge è terminata
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * Verifica se la challenge è in corso
     */
    public boolean isOngoing() {
        return this == PRELIMINARY || this == ACTIVE || this == BRACKETS || this == VOTING;
    }
}
