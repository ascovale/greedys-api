package com.application.common.persistence.model.notification.messaging.event;

import lombok.Getter;

/**
 * Evento interno Spring: nuovo record salvato in notification_outbox
 * Triggerato dopo commit transazione per pubblicazione immediata su RabbitMQ
 */
@Getter
public class OutboxCreatedEvent {
    
    private final Long outboxId;
    
    public OutboxCreatedEvent(Long outboxId) {
        this.outboxId = outboxId;
    }
    
    @Override
    public String toString() {
        return "OutboxCreatedEvent{outboxId=" + outboxId + "}";
    }
}
