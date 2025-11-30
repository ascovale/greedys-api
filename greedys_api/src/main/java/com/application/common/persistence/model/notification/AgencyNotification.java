package com.application.common.persistence.model.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.application.agency.persistence.model.user.AgencyUser;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ⭐ NOTIFICA RECIPIENT-SPECIFICA PER AGENCY
 * 
 * Estende ANotification con agencyUserId (FK to AgencyUser)
 * 
 * REFACTORING con JOINED inheritance:
 * - PRIMA: agencyNotification con (user_id: Long, user_type: "AGENCY_USER")
 *   - Ambiguo: quale tabella contiene l'utente?
 * 
 * - DOPO: agencyNotification con (agencyUserId FK to AgencyUser.id)
 *   - Type-safe: FK referenzia direttamente AgencyUser table
 *   - Polymorphic: getRecipientId() ritorna agencyUserId
 * 
 * FLOW:
 * 1. ReservationRequestedEvent oppure ServiceActivatedEvent generato
 * 2. AgencyNotificationListener ascolta
 * 3. Crea AgencyEventNotification (title="Evento agenzia", agency_id=789, NO userId)
 * 4. Per ogni utente dell'agenzia:
 *    ├─ Crea AgencyNotification (agencyUserId=100)
 *    ├─ Crea AgencyNotification (agencyUserId=101)
 *    └─ Crea AgencyNotification (agencyUserId=102)
 * 5. ChannelPoller crea NotificationChannelSend per ogni canale (EMAIL, PUSH, WEBSOCKET)
 * 6. Notificatori leggono AgencyNotification(agencyUserId) per estrarre dati di contatto
 * 
 * DATABASE:
 * - Table: agency_notification (estende ANotification)
 * - Columns: id, agency_user_id (FK), ... (inherited da ANotification)
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

    /**
     * FK to AgencyUser
     * 
     * Con JOINED inheritance, questo referenzia direttamente AbstractUser.id
     * (poiché AgencyUser estende AbstractUser)
     */
    @Column(name = "agency_user_id", nullable = false)
    private Long agencyUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_user_id", insertable = false, updatable = false)
    private AgencyUser agencyUser;

    @Override
    public Long getRecipientId() {
        return agencyUserId;
    }

    @Override
    public String getRecipientType() {
        return "AGENCY_USER";
    }
}
