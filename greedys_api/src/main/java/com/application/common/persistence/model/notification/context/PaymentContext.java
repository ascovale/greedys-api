package com.application.common.persistence.model.notification.context;

import java.math.BigDecimal;

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
 * Context condiviso per notifiche relative a pagamenti.
 * 
 * Usato da Customer, Restaurant e Admin con prospettive diverse:
 * - Customer: "Pagamento effettuato"
 * - Restaurant: "Ricevuto pagamento da cliente"
 * - Admin: "Commissione calcolata"
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentContext {
    
    /**
     * ID del pagamento
     */
    @Column(name = "ctx_payment_id")
    private Long paymentId;
    
    /**
     * Importo totale
     */
    @Column(name = "ctx_amount", precision = 10, scale = 2)
    private BigDecimal amount;
    
    /**
     * Commissione (per admin)
     */
    @Column(name = "ctx_commission", precision = 10, scale = 2)
    private BigDecimal commission;
    
    /**
     * Stato del pagamento
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ctx_payment_status", length = 20)
    private PaymentStatus status;
    
    /**
     * Metodo di pagamento
     */
    @Column(name = "ctx_payment_method", length = 50)
    private String paymentMethod;
    
    /**
     * ID prenotazione associata (opzionale)
     */
    @Column(name = "ctx_related_reservation_id")
    private Long relatedReservationId;
    
    /**
     * Stato del pagamento
     */
    public enum PaymentStatus {
        /**
         * Pagamento riuscito
         */
        SUCCESS,
        
        /**
         * Pagamento fallito
         */
        FAILED,
        
        /**
         * Pagamento in attesa
         */
        PENDING,
        
        /**
         * Pagamento rimborsato
         */
        REFUNDED
    }
}
