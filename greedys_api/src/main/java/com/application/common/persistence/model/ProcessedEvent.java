package com.application.common.persistence.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.application.common.type.ProcessingStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ⭐ IDEMPOTENCY: Event-Level Deduplication
 * 
 * Tracks which events have been processed by EventOutboxOrchestrator
 * to prevent duplicate message publishing to RabbitMQ.
 * 
 * 📋 USAGE:
 * EventOutboxOrchestrator.processEventOutbox():
 *   1. Try INSERT ProcessedEvent(eventId) with UNIQUE constraint
 *   2. If success → event never processed, continue
 *   3. If UNIQUE violation → event already processed, SKIP
 * 
 * 🔒 GUARANTEE: Same event never published twice to RabbitMQ
 * 
 * @author Greedy's System
 * @since 2025-01-21 (Idempotency Implementation)
 */
@Entity
@Table(name = "processed_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * ⭐ UNIQUE INDEX: Ensures same eventId cannot be processed twice
	 * 
	 * When EventOutboxOrchestrator tries to insert ProcessedEvent:
	 * - First insert: SUCCESS (new eventId)
	 * - Retry insert: UNIQUE CONSTRAINT VIOLATION (eventId already exists)
	 * 
	 * This prevents duplicate RabbitMQ messages.
	 */
	@Column(nullable = false, unique = true, length = 100)
	private String eventId;

	/**
	 * Current status of this event processing:
	 * - PROCESSING: Event is being processed right now
	 * - SUCCESS: Event successfully published to RabbitMQ
	 * - FAILED: Event processing failed (will retry)
	 * 
	 * Updated after successful RabbitMQ publish.
	 */
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private ProcessingStatus status;

	/**
	 * Timestamp when event was marked as processed.
	 * Auto-populated by @CreationTimestamp.
	 */
	@CreationTimestamp
	@Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime processedAt;
}
