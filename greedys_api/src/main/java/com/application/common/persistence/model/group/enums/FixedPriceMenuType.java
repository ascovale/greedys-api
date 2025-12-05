package com.application.common.persistence.model.group.enums;

/**
 * Type of fixed price menu.
 * Helps categorize menus for search and display.
 */
public enum FixedPriceMenuType {
    
    /**
     * Tasting/degustation menu
     */
    TASTING("Percorso Degustazione"),
    
    /**
     * Standard group menu
     */
    GROUP_STANDARD("Menu Gruppo"),
    
    /**
     * Menu for celebrations (graduations, communions, etc.)
     */
    CELEBRATION("Menu Celebrazione"),
    
    /**
     * Business lunch menu
     */
    BUSINESS("Menu Business"),
    
    /**
     * Wedding menu
     */
    WEDDING("Menu Matrimonio"),
    
    /**
     * Tourist group menu
     */
    TOUR("Menu Tour"),
    
    /**
     * Kids menu (for family events)
     */
    KIDS("Menu Bambini"),
    
    /**
     * Aperitivo/Buffet style
     */
    APERITIVO("Aperitivo/Buffet");
    
    private final String displayName;
    
    FixedPriceMenuType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
