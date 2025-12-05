package com.application.common.persistence.model.group;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.application.common.persistence.model.group.enums.FixedPriceMenuType;
import com.application.common.persistence.model.group.enums.MenuVisibility;
import com.application.restaurant.persistence.model.Restaurant;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fixed price menu for groups.
 * 
 * Unlike the regular Menu which prices individual dishes,
 * FixedPriceMenu has a single price per person that includes
 * all courses.
 * 
 * Features:
 * - Dual pricing: basePrice (B2C) and agencyPrice (B2B)
 * - Visibility control: who can see this menu
 * - Structured courses with selection options
 * - Optional supplements for premium items
 */
@Entity
@Table(name = "fixed_price_menu")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedPriceMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ═══════════════════════════════════════════════════════════════════════════
    // PRICING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Base price per person (public/customer price)
     */
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    /**
     * Agency price per person (B2B, may be discounted)
     * If null, agency sees basePrice
     */
    @Column(name = "agency_price", precision = 10, scale = 2)
    private BigDecimal agencyPrice;

    /**
     * Children price per person (usually discounted)
     * If null, use a percentage of base price
     */
    @Column(name = "children_price", precision = 10, scale = 2)
    private BigDecimal childrenPrice;

    /**
     * Children price as percentage of adult price (e.g., 0.5 = 50%)
     * Used if childrenPrice is null
     */
    @Column(name = "children_price_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal childrenPricePercentage = new BigDecimal("0.50");

    // ═══════════════════════════════════════════════════════════════════════════
    // CAPACITY
    // ═══════════════════════════════════════════════════════════════════════════

    @Column(name = "minimum_pax")
    private Integer minimumPax;

    @Column(name = "maximum_pax")
    private Integer maximumPax;

    // ═══════════════════════════════════════════════════════════════════════════
    // VISIBILITY & TYPE
    // ═══════════════════════════════════════════════════════════════════════════

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MenuVisibility visibility = MenuVisibility.BOTH;

    @Enumerated(EnumType.STRING)
    @Column(name = "menu_type", nullable = false)
    @Builder.Default
    private FixedPriceMenuType menuType = FixedPriceMenuType.GROUP_STANDARD;

    // ═══════════════════════════════════════════════════════════════════════════
    // COURSES (portate)
    // ═══════════════════════════════════════════════════════════════════════════

    @OneToMany(mappedBy = "fixedPriceMenu", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<MenuCourse> courses = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════════════════════
    // INCLUSIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Column(name = "includes_beverages")
    @Builder.Default
    private Boolean includesBeverages = false;

    @Column(name = "beverage_description", length = 500)
    private String beverageDescription;  // e.g., "1/4 vino, acqua, caffè"

    @Column(name = "includes_private_room")
    @Builder.Default
    private Boolean includesPrivateRoom = false;

    @Column(name = "includes_decoration")
    @Builder.Default
    private Boolean includesDecoration = false;

    @Column(name = "includes_music")
    @Builder.Default
    private Boolean includesMusic = false;

    @Column(name = "includes_parking")
    @Builder.Default
    private Boolean includesParking = false;

    @Column(name = "extra_notes", columnDefinition = "TEXT")
    private String extraNotes;

    // ═══════════════════════════════════════════════════════════════════════════
    // VALIDITY
    // ═══════════════════════════════════════════════════════════════════════════

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Builder.Default
    private Boolean enabled = true;

    // ═══════════════════════════════════════════════════════════════════════════
    // AUDIT
    // ═══════════════════════════════════════════════════════════════════════════

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Integer version;

    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE CALLBACKS
    // ═══════════════════════════════════════════════════════════════════════════

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get the effective price for a given booker type
     */
    public BigDecimal getPriceForAgency() {
        return agencyPrice != null ? agencyPrice : basePrice;
    }

    /**
     * Get the effective children price
     */
    public BigDecimal getEffectiveChildrenPrice() {
        if (childrenPrice != null) {
            return childrenPrice;
        }
        if (childrenPricePercentage != null) {
            return basePrice.multiply(childrenPricePercentage);
        }
        return basePrice.multiply(new BigDecimal("0.50")); // Default 50%
    }

    /**
     * Check if menu is currently valid
     */
    public boolean isCurrentlyValid() {
        if (!enabled) return false;
        LocalDate today = LocalDate.now();
        if (validFrom != null && today.isBefore(validFrom)) return false;
        if (validTo != null && today.isAfter(validTo)) return false;
        return true;
    }

    /**
     * Check if pax count is within allowed range
     */
    public boolean isPaxAllowed(int pax) {
        if (minimumPax != null && pax < minimumPax) return false;
        if (maximumPax != null && pax > maximumPax) return false;
        return true;
    }

    /**
     * Add a course to this menu
     */
    public void addCourse(MenuCourse course) {
        courses.add(course);
        course.setFixedPriceMenu(this);
    }

    /**
     * Remove a course from this menu
     */
    public void removeCourse(MenuCourse course) {
        courses.remove(course);
        course.setFixedPriceMenu(null);
    }

    /**
     * Calculate discount percentage for agency price
     */
    public BigDecimal getAgencyDiscountPercentage() {
        if (agencyPrice == null || agencyPrice.compareTo(basePrice) >= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = basePrice.subtract(agencyPrice);
        return discount.divide(basePrice, 4, java.math.RoundingMode.HALF_UP)
                       .multiply(new BigDecimal("100"));
    }
}
