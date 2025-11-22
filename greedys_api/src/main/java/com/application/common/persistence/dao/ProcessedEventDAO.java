package com.application.common.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.ProcessedEvent;

/**
 * ⭐ IDEMPOTENCY: Event-Level Deduplication Repository
 * 
 * 📋 USAGE in EventOutboxOrchestrator:
 * 
 *   // Try insert (UNIQUE constraint on eventId)
 *   ProcessedEvent processed = new ProcessedEvent();
 *   processed.setEventId(event.getId());
 *   processed.setStatus(PROCESSING);
 *   processedEventRepository.save(processed);  // Can throw DataIntegrityViolationException
 *   
 *   if (exception instanceof DataIntegrityViolationException) {
 *       // Event already processed, SKIP publishing to RabbitMQ
 *       log.info("Event {} already processed, skipping", event.getId());
 *       return;
 *   }
 * 
 * 🔒 GUARANTEE: Same eventId never inserted twice
 *    → Same event never published to RabbitMQ twice
 * 
 * @author Greedy's System
 * @since 2025-01-21 (Idempotency Implementation)
 */
@Repository
public interface ProcessedEventDAO extends JpaRepository<ProcessedEvent, Long> {

	/**
	 * Check if event with given eventId was already processed.
	 * 
	 * @param eventId The unique event identifier
	 * @return true if event exists in ProcessedEvent table
	 */
	boolean existsByEventId(String eventId);
}
