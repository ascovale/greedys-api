package com.application.common.persistence.model.notification.context;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Context condiviso per notifiche relative a prenotazioni.
 * 
 * Usato da Customer, Restaurant e Admin per rappresentare
 * gli stessi dati business della prenotazione.
 * 
 * La presentazione (title/body) sarà diversa per ogni recipient,
 * ma i dati della prenotazione sono gli stessi.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationContext {
    
    /**
     * ID della prenotazione
     */
    @Column(name = "ctx_reservation_id")
    private Long reservationId;
    
    /**
     * Azione sulla prenotazione
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ctx_reservation_action", length = 30)
    private ReservationAction action;
    
    /**
     * Nome del cliente
     */
    @Column(name = "ctx_customer_name", length = 100)
    private String customerName;
    
    /**
     * Nome del ristorante
     */
    @Column(name = "ctx_restaurant_name", length = 100)
    private String restaurantName;
    
    /**
     * Numero coperti
     */
    @Column(name = "ctx_pax")
    private Integer pax;
    
    /**
     * Data prenotazione
     */
    @Column(name = "ctx_reservation_date")
    private LocalDate reservationDate;
    
    /**
     * Orario slot (es: "19:00-21:00")
     */
    @Column(name = "ctx_slot_time", length = 20)
    private String slotTime;
    
    /**
     * Numero tavolo (se assegnato)
     */
    @Column(name = "ctx_table_number")
    private Integer tableNumber;
    
    /**
     * Urgenza (es: prenotazione last-minute)
     */
    @Column(name = "ctx_is_urgent")
    @Builder.Default
    private Boolean urgent = false;
    
    /**
     * Azioni possibili su una prenotazione
     */
    public enum ReservationAction {
        /**
         * Nuova richiesta di prenotazione
         */
        REQUEST,
        
        /**
         * Prenotazione accettata
         */
        ACCEPTED,
        
        /**
         * Prenotazione rifiutata
         */
        REJECTED,
        
        /**
         * Prenotazione modificata (orario/pax)
         */
        MODIFIED,
        
        /**
         * Prenotazione cancellata dal cliente
         */
        CANCELLED,
        
        /**
         * Cliente non si è presentato
         */
        NO_SHOW,
        
        /**
         * Cliente seduto al tavolo
         */
        SEATED,
        
        /**
         * Cliente ha finito (tavolo libero)
         */
        UNSEATED
    }
}
