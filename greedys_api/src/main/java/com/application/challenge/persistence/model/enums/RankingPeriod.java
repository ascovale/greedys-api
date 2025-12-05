package com.application.challenge.persistence.model.enums;

/**
 * Periodo temporale per il Ranking.
 */
public enum RankingPeriod {

    /**
     * Classifica settimanale
     */
    WEEKLY("Settimanale", 7),

    /**
     * Classifica mensile
     */
    MONTHLY("Mensile", 30),

    /**
     * Classifica trimestrale
     */
    QUARTERLY("Trimestrale", 90),

    /**
     * Classifica annuale
     */
    YEARLY("Annuale", 365),

    /**
     * Classifica di sempre (all-time)
     */
    ALL_TIME("Di Sempre", -1);

    private final String displayName;
    private final int days;

    RankingPeriod(String displayName, int days) {
        this.displayName = displayName;
        this.days = days;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Restituisce il numero di giorni del periodo.
     * -1 per ALL_TIME (nessun limite)
     */
    public int getDays() {
        return days;
    }

    /**
     * Verifica se il periodo ha un limite temporale
     */
    public boolean hasTimeLimit() {
        return days > 0;
    }
}
