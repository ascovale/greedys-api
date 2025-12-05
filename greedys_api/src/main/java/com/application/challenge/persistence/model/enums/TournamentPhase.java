package com.application.challenge.persistence.model.enums;

/**
 * Fase di un Tournament.
 */
public enum TournamentPhase {

    /**
     * Fase di registrazione
     */
    REGISTRATION("Registrazione", 0),

    /**
     * Fase di qualificazione (dal ranking)
     */
    QUALIFICATION("Qualificazione", 1),

    /**
     * Fase a gironi (round-robin)
     */
    GROUP_STAGE("Fase a Gironi", 2),

    /**
     * Ottavi di finale
     */
    ROUND_OF_16("Ottavi di Finale", 3),

    /**
     * Quarti di finale
     */
    QUARTER_FINALS("Quarti di Finale", 4),

    /**
     * Semifinale
     */
    SEMI_FINALS("Semifinale", 5),

    /**
     * Finale per il terzo posto
     */
    THIRD_PLACE("Finale 3°/4° Posto", 6),

    /**
     * Finale
     */
    FINALS("Finale", 7),

    /**
     * Torneo completato
     */
    COMPLETED("Completato", 8);

    private final String displayName;
    private final int order;

    TournamentPhase(String displayName, int order) {
        this.displayName = displayName;
        this.order = order;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getOrder() {
        return order;
    }

    /**
     * Verifica se è una fase eliminatoria (scontri diretti)
     */
    public boolean isKnockoutPhase() {
        return this == ROUND_OF_16 || this == QUARTER_FINALS || 
               this == SEMI_FINALS || this == FINALS || this == THIRD_PLACE;
    }

    /**
     * Verifica se è la fase a gironi
     */
    public boolean isGroupPhase() {
        return this == GROUP_STAGE;
    }

    /**
     * Restituisce la fase successiva
     */
    public TournamentPhase nextPhase() {
        return switch (this) {
            case REGISTRATION -> QUALIFICATION;
            case QUALIFICATION -> GROUP_STAGE;
            case GROUP_STAGE -> QUARTER_FINALS;
            case ROUND_OF_16 -> QUARTER_FINALS;
            case QUARTER_FINALS -> SEMI_FINALS;
            case SEMI_FINALS -> FINALS;
            case THIRD_PLACE, FINALS -> COMPLETED;
            case COMPLETED -> COMPLETED;
        };
    }
}
