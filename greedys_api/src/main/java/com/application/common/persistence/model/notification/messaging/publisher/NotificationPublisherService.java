package com.application.common.persistence.model.notification.messaging.publisher;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.dao.NotificationOutboxDAO;
import com.application.common.persistence.model.notification.NotificationOutbox;
import com.application.common.persistence.model.notification.messaging.event.OutboxCreatedEvent;
import com.application.common.persistence.model.notification.messaging.model.NotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisherService {

    private final NotificationOutboxDAO outboxRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void publish(NotificationEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            String aggregateType = determineAggregateType(event.getType());
            Long aggregateId = extractAggregateId(event);

            NotificationOutbox outbox = NotificationOutbox.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(event.getType().name())
                    .payload(payload)
                    .status(NotificationOutbox.Status.PENDING)
                    .retryCount(0)
                    .build();

            outbox = outboxRepository.save(outbox);
            log.debug("Saved notification to outbox: eventId={}, outboxId={}", event.getEventId(), outbox.getId());

            // 🔥 TRIGGER EVENTO: publishing immediato tramite Spring Events
            eventPublisher.publishEvent(new OutboxCreatedEvent(outbox.getId()));
            log.debug("Published OutboxCreatedEvent for outboxId={}", outbox.getId());

        } catch (Exception e) {
            log.error("Failed to save notification to outbox", e);
            throw new RuntimeException("Outbox save failed", e);
        }
    }

    private String determineAggregateType(NotificationEvent.NotificationType type) {
        if (type.name().startsWith("RESERVATION")) {
            return "RESERVATION";
        } else if (type.name().startsWith("CHAT")) {
            return "CHAT";
        } else {
            return "SYSTEM";
        }
    }

    private Long extractAggregateId(NotificationEvent event) {
        if (event.getMetadata() != null) {
            String reservationId = event.getMetadata().get("reservationId");
            if (reservationId != null) {
                return Long.parseLong(reservationId);
            }
            String chatMessageId = event.getMetadata().get("chatMessageId");
            if (chatMessageId != null) {
                return Long.parseLong(chatMessageId);
            }
        }
        return 0L;
    }
}
