package com.application.challenge.persistence.model.enums;

import java.math.BigDecimal;

/**
 * Tipo di votante basato sullo storico prenotazioni.
 */
public enum VoterType {

    /**
     * Cliente locale: ≥3 prenotazioni SEATED nella zona in 12 mesi
     * Peso voto: 1.0
     */
    LOCAL("Locale", new BigDecimal("1.0"), 3),

    /**
     * Turista: <3 prenotazioni nella zona
     * Peso voto: 0.7
     */
    TOURIST("Turista", new BigDecimal("0.7"), 0),

    /**
     * Cliente verificato: >10 prenotazioni totali, account verificato
     * Peso voto: 1.2
     */
    VERIFIED("Verificato", new BigDecimal("1.2"), 10);

    private final String displayName;
    private final BigDecimal voteWeight;
    private final int minReservations;

    VoterType(String displayName, BigDecimal voteWeight, int minReservations) {
        this.displayName = displayName;
        this.voteWeight = voteWeight;
        this.minReservations = minReservations;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Peso del voto per il calcolo dello score
     */
    public BigDecimal getVoteWeight() {
        return voteWeight;
    }

    /**
     * Numero minimo di prenotazioni per questa categoria
     */
    public int getMinReservations() {
        return minReservations;
    }

    /**
     * Classifica un votante basandosi sul numero di prenotazioni nella zona
     */
    public static VoterType classify(int reservationsInZone, int totalReservations, boolean isVerified) {
        if (isVerified && totalReservations >= VERIFIED.minReservations) {
            return VERIFIED;
        }
        if (reservationsInZone >= LOCAL.minReservations) {
            return LOCAL;
        }
        return TOURIST;
    }
}
