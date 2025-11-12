package com.application.common.persistence.model.notification.messaging.publisher;

import com.application.common.persistence.model.notification.messaging.model.NotificationEvent;

public interface NotificationEventPublisher {
    void publish(NotificationEvent event);
}
