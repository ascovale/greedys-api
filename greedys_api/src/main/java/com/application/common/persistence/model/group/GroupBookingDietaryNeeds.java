package com.application.common.persistence.model.group;

import com.application.common.persistence.model.group.enums.DietaryRequirement;
import jakarta.persistence.*;
import lombok.*;

/**
 * Esigenze alimentari per una prenotazione di gruppo.
 * <p>
 * Traccia quanti partecipanti hanno specifiche esigenze alimentari.
 * Es: "3 vegetariani, 2 celiaci, 1 halal"
 */
@Entity
@Table(name = "group_booking_dietary_needs", indexes = {
    @Index(name = "idx_dietary_needs_booking", columnList = "group_booking_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupBookingDietaryNeeds {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_booking_id", nullable = false)
    private GroupBooking groupBooking;

    /**
     * Tipo di esigenza alimentare
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "dietary_requirement", nullable = false, length = 30)
    private DietaryRequirement dietaryRequirement;

    /**
     * Numero di partecipanti con questa esigenza
     */
    @Column(name = "pax_count", nullable = false)
    private Integer paxCount;

    /**
     * Note aggiuntive (es. "allergia grave alle arachidi")
     */
    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Severità: true = critico (allergia), false = preferenza
     */
    @Column(name = "is_critical")
    @Builder.Default
    private Boolean isCritical = false;
}
