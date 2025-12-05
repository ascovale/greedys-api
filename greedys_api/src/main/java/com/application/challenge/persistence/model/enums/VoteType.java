package com.application.challenge.persistence.model.enums;

/**
 * Tipo di voto.
 */
public enum VoteType {

    /**
     * Voto per ranking continuo (singolo ristorante)
     */
    RANKING_VOTE("Voto Ranking"),

    /**
     * Voto per match diretto (scontro tra 2 ristoranti)
     */
    MATCH_VOTE("Voto Match"),

    /**
     * Voto per challenge (classifica challenge)
     */
    CHALLENGE_VOTE("Voto Challenge");

    private final String displayName;

    VoteType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Verifica se richiede prenotazioni in entrambi i ristoranti
     */
    public boolean requiresBothRestaurants() {
        return this == MATCH_VOTE;
    }
}
