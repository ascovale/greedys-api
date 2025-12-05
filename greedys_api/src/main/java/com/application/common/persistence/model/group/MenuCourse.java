package com.application.common.persistence.model.group;

import java.util.ArrayList;
import java.util.List;

import com.application.common.persistence.model.group.enums.CourseType;
import com.application.common.persistence.model.group.enums.SelectionType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A course (portata) within a FixedPriceMenu.
 * 
 * Examples:
 * - "Antipasti" with ALL_INCLUDED (all items served)
 * - "Primi" with CHOICE_OF_1 (customer picks one)
 * - "Dessert" with CHOICE_OF_1
 */
@Entity
@Table(name = "menu_course")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_price_menu_id", nullable = false)
    private FixedPriceMenu fixedPriceMenu;

    // ═══════════════════════════════════════════════════════════════════════════
    // COURSE TYPE
    // ═══════════════════════════════════════════════════════════════════════════

    @Enumerated(EnumType.STRING)
    @Column(name = "course_type", nullable = false)
    private CourseType courseType;

    /**
     * Custom name for the course (overrides courseType.displayName if set)
     * e.g., "Antipasti della Casa" instead of just "Antipasto"
     */
    @Column(length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ═══════════════════════════════════════════════════════════════════════════
    // SELECTION
    // ═══════════════════════════════════════════════════════════════════════════

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_type", nullable = false)
    @Builder.Default
    private SelectionType selectionType = SelectionType.CHOICE_OF_N;

    /**
     * How many items customer can choose (only for CHOICE_OF_N)
     * e.g., 1 = pick one, 2 = pick two
     */
    @Column(name = "selection_count")
    @Builder.Default
    private Integer selectionCount = 1;

    // ═══════════════════════════════════════════════════════════════════════════
    // ITEMS
    // ═══════════════════════════════════════════════════════════════════════════

    @OneToMany(mappedBy = "menuCourse", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<CourseItem> items = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════════════════════
    // DISPLAY
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Order in which this course appears in the menu
     */
    @Column(name = "display_order")
    private Integer displayOrder;

    /**
     * Whether this course is optional
     */
    @Builder.Default
    private Boolean optional = false;

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get the display name for this course
     */
    public String getDisplayName() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        return courseType.getDisplayName();
    }

    /**
     * Add an item to this course
     */
    public void addItem(CourseItem item) {
        items.add(item);
        item.setMenuCourse(this);
    }

    /**
     * Remove an item from this course
     */
    public void removeItem(CourseItem item) {
        items.remove(item);
        item.setMenuCourse(null);
    }

    /**
     * Check if all items in this course are included
     */
    public boolean isAllIncluded() {
        return selectionType == SelectionType.ALL_INCLUDED;
    }

    /**
     * Get count of vegetarian items
     */
    public long countVegetarianItems() {
        return items.stream()
                .filter(CourseItem::isVegetarianItem)
                .count();
    }

    /**
     * Get count of gluten-free items
     */
    public long countGlutenFreeItems() {
        return items.stream()
                .filter(CourseItem::isGlutenFreeItem)
                .count();
    }

    /**
     * Check if course has vegetarian options
     */
    public boolean hasVegetarianOption() {
        return countVegetarianItems() > 0;
    }

    /**
     * Check if course has gluten-free options
     */
    public boolean hasGlutenFreeOption() {
        return countGlutenFreeItems() > 0;
    }
}
