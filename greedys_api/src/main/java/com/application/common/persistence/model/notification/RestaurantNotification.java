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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.application.restaurant.persistence.model.user.RUser;

/**
 * ⭐ NOTIFICA RECIPIENT-SPECIFICA PER STAFF RISTORANTE
 * 
 * Estende ANotification (che estende AEventNotification)
 * Ha: user_id (RUser ID dello staff), user_type = "RESTAURANT_USER"
 * 
 * FLOW COMPLETO:
 * 1. ReservationRequestedEvent generato
 * 2. RestaurantNotificationListener ascolta
 * 3. Crea RestaurantEventNotification (title="Nuova prenotazione", NO userId)
 * 4. Per ogni staff (RUser) del ristorante:
 *    ├─ Crea RestaurantNotification (user_id=50, user_type="RESTAURANT_USER")
 *    ├─ Crea RestaurantNotification (user_id=51, user_type="RESTAURANT_USER")
 *    └─ Crea RestaurantNotification (user_id=52, user_type="RESTAURANT_USER")
 * 5. Per ogni RestaurantNotification, ChannelPoller:
 *    ├─ Crea NotificationChannelSend (channel=SMS)
 *    ├─ Crea NotificationChannelSend (channel=EMAIL)
 *    ├─ Crea NotificationChannelSend (channel=PUSH)
 *    └─ Crea NotificationChannelSend (channel=WEBSOCKET)
 * 6. I notificatori (SMS, Email, Push, WebSocket) leggono RestaurantNotification(user_id) 
 *    per trovare phone/email/fcm_token dello staff
 * 
 * DATABASE:
 * - Table: restaurant_notification (estende ANotification)
 * - PK: id
 * - FK: restaurant_id (quale ristorante ha ricevuto questa notifica)
 * - FK: user_id (relazione LAZY con RUser dello staff)
 * - Inherited: user_id, user_type, title, body, is_read, shared_read, creation_time
 * 
 * @author Greedy's System
 * @since 2025-01-20
 */
@Entity
@Table(name = "restaurant_notification")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public class RestaurantNotification extends ANotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * FK al ristorante che ha ricevuto questa notifica
     * 
     * Usato per:
     * - Contesto: "Notifica per quale ristorante?"
     * - Query: SELECT * FROM restaurant_notification WHERE restaurant_id = ? AND is_read = false
     * - Audit: Log di tutte le notifiche per un ristorante specifico
     */
    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    /**
     * Relazione LAZY con RUser (staff del ristorante).
     * 
     * ⭐ NOTA: Usa userId ereditato da ANotification come FK
     * Questa relazione è OPZIONALE per comodità (evita di dover fare query separate)
     * 
     * @Transactional obbligatorio se accedi a rUser.getEmail() etc.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private RUser rUser;

    /**
     * Helper method per ottenere l'RUser (staff).
     * 
     * ⚠️ ATTENZIONE: Usa LAZY loading, la relazione viene caricata al primo accesso
     * Assicurati di essere in contesto @Transactional!
     * 
     * @return L'RUser associato a questa notifica, o null se userId non esiste
     */
    public RUser getRUser() {
        return this.rUser;
    }

}

