package com.application.common.persistence.model.notification;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

}
