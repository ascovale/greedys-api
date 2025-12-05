package com.application.common.persistence.model.group.enums;

/**
 * Defines how items are selected within a course.
 */
public enum SelectionType {
    
    /**
     * All items in the course are included (e.g., "Antipasti misti")
     */
    ALL_INCLUDED("Tutti inclusi"),
    
    /**
     * Customer chooses N items from the list
     */
    CHOICE_OF_N("Scelta tra");
    
    private final String displayName;
    
    SelectionType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
