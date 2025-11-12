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
 * ⭐ NOTIFICA RECIPIENT-SPECIFICA PER ADMIN
 * 
 * Estende ANotification (che estende AEventNotification)
 * Ha: user_id (Admin ID), user_type = "ADMIN_USER"
 * 
 * FLOW:
 * 1. ReservationRequestedEvent generato
 * 2. AdminNotificationListener ascolta
 * 3. Crea AdminEventNotification (title="Nuova prenotazione", NO userId)
 * 4. Per ogni admin del sistema:
 *    ├─ Crea AdminNotification (user_id=1, user_type="ADMIN_USER")
 *    ├─ Crea AdminNotification (user_id=2, user_type="ADMIN_USER")
 *    └─ Crea AdminNotification (user_id=3, user_type="ADMIN_USER")
 * 5. Per ogni AdminNotification, ChannelPoller:
 *    ├─ Crea NotificationChannelSend (channel=EMAIL)
 *    ├─ Crea NotificationChannelSend (channel=PUSH)
 *    └─ Crea NotificationChannelSend (channel=WEBSOCKET)
 * 6. Email/Push/WebSocket leggono AdminNotification(user_id) per trovare email/device dell'admin
 * 
 * DATABASE:
 * - Table: admin_notification (estende ANotification)
 * - PK: id
 * - Inherited: user_id, user_type, title, body, is_read, shared_read, creation_time
 * 
 * @author Greedy's System
 * @since 2025-01-20
 */
@Entity
@Table(name = "admin_notification")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public class AdminNotification extends ANotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

}
