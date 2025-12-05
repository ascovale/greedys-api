package com.application.common.persistence.model.group.enums;

/**
 * Type of cuisine offered by a restaurant.
 * Used for search filtering.
 */
public enum CuisineType {
    
    // ═══════════════════════════════════════════════════════════════
    // MAIN TYPES
    // ═══════════════════════════════════════════════════════════════
    
    ITALIAN("Italiana"),
    MEDITERRANEAN("Mediterranea"),
    SEAFOOD("Pesce"),
    MEAT("Carne"),
    PIZZA("Pizzeria"),
    VEGETARIAN("Vegetariana"),
    VEGAN("Vegana"),
    
    // ═══════════════════════════════════════════════════════════════
    // INTERNATIONAL
    // ═══════════════════════════════════════════════════════════════
    
    JAPANESE("Giapponese"),
    CHINESE("Cinese"),
    THAI("Thailandese"),
    INDIAN("Indiana"),
    MEXICAN("Messicana"),
    AMERICAN("Americana"),
    FRENCH("Francese"),
    SPANISH("Spagnola"),
    GREEK("Greca"),
    MIDDLE_EASTERN("Mediorientale"),
    
    // ═══════════════════════════════════════════════════════════════
    // SPECIALTIES
    // ═══════════════════════════════════════════════════════════════
    
    GRILL("Griglieria"),
    FISH_RESTAURANT("Ristorante di Pesce"),
    STEAKHOUSE("Steakhouse"),
    TRATTORIA("Trattoria"),
    OSTERIA("Osteria"),
    FINE_DINING("Fine Dining"),
    STREET_FOOD("Street Food"),
    REGIONAL_TUSCAN("Cucina Toscana"),
    REGIONAL_SICILIAN("Cucina Siciliana"),
    REGIONAL_ROMAN("Cucina Romana"),
    REGIONAL_NEAPOLITAN("Cucina Napoletana"),
    REGIONAL_MILANESE("Cucina Milanese");
    
    private final String displayName;
    
    CuisineType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
