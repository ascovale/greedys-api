package com.application.common.persistence.model.notification.channel;

import com.application.common.service.notification.model.NotificationMessage;

public interface NotificationChannel {

    void send(NotificationMessage message);

    ChannelType getChannelType();

    boolean isAvailable();

    default int getPriority() {
        return 10; // Lower = higher priority
    }

    enum ChannelType {
        EMAIL,
        WEBSOCKET,
        FIREBASE_PUSH,
        SMS
    }
}
