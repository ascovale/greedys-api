package com.application.common.type;

/**
 * ⭐ Event Processing Status Enumeration
 * 
 * Used by ProcessedEvent to track event processing lifecycle:
 * 
 * PROCESSING → SUCCESS (normal flow)
 * PROCESSING → FAILED → PROCESSING (on retry)
 * 
 * @author Greedy's System
 * @since 2025-01-21
 */
public enum ProcessingStatus {

	/**
	 * Event is currently being processed.
	 * Status set right after INSERT ProcessedEvent(eventId).
	 */
	PROCESSING,

	/**
	 * Event successfully published to RabbitMQ.
	 * Status set after rabbitTemplate.convertAndSend() completes.
	 * Also means EventOutbox.status = PROCESSED.
	 */
	SUCCESS,

	/**
	 * Event processing failed.
	 * Will be retried by EventOutboxPoller on next poll.
	 */
	FAILED
}
