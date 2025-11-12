package com.application.common.persistence.model.notification.channel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import com.application.common.service.FirebaseService;
import com.application.common.service.notification.model.NotificationMessage;
import com.application.restaurant.service.RUserFcmTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(FirebaseService.class)
public class FirebaseNotificationChannel implements NotificationChannel {

    private final FirebaseService firebaseService;
    private final RUserFcmTokenService tokenService;

    @Override
    public void send(NotificationMessage message) {
        Long userId = message.getRecipientId();

        List<String> tokens = tokenService.getTokensByRUserId(userId);

        if (tokens == null || tokens.isEmpty()) {
            log.debug("No FCM tokens for user {}, skipping push notification", userId);
            return;
        }

        Map<String, String> data = buildDataPayload(message);

        try {
            firebaseService.sendNotification(
                message.getTitle(),
                message.getBody(),
                data,
                tokens
            );
            log.info("Sent Firebase push notification to {} tokens", tokens.size());
        } catch (Exception e) {
            log.error("Failed to send Firebase notification: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.FIREBASE_PUSH;
    }

    @Override
    public boolean isAvailable() {
        return firebaseService != null;
    }

    @Override
    public int getPriority() {
        return 2; // High priority
    }

    private Map<String, String> buildDataPayload(NotificationMessage message) {
        Map<String, String> data = new HashMap<>();
        if (message.getNotificationId() != null) {
            data.put("notificationId", message.getNotificationId().toString());
        }
        data.put("type", message.getType());
        if (message.getTimestamp() != null) {
            data.put("timestamp", message.getTimestamp().toString());
        }

        if (message.getMetadata() != null) {
            data.putAll(message.getMetadata());
        }

        return data;
    }
}
