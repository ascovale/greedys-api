package com.application.common.persistence.model.notification.websocket;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;

/**
 * Rate limiter per connessioni WebSocket.
 * Protegge il sistema da abusi e attacchi DoS limitando:
 * - Numero di connessioni per utente
 * - Numero di tentativi di connessione falliti
 */
@Component
@Slf4j
public class WebSocketRateLimiter {

    // Cache: username → Bucket per connessioni
    private final Cache<String, Bucket> connectionBuckets;
    
    // Cache: username → Bucket per tentativi falliti
    private final Cache<String, Bucket> failedAttemptsBuckets;
    
    // Cache: IP address → Bucket per connessioni da IP
    private final Cache<String, Bucket> ipBuckets;

    public WebSocketRateLimiter() {
        // Cache con TTL 10 minuti (auto-cleanup)
        this.connectionBuckets = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();
        
        this.failedAttemptsBuckets = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();
        
        this.ipBuckets = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();
        
        log.info("✅ WebSocket Rate Limiter initialized");
    }

    /**
     * Verifica se l'utente può connettersi.
     * Limite: 10 connessioni ogni 1 minuto.
     * 
     * @param username Email dell'utente
     * @return true se connessione permessa, false altrimenti
     */
    public boolean allowConnection(String username) {
        Bucket bucket = connectionBuckets.get(username, k -> createConnectionBucket());
        
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            log.warn("⚠️ WebSocket rate limit exceeded for user: {}", username);
        } else {
            log.debug("✅ WebSocket connection allowed for user: {}", username);
        }
        
        return allowed;
    }

    /**
     * Verifica se l'IP può connettersi.
     * Limite: 50 connessioni ogni 1 minuto per IP.
     * Protegge da attacchi distribuiti da stesso IP.
     * 
     * @param ipAddress IP address
     * @return true se connessione permessa, false altrimenti
     */
    public boolean allowConnectionFromIp(String ipAddress) {
        Bucket bucket = ipBuckets.get(ipAddress, k -> createIpBucket());
        
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            log.warn("⚠️ WebSocket rate limit exceeded for IP: {}", ipAddress);
        }
        
        return allowed;
    }

    /**
     * Registra tentativo di connessione fallito.
     * Limite: 5 tentativi falliti ogni 5 minuti.
     * Blocca brute-force attacks.
     * 
     * @param username Email dell'utente o identificatore
     * @return true se ancora permesso provare, false se bloccato
     */
    public boolean allowFailedAttempt(String username) {
        Bucket bucket = failedAttemptsBuckets.get(username, k -> createFailedAttemptsBucket());
        
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            log.error("🚨 Too many failed WebSocket attempts for user: {} - BLOCKED", username);
        } else {
            log.warn("⚠️ Failed WebSocket attempt recorded for user: {}", username);
        }
        
        return allowed;
    }

    /**
     * Resetta il rate limit per un utente.
     * Utile per test o in caso di false positive.
     * 
     * @param username Email dell'utente
     */
    public void resetLimits(String username) {
        connectionBuckets.invalidate(username);
        failedAttemptsBuckets.invalidate(username);
        log.info("🔄 Rate limits reset for user: {}", username);
    }

    /**
     * Resetta tutti i rate limits.
     * Utile per amministrazione o emergenze.
     */
    public void resetAllLimits() {
        connectionBuckets.invalidateAll();
        failedAttemptsBuckets.invalidateAll();
        ipBuckets.invalidateAll();
        log.info("🔄 All rate limits reset");
    }

    /**
     * Ottieni statistiche rate limiting per debug.
     */
    public RateLimitStats getStats() {
        return RateLimitStats.builder()
            .connectionBucketsSize(connectionBuckets.estimatedSize())
            .failedAttemptsBucketsSize(failedAttemptsBuckets.estimatedSize())
            .ipBucketsSize(ipBuckets.estimatedSize())
            .build();
    }

    // ========== BUCKET FACTORIES ==========

    /**
     * Crea bucket per connessioni normali.
     * Limite: 10 token, refill 10 token ogni 1 minuto.
     */
    @SuppressWarnings("deprecation")
    private Bucket createConnectionBucket() {
        Bandwidth limit = Bandwidth.classic(
            10,  // capacity: 10 connessioni
            Refill.intervally(10, Duration.ofMinutes(1))  // refill: 10 ogni 1 minuto
        );
        
        return io.github.bucket4j.Bucket4j.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Crea bucket per connessioni da IP.
     * Limite: 50 token, refill 50 token ogni 1 minuto.
     * Più permissivo per supportare NAT/proxy condivisi.
     */
    @SuppressWarnings("deprecation")
    private Bucket createIpBucket() {
        Bandwidth limit = Bandwidth.classic(
            50,  // capacity: 50 connessioni
            Refill.intervally(50, Duration.ofMinutes(1))
        );
        
        return io.github.bucket4j.Bucket4j.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Crea bucket per tentativi falliti.
     * Limite: 5 token, refill 5 token ogni 5 minuti.
     * Più restrittivo per bloccare brute-force.
     */
    @SuppressWarnings("deprecation")
    private Bucket createFailedAttemptsBucket() {
        Bandwidth limit = Bandwidth.classic(
            5,  // capacity: 5 tentativi
            Refill.intervally(5, Duration.ofMinutes(5))  // refill: 5 ogni 5 minuti
        );
        
        return io.github.bucket4j.Bucket4j.builder()
            .addLimit(limit)
            .build();
    }

    // ========== STATS DTO ==========

    @lombok.Builder
    @lombok.Data
    public static class RateLimitStats {
        private long connectionBucketsSize;
        private long failedAttemptsBucketsSize;
        private long ipBucketsSize;
    }
}
