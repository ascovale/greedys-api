package com.application.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * ⭐ CONFIGURATION PROPERTIES FOR NOTIFICATION OUTBOX POLLER
 * 
 * Proprietà di configurazione per controllare il comportamento del poller
 * tramite application.properties:
 * 
 * notification.outbox.multi-poller.enabled=true/false
 * notification.outbox.fast-poller.delay-ms=1000
 * notification.outbox.fast-poller.fresh-event-window-seconds=60
 * notification.outbox.slow-poller.delay-ms=30000
 * notification.outbox.slow-poller.stuck-event-threshold-seconds=60
 * 
 * @author Greedy's System
 * @since 2025-01-20
 */
@Data
@Component
@ConfigurationProperties(prefix = "notification.outbox")
public class NotificationOutboxPollerProperties {

    /**
     * Sezione multi-poller: controlla se abilitare FAST + SLOW oppure solo FAST
     */
    private MultiPoller multiPoller = new MultiPoller();

    /**
     * Sezione fast-poller: configurazione del poller veloce (sempre attivo)
     */
    private FastPoller fastPoller = new FastPoller();

    /**
     * Sezione slow-poller: configurazione del poller lento (opzionale)
     */
    private SlowPoller slowPoller = new SlowPoller();

    /**
     * 🔴 MULTI-POLLER CONFIGURATION
     * 
     * Controlla se il SLOW poller è attivo o meno.
     */
    @Data
    public static class MultiPoller {
        /**
         * true  = Abilita SLOW poller per pulire vecchi eventi stuck
         * false = Usa solo FAST poller (comportamento precedente)
         * 
         * Default: false (SOLO FAST POLLER) ✅
         */
        private boolean enabled = false;
    }

    /**
     * ⚡ FAST POLLER CONFIGURATION
     * 
     * Poller veloce che processa i nuovi eventi subito.
     * SEMPRE ATTIVO (indipendentemente da multiPoller.enabled).
     */
    @Data
    public static class FastPoller {
        /**
         * Intervallo di esecuzione del FAST poller in millisecondi.
         * Default: 1000 ms (1 secondo)
         * 
         * Intervalli comuni:
         * - 500ms   = Molto veloce (notifiche in ~500ms, CPU alta)
         * - 1000ms  = CONSIGLIATO (notifiche in ~1-2s, CPU balanced) ✅
         * - 2000ms  = Moderato (notifiche in ~2-3s, CPU bassa)
         * - 5000ms  = Lento (notifiche in ~5s, compatibile con vecchio poller)
         */
        private long delayMs = 1000;

        /**
         * Finestra temporale (in secondi) per considerare un evento "NUOVO".
         * Default: 60 secondi
         * 
         * Esempio:
         * - Se createdAt >= NOW - 60s → evento è NUOVO
         * - Se createdAt < NOW - 60s  → evento passa al SLOW poller
         * 
         * Range consigliato: 30-120 secondi
         */
        private long freshEventWindowSeconds = 60;
    }

    /**
     * 🐢 SLOW POLLER CONFIGURATION
     * 
     * Poller lento che pulisce i vecchi eventi stuck.
     * Attivo SOLO se multiPoller.enabled=true.
     */
    @Data
    public static class SlowPoller {
        /**
         * Intervallo di esecuzione del SLOW poller in millisecondi.
         * Default: 30000 ms (30 secondi)
         * 
         * Nota: Corre raramente, serve come safety net per retry.
         * Non ha impatto significativo su CPU.
         */
        private long delayMs = 30000;

        /**
         * Soglia (in secondi) oltre la quale un evento è considerato "STUCK/VECCHIO".
         * Default: 60 secondi
         * 
         * Deve essere UGUALE a fastPoller.freshEventWindowSeconds per logica corretta.
         * Se un evento rimane PENDING >60s, il SLOW poller lo riprova.
         */
        private long stuckEventThresholdSeconds = 60;
    }

    /**
     * ✅ METODO HELPER: Verifica se SLOW poller è abilitato
     */
    public boolean isSlowPollerEnabled() {
        return this.multiPoller.enabled;
    }

    /**
     * ✅ METODO HELPER: Ottiene il delay del FAST poller
     */
    public long getFastPollerDelayMs() {
        return this.fastPoller.delayMs;
    }

    /**
     * ✅ METODO HELPER: Ottiene il delay del SLOW poller
     */
    public long getSlowPollerDelayMs() {
        return this.slowPoller.delayMs;
    }
}
