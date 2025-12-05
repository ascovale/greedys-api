package com.application.challenge.persistence.model.enums;

/**
 * Ambito geografico del Ranking.
 */
public enum RankingScope {

    /**
     * Classifica nazionale
     */
    NATIONAL("Nazionale", 1),

    /**
     * Classifica regionale
     */
    REGIONAL("Regionale", 2),

    /**
     * Classifica per città
     */
    CITY("Città", 3),

    /**
     * Classifica per zona/quartiere
     */
    ZONE("Zona", 4),

    /**
     * Classifica per area estesa (per ristoranti piccoli)
     */
    EXTENDED_AREA("Area Estesa", 5);

    private final String displayName;
    private final int level;

    RankingScope(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Livello di granularità (1 = più ampio, 5 = più specifico)
     */
    public int getLevel() {
        return level;
    }

    /**
     * Verifica se è un ambito locale
     */
    public boolean isLocal() {
        return level >= 3;
    }
}
