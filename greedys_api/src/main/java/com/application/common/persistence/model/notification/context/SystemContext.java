package com.application.common.persistence.model.notification.context;

import java.time.LocalDateTime;

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
 * Context per notifiche di sistema.
 * 
 * Usato principalmente da Admin per notifiche di manutenzione,
 * aggiornamenti, alert di sistema, etc.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemContext {
    
    /**
     * Tipo di notifica di sistema
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ctx_system_type", length = 30)
    private SystemNotificationType type;
    
    /**
     * Severità dell'alert
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ctx_severity", length = 20)
    private Severity severity;
    
    /**
     * Componente coinvolto (es: "Database", "Payment Service")
     */
    @Column(name = "ctx_component", length = 100)
    private String component;
    
    /**
     * Timestamp dell'evento (se diverso da creationTime della notifica)
     */
    @Column(name = "ctx_event_timestamp")
    private LocalDateTime eventTimestamp;
    
    /**
     * Link ad azione consigliata (es: URL dashboard)
     */
    @Column(name = "ctx_action_url", length = 500)
    private String actionUrl;
    
    /**
     * Tipo di notifica di sistema
     */
    public enum SystemNotificationType {
        /**
         * Manutenzione programmata
         */
        MAINTENANCE_SCHEDULED,
        
        /**
         * Manutenzione in corso
         */
        MAINTENANCE_ONGOING,
        
        /**
         * Aggiornamento disponibile
         */
        UPDATE_AVAILABLE,
        
        /**
         * Alert di sicurezza
         */
        SECURITY_ALERT,
        
        /**
         * Errore di sistema
         */
        SYSTEM_ERROR,
        
        /**
         * Backup completato
         */
        BACKUP_COMPLETED,
        
        /**
         * Performance degradate
         */
        PERFORMANCE_DEGRADED
    }
    
    /**
     * Severità dell'alert
     */
    public enum Severity {
        /**
         * Informativo (non richiede azione)
         */
        INFO,
        
        /**
         * Warning (monitorare)
         */
        WARNING,
        
        /**
         * Errore (richiede intervento)
         */
        ERROR,
        
        /**
         * Critico (intervento immediato)
         */
        CRITICAL
    }
}
