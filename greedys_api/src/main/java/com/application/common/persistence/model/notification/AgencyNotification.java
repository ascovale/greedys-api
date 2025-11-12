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
 * ⭐ NOTIFICA RECIPIENT-SPECIFICA PER AGENCY
 * 
 * Estende ANotification (che estende AEventNotification)
 * Ha: user_id (Agency User ID), user_type = "AGENCY_USER"
 * 
 * FLOW:
 * 1. ReservationRequestedEvent oppure ServiceActivatedEvent generato
 * 2. AgencyNotificationListener ascolta
 * 3. Crea AgencyEventNotification (title="Evento agenzia", agency_id=789, NO userId)
 * 4. Per ogni utente dell'agenzia:
 *    ├─ Crea AgencyNotification (user_id=100, user_type="AGENCY_USER")
 *    ├─ Crea AgencyNotification (user_id=101, user_type="AGENCY_USER")
 *    └─ Crea AgencyNotification (user_id=102, user_type="AGENCY_USER")
 * 5. ChannelPoller crea NotificationChannelSend per ogni canale (EMAIL, PUSH, WEBSOCKET)
 * 6. Notificatori leggono AgencyNotification(user_id) per estrarre dati di contatto
 * 
 * DATABASE:
 * - Table: agency_notification (estende ANotification)
 * - PK: id
 * - Inherited: user_id, user_type, title, body, is_read, shared_read, creation_time
 * 
 * @author Greedy's System
 * @since 2025-01-20
 */
@Entity
@Table(name = "agency_notification")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public class AgencyNotification extends ANotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

}
