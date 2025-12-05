package com.application.common.service.notification.preferences;

import com.application.common.persistence.dao.notification.*;
import com.application.common.persistence.model.notification.preferences.*;
import com.application.common.persistence.model.notification.preferences.HubNotificationBlock.HubType;
import com.application.common.persistence.model.notification.preferences.OrganizationNotificationBlock.OrganizationType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;
import java.util.*;

/**
 * 🔒 NotificationBlockService
 * 
 * Servizio centrale per verificare blocchi notifiche a tutti i livelli.
 * Implementa logica "opt-out": default = abilitato, record = bloccato.
 * 
 * GERARCHIA BLOCCHI (dal più generale al più specifico):
 * 
 * Livello 0: Global Block (Admin blocca per TUTTI)
 *     ↓
 * Livello 1: Event Rules (Admin definisce regole, canali mandatory)
 *     ↓
 * Livello 2: Organization Block (Restaurant/Agency blocca per suoi utenti)
 *     ↓
 * Livello 3: Hub Block (Hub blocca per tutti i suoi account)
 *     ↓
 * Livello 4: User Block (Singolo utente blocca, se permesso)
 * 
 * FORMULA:
 * canSend = !globalBlocked 
 *           && (isMandatory || (!orgBlocked && !hubBlocked && !userBlocked))
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationBlockService {

    private final GlobalNotificationBlockDAO globalBlockDAO;
    private final EventTypeNotificationRuleDAO eventRuleDAO;
    private final OrganizationNotificationBlockDAO orgBlockDAO;
    private final HubNotificationBlockDAO hubBlockDAO;
    private final UserNotificationBlockDAO userBlockDAO;
    private final ObjectMapper objectMapper;

    // ========================================================================
    // METODO PRINCIPALE: Verifica se notifica può essere inviata
    // ========================================================================

    /**
     * Verifica se una notifica può essere inviata a un utente su un canale.
     * 
     * @param eventType    Tipo evento (es: "RESERVATION_CONFIRMED")
     * @param channel      Canale (es: "EMAIL", "PUSH", "WEBSOCKET")
     * @param userId       ID utente destinatario
     * @param context      Contesto aggiuntivo (orgType, orgId, hubType, hubId)
     * @return true se la notifica può essere inviata, false se bloccata
     */
    public boolean canSendNotification(
            String eventType,
            String channel,
            Long userId,
            NotificationContext context
    ) {
        log.debug("🔍 Checking notification block: eventType={}, channel={}, userId={}", 
            eventType, channel, userId);

        // LIVELLO 0: Blocco globale
        if (isGloballyBlocked(eventType)) {
            log.debug("🚫 Livello 0: EventType {} è bloccato globalmente", eventType);
            return false;
        }

        // LIVELLO 1: Regole evento - canale mandatory?
        if (isMandatoryChannel(eventType, channel)) {
            log.debug("✅ Livello 1: Canale {} è MANDATORY per {}, bypass altri blocchi", channel, eventType);
            return true; // Canale mandatory bypassa livelli 2-4
        }

        // LIVELLO 1: Utente può disabilitare?
        boolean userCanDisable = canUserDisable(eventType);

        // LIVELLO 2: Blocco organizzazione
        if (context != null && context.hasOrganization()) {
            if (isOrganizationBlocked(context.getOrgType(), context.getOrgId(), eventType, channel)) {
                log.debug("🚫 Livello 2: Bloccato da {} {}", context.getOrgType(), context.getOrgId());
                return false;
            }
        }

        // LIVELLO 3: Blocco hub
        if (context != null && context.hasHub()) {
            if (isHubBlocked(context.getHubType(), context.getHubId(), eventType, channel)) {
                log.debug("🚫 Livello 3: Bloccato da hub {} {}", context.getHubType(), context.getHubId());
                return false;
            }
        }

        // LIVELLO 4: Blocco utente (solo se userCanDisable=true)
        if (userCanDisable && userId != null) {
            if (isUserBlocked(userId, eventType, channel)) {
                log.debug("🚫 Livello 4: Bloccato da utente {}", userId);
                return false;
            }
        }

        log.debug("✅ Notifica permessa: eventType={}, channel={}, userId={}", eventType, channel, userId);
        return true;
    }

    /**
     * Versione semplificata senza contesto (per Customer che non hanno org/hub)
     */
    public boolean canSendNotification(String eventType, String channel, Long userId) {
        return canSendNotification(eventType, channel, userId, null);
    }

    // ========================================================================
    // LIVELLO 0: BLOCCO GLOBALE
    // ========================================================================

    /**
     * Verifica se un eventType è bloccato globalmente
     */
    public boolean isGloballyBlocked(String eventType) {
        return globalBlockDAO.isEventTypeBlocked(eventType, Instant.now());
    }

    // ========================================================================
    // LIVELLO 1: REGOLE EVENTO
    // ========================================================================

    /**
     * Verifica se un canale è mandatory per un eventType
     */
    public boolean isMandatoryChannel(String eventType, String channel) {
        List<String> mandatoryChannels = getMandatoryChannels(eventType);
        return mandatoryChannels.contains(channel);
    }

    /**
     * Ottiene i canali mandatory per un eventType
     */
    public List<String> getMandatoryChannels(String eventType) {
        List<EventTypeNotificationRule> rules = eventRuleDAO.findRulesForEventType(eventType);
        
        Set<String> mandatory = new HashSet<>();
        for (EventTypeNotificationRule rule : rules) {
            List<String> channels = parseJsonArray(rule.getMandatoryChannels());
            mandatory.addAll(channels);
        }
        
        return new ArrayList<>(mandatory);
    }

    /**
     * Verifica se l'utente può disabilitare un eventType
     */
    public boolean canUserDisable(String eventType) {
        return eventRuleDAO.canUserDisable(eventType);
    }

    // ========================================================================
    // LIVELLO 2: BLOCCO ORGANIZZAZIONE
    // ========================================================================

    /**
     * Verifica se un canale è bloccato a livello organizzazione
     */
    public boolean isOrganizationBlocked(
            OrganizationType orgType, 
            Long orgId, 
            String eventType, 
            String channel
    ) {
        if (orgType == null || orgId == null) return false;
        
        // Check quiet hours
        List<OrganizationNotificationBlock> blocks = 
            orgBlockDAO.findActiveBlocksForEventType(orgType, orgId, eventType);
        
        for (OrganizationNotificationBlock block : blocks) {
            // Check quiet hours
            if (isInQuietHours(block.getQuietHoursEnabled(), 
                              block.getQuietHoursStart(), 
                              block.getQuietHoursEnd())) {
                return true;
            }
            
            // Check channel block
            List<String> blockedChannels = parseJsonArray(block.getBlockedChannels());
            if (blockedChannels.isEmpty() || blockedChannels.contains(channel)) {
                return true;
            }
        }
        
        return false;
    }

    // ========================================================================
    // LIVELLO 3: BLOCCO HUB
    // ========================================================================

    /**
     * Verifica se un canale è bloccato a livello hub
     */
    public boolean isHubBlocked(HubType hubType, Long hubId, String eventType, String channel) {
        if (hubType == null || hubId == null) return false;
        
        List<HubNotificationBlock> blocks = 
            hubBlockDAO.findActiveBlocksForEventType(hubType, hubId, eventType);
        
        for (HubNotificationBlock block : blocks) {
            // Check quiet hours
            if (isInQuietHours(block.getQuietHoursEnabled(), 
                              block.getQuietHoursStart(), 
                              block.getQuietHoursEnd())) {
                return true;
            }
            
            // Check channel block
            List<String> blockedChannels = parseJsonArray(block.getBlockedChannels());
            if (blockedChannels.isEmpty() || blockedChannels.contains(channel)) {
                return true;
            }
        }
        
        return false;
    }

    // ========================================================================
    // LIVELLO 4: BLOCCO UTENTE
    // ========================================================================

    /**
     * Verifica se un canale è bloccato a livello utente
     */
    public boolean isUserBlocked(Long userId, String eventType, String channel) {
        if (userId == null) return false;
        
        List<UserNotificationBlock> blocks = 
            userBlockDAO.findActiveBlocksForEventType(userId, eventType);
        
        for (UserNotificationBlock block : blocks) {
            // Check quiet hours
            if (isInQuietHours(block.getQuietHoursEnabled(), 
                              block.getQuietHoursStart(), 
                              block.getQuietHoursEnd())) {
                return true;
            }
            
            // Check channel block
            List<String> blockedChannels = parseJsonArray(block.getBlockedChannels());
            if (blockedChannels.isEmpty() || blockedChannels.contains(channel)) {
                return true;
            }
        }
        
        return false;
    }

    // ========================================================================
    // METODI DI UTILITÀ
    // ========================================================================

    /**
     * Calcola i canali finali per un utente/evento (rimuove quelli bloccati)
     */
    public List<String> getAvailableChannels(
            String eventType,
            List<String> requestedChannels,
            Long userId,
            NotificationContext context
    ) {
        List<String> available = new ArrayList<>();
        
        for (String channel : requestedChannels) {
            if (canSendNotification(eventType, channel, userId, context)) {
                available.add(channel);
            }
        }
        
        return available;
    }

    /**
     * Verifica se siamo in quiet hours
     */
    private boolean isInQuietHours(Boolean enabled, LocalTime start, LocalTime end) {
        if (enabled == null || !enabled || start == null || end == null) {
            return false;
        }
        
        LocalTime now = LocalTime.now();
        
        // Gestisce il caso in cui quiet hours attraversa mezzanotte
        if (start.isBefore(end)) {
            // Es: 22:00 - 08:00 NON attraversa mezzanotte? No, questo è 22:00 - 08:00 attraversa
            // Es: 09:00 - 17:00 non attraversa
            return !now.isBefore(start) && now.isBefore(end);
        } else {
            // Attraversa mezzanotte: es 22:00 - 08:00
            return !now.isBefore(start) || now.isBefore(end);
        }
    }

    /**
     * Parse JSON array di canali
     */
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) {
            return Collections.emptyList();
        }
        
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("⚠️ Errore parsing JSON array: {}", json, e);
            return Collections.emptyList();
        }
    }

    // ========================================================================
    // CONTEXT CLASS
    // ========================================================================

    /**
     * Contesto per la verifica blocchi (org, hub)
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NotificationContext {
        private OrganizationType orgType;
        private Long orgId;
        private HubType hubType;
        private Long hubId;

        public boolean hasOrganization() {
            return orgType != null && orgId != null;
        }

        public boolean hasHub() {
            return hubType != null && hubId != null;
        }

        // Factory methods
        public static NotificationContext forRestaurant(Long restaurantId) {
            return NotificationContext.builder()
                .orgType(OrganizationType.RESTAURANT)
                .orgId(restaurantId)
                .build();
        }

        public static NotificationContext forRestaurant(Long restaurantId, Long hubId) {
            return NotificationContext.builder()
                .orgType(OrganizationType.RESTAURANT)
                .orgId(restaurantId)
                .hubType(HubType.RESTAURANT_USER_HUB)
                .hubId(hubId)
                .build();
        }

        public static NotificationContext forAgency(Long agencyId) {
            return NotificationContext.builder()
                .orgType(OrganizationType.AGENCY)
                .orgId(agencyId)
                .build();
        }

        public static NotificationContext forAgency(Long agencyId, Long hubId) {
            return NotificationContext.builder()
                .orgType(OrganizationType.AGENCY)
                .orgId(agencyId)
                .hubType(HubType.AGENCY_USER_HUB)
                .hubId(hubId)
                .build();
        }
    }
}
