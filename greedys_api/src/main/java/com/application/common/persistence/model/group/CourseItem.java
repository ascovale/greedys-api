package com.application.common.persistence.model.group;

import java.math.BigDecimal;

import com.application.restaurant.persistence.model.menu.Dish;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An item within a MenuCourse.
 * 
 * Can either:
 * - Reference an existing Dish from the restaurant's catalog
 * - Define a custom dish specific to this menu (dish = null)
 * 
 * Supports supplements for premium items (e.g., +€15 for Bistecca).
 */
@Entity
@Table(name = "course_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_course_id", nullable = false)
    private MenuCourse menuCourse;

    // ═══════════════════════════════════════════════════════════════════════════
    // DISH REFERENCE (optional)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Reference to existing dish in restaurant catalog.
     * If null, use customName/customDescription instead.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id")
    private Dish dish;

    /**
     * Custom name (used when dish is null or to override dish name)
     */
    @Column(name = "custom_name", length = 100)
    private String customName;

    /**
     * Custom description (used when dish is null or to override)
     */
    @Column(name = "custom_description", columnDefinition = "TEXT")
    private String customDescription;

    // ═══════════════════════════════════════════════════════════════════════════
    // SUPPLEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Additional charge for this item (null = no supplement)
     * e.g., +€15 for premium items like Bistecca
     */
    @Column(name = "supplement_price", precision = 10, scale = 2)
    private BigDecimal supplementPrice;

    // ═══════════════════════════════════════════════════════════════════════════
    // DIETARY TAGS (override dish tags if set)
    // ═══════════════════════════════════════════════════════════════════════════

    @Column(name = "is_vegetarian")
    private Boolean isVegetarian;

    @Column(name = "is_vegan")
    private Boolean isVegan;

    @Column(name = "is_gluten_free")
    private Boolean isGlutenFree;

    @Column(name = "is_lactose_free")
    private Boolean isLactoseFree;

    @Column(name = "is_halal")
    private Boolean isHalal;

    @Column(name = "is_kosher")
    private Boolean isKosher;

    @Column(name = "contains_alcohol")
    private Boolean containsAlcohol;

    @Column(name = "contains_pork")
    private Boolean containsPork;

    // ═══════════════════════════════════════════════════════════════════════════
    // DISPLAY
    // ═══════════════════════════════════════════════════════════════════════════

    @Column(name = "display_order")
    private Integer displayOrder;

    /**
     * Whether this item is currently available
     */
    @Builder.Default
    private Boolean available = true;

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get the name (from dish if linked, otherwise custom)
     */
    public String getName() {
        if (customName != null && !customName.isBlank()) {
            return customName;
        }
        if (dish != null) {
            return dish.getName();
        }
        return "Unnamed Item";
    }

    /**
     * Get the description (from dish if linked, otherwise custom)
     */
    public String getDescription() {
        if (customDescription != null && !customDescription.isBlank()) {
            return customDescription;
        }
        if (dish != null) {
            return dish.getDescription();
        }
        return null;
    }

    /**
     * Check if this item is vegetarian.
     * Uses local override if set, otherwise from dish.
     */
    public boolean isVegetarianItem() {
        if (this.isVegetarian != null) {
            return this.isVegetarian;
        }
        if (dish != null && dish.getIsVegetarian() != null) {
            return dish.getIsVegetarian();
        }
        return false;
    }

    /**
     * Check if this item is vegan.
     */
    public boolean isVeganItem() {
        if (this.isVegan != null) {
            return this.isVegan;
        }
        if (dish != null && dish.getIsVegan() != null) {
            return dish.getIsVegan();
        }
        return false;
    }

    /**
     * Check if this item is gluten-free.
     */
    public boolean isGlutenFreeItem() {
        if (this.isGlutenFree != null) {
            return this.isGlutenFree;
        }
        if (dish != null && dish.getIsGlutenFree() != null) {
            return dish.getIsGlutenFree();
        }
        return false;
    }

    /**
     * Check if this item is lactose-free.
     */
    public boolean isLactoseFreeItem() {
        if (this.isLactoseFree != null) {
            return this.isLactoseFree;
        }
        if (dish != null && dish.getIsLactoseFree() != null) {
            return dish.getIsLactoseFree();
        }
        return false;
    }

    /**
     * Check if this item is halal.
     */
    public boolean isHalalItem() {
        if (this.isHalal != null) {
            return this.isHalal;
        }
        if (dish != null && dish.getIsHalal() != null) {
            return dish.getIsHalal();
        }
        return false;
    }

    /**
     * Check if this item is kosher.
     */
    public boolean isKosherItem() {
        if (this.isKosher != null) {
            return this.isKosher;
        }
        if (dish != null && dish.getIsKosher() != null) {
            return dish.getIsKosher();
        }
        return false;
    }

    /**
     * Check if item has a supplement
     */
    public boolean hasSupplement() {
        return supplementPrice != null && supplementPrice.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if this item references an existing dish
     */
    public boolean isLinkedToDish() {
        return dish != null;
    }
}
