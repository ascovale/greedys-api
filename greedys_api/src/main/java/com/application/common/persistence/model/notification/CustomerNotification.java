package com.application.common.persistence.model.notification;

import com.application.customer.persistence.model.Customer;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ⭐ NOTIFICA RECIPIENT-SPECIFICA PER CUSTOMER
 * 
 * Estende ANotification (che estende AEventNotification)
 * Ha: user_id (Customer ID), user_type = "CUSTOMER"
 * 
 * FLOW:
 * 1. ReservationConfirmedEvent generato (dopo admin accetta)
 * 2. CustomerNotificationListener ascolta
 * 3. Crea CustomerEventNotification (title="Prenotazione confermata", customer_id=456, NO userId)
 * 4. Crea CustomerNotification (user_id=456, user_type="CUSTOMER")
 * 5. ChannelPoller crea NotificationChannelSend:
 *    ├─ SMS (invia a customer.phone)
 *    ├─ EMAIL (invia a customer.email)
 *    ├─ PUSH (invia a customer FCM tokens)
 *    └─ WEBSOCKET (invia a browser customer)
 * 6. Notificatori leggono CustomerNotification(user_id) per estrarre i dati di contatto
 * 
 * DATABASE:
 * - Table: customer_notification (estende ANotification)
 * - PK: id
 * - FK: customer_id (relazione LAZY con Customer)
 * - Inherited: user_id, user_type, title, body, is_read, shared_read, creation_time
 * 
 * @author Greedy's System
 * @since 2025-01-20
 */
@Entity
@Table(name = "customer_notification")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public class CustomerNotification extends ANotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Relazione LAZY con Customer.
     * 
     * ⭐ NOTA: Usa userId ereditato da ANotification come FK
     * Questa relazione è OPZIONALE per comodità (evita di dover fare query separate)
     * 
     * @Transactional obbligatorio se accedi a customer.getEmail() etc.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private Customer customer;

    /**
     * Helper method per ottenere il Customer.
     * 
     * ⚠️ ATTENZIONE: Usa LAZY loading, la relazione viene caricata al primo accesso
     * Assicurati di essere in contesto @Transactional!
     * 
     * @return Il Customer associato a questa notifica, o null se userId non esiste
     */
    public Customer getCustomer() {
        return this.customer;
    }

}

