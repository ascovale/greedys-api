package com.application.challenge.persistence.model.enums;

/**
 * Tipo di contenuto social.
 */
public enum ContentType {

    /**
     * Immagine statica
     */
    IMAGE("Immagine"),

    /**
     * Video breve
     */
    VIDEO("Video"),

    /**
     * Solo testo
     */
    TEXT("Testo");

    private final String displayName;

    ContentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Verifica se richiede upload media
     */
    public boolean requiresMedia() {
        return this == IMAGE || this == VIDEO;
    }
}
