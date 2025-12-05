package com.application.common.persistence.model.group.enums;

/**
 * Dietary requirements and restrictions.
 * Used for filtering restaurants and tracking group needs.
 */
public enum DietaryRequirement {
    
    // ═══════════════════════════════════════════════════════════════
    // DIETS
    // ═══════════════════════════════════════════════════════════════
    
    VEGETARIAN("Vegetariano", "No carne e pesce", DietaryCategory.DIET),
    VEGAN("Vegano", "No prodotti di origine animale", DietaryCategory.DIET),
    PESCATARIAN("Pescetariano", "No carne, sì pesce", DietaryCategory.DIET),
    
    // ═══════════════════════════════════════════════════════════════
    // ALLERGIES & INTOLERANCES
    // ═══════════════════════════════════════════════════════════════
    
    GLUTEN_FREE("Senza glutine", "Celiachia / intolleranza al glutine", DietaryCategory.ALLERGY),
    LACTOSE_FREE("Senza lattosio", "Intolleranza al lattosio", DietaryCategory.ALLERGY),
    NUT_FREE("Senza frutta secca", "Allergia a noci, mandorle, etc.", DietaryCategory.ALLERGY),
    PEANUT_FREE("Senza arachidi", "Allergia alle arachidi", DietaryCategory.ALLERGY),
    SHELLFISH_FREE("Senza crostacei", "Allergia a crostacei e molluschi", DietaryCategory.ALLERGY),
    EGG_FREE("Senza uova", "Allergia alle uova", DietaryCategory.ALLERGY),
    SOY_FREE("Senza soia", "Allergia alla soia", DietaryCategory.ALLERGY),
    FISH_FREE("Senza pesce", "Allergia al pesce", DietaryCategory.ALLERGY),
    
    // ═══════════════════════════════════════════════════════════════
    // RELIGIOUS
    // ═══════════════════════════════════════════════════════════════
    
    HALAL("Halal", "Conforme ai precetti islamici", DietaryCategory.RELIGIOUS),
    KOSHER("Kosher", "Conforme ai precetti ebraici", DietaryCategory.RELIGIOUS),
    NO_PORK("No maiale", "Nessun derivato del maiale", DietaryCategory.RELIGIOUS),
    NO_ALCOHOL("No alcol", "Nessun alcol, neanche in cottura", DietaryCategory.RELIGIOUS),
    
    // ═══════════════════════════════════════════════════════════════
    // HEALTH PREFERENCES
    // ═══════════════════════════════════════════════════════════════
    
    LOW_SODIUM("Basso sodio", "Dieta iposodica", DietaryCategory.HEALTH),
    DIABETIC_FRIENDLY("Per diabetici", "Basso indice glicemico", DietaryCategory.HEALTH),
    LOW_FAT("Basso contenuto grassi", "Dieta ipocalorica", DietaryCategory.HEALTH),
    LOW_CARB("Low carb", "Basso contenuto carboidrati", DietaryCategory.HEALTH);
    
    private final String displayName;
    private final String description;
    private final DietaryCategory category;
    
    DietaryRequirement(String displayName, String description, DietaryCategory category) {
        this.displayName = displayName;
        this.description = description;
        this.category = category;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public DietaryCategory getCategory() {
        return category;
    }
    
    /**
     * Category of dietary requirement for grouping in UI.
     */
    public enum DietaryCategory {
        DIET("Diete"),
        ALLERGY("Allergie e Intolleranze"),
        RELIGIOUS("Requisiti Religiosi"),
        HEALTH("Preferenze Salutari");
        
        private final String displayName;
        
        DietaryCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
