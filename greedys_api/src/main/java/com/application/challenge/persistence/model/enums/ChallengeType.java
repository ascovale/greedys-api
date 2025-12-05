package com.application.challenge.persistence.model.enums;

/**
 * Tipologia di Challenge.
 */
public enum ChallengeType {

    /**
     * Sfida su un piatto specifico (es: "Miglior Carbonara")
     */
    DISH_BATTLE("Sfida Piatto"),

    /**
     * Miglior ristorante per tipo cucina (es: "Top Sushi Milano")
     */
    CUISINE_BEST("Miglior Cucina"),

    /**
     * Challenge stagionale (es: "Miglior Menu Natale")
     */
    SEASONAL("Stagionale"),

    /**
     * Challenge sponsorizzata da brand
     */
    SPONSORED("Sponsorizzata"),

    /**
     * Challenge settimanale automatica
     */
    WEEKLY("Settimanale"),

    /**
     * Challenge mensile automatica
     */
    MONTHLY("Mensile");

    private final String displayName;

    ChallengeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
