package com.application.common.persistence.model.group.enums;

/**
 * Type of course/portata in a fixed price menu.
 */
public enum CourseType {
    
    APPETIZER("Antipasto", 1),
    FIRST("Primo", 2),
    SECOND("Secondo", 3),
    SIDE("Contorno", 4),
    CHEESE("Formaggi", 5),
    FRUIT("Frutta", 6),
    DESSERT("Dessert", 7),
    BEVERAGE("Bevande", 8);
    
    private final String displayName;
    private final int defaultOrder;
    
    CourseType(String displayName, int defaultOrder) {
        this.displayName = displayName;
        this.defaultOrder = defaultOrder;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getDefaultOrder() {
        return defaultOrder;
    }
}
