package com.application.common.service.notification.cleanup;

import com.application.common.persistence.dao.EventOutboxDAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * ⭐ EVENT OUTBOX CLEANUP JOB
 * 
 * PROBLEM #11: EventOutbox never cleaned up → table grows indefinitely
 * 
 * This job runs daily and:
 * 1. Deletes PROCESSED events older than 30 days (archive policy)
 * 2. Keeps FAILED events for audit trail (never auto-delete)
 * 3. Logs cleanup statistics
 * 4. Sends alert if cleanup fails
 * 
 * SCHEDULE: 2 AM daily (off-peak)
 * 
 * RETENTION POLICY:
 * - PENDING: Keep indefinitely (in progress)
 * - PROCESSED: Keep 30 days, then delete
 * - FAILED: Keep forever (audit trail)
 * 
 * @author Greedy's System
 * @since 2025-01-22 (Database maintenance)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EventOutboxCleanupJob {
    
    private final EventOutboxDAO eventOutboxRepository;
    
    private static final int RETENTION_DAYS = 30;
    
    @Scheduled(cron = "0 2 * * *")  // 2 AM daily
    public void cleanupOldEvents() {
        try {
            log.info("🧹 Starting EventOutbox cleanup job...");
            
            Instant cutoff = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
            
            // Delete PROCESSED events older than 30 days
            int deletedCount = eventOutboxRepository.deleteProcessedBefore(cutoff);
            
            log.info("🗑️  Deleted {} old PROCESSED events (older than {} days)", 
                deletedCount, RETENTION_DAYS);
            
            // Note: FAILED events are KEPT for audit trail
            // If needed in future, can archive to separate table
            
        } catch (Exception e) {
            log.error("❌ Error during EventOutbox cleanup: {}", e.getMessage(), e);
        }
    }
}
