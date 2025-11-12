package com.application.common.persistence.model.notification.channel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import com.application.common.service.EmailService;
import com.application.common.service.notification.model.NotificationMessage;
import com.application.customer.persistence.dao.CustomerDAO;
import com.application.restaurant.persistence.dao.RUserDAO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(EmailService.class)
public class EmailNotificationChannel implements NotificationChannel {

    private final EmailService emailService;
    private final CustomerDAO customerDAO;
    private final RUserDAO rUserDAO;

    @Override
    public void send(NotificationMessage message) {
        // message contiene: recipientId (Long) + recipientType (String)
        String email = getRecipientEmail(message.getRecipientId(), message.getRecipientType());

        if (email == null || email.isBlank()) {
            log.warn("No email found for {} with id {}, skipping", 
                message.getRecipientType(), message.getRecipientId());
            return;
        }

        try {
            emailService.sendEmail(
                email,
                message.getTitle(),
                message.getBody()
            );
            log.info("Sent email notification to {} (recipientType={})", 
                email, message.getRecipientType());
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.EMAIL;
    }

    @Override
    public boolean isAvailable() {
        return emailService != null;
    }

    @Override
    public int getPriority() {
        return 3; // Medium-low priority
    }

    private String getRecipientEmail(Long recipientId, String recipientType) {
        if ("CUSTOMER".equals(recipientType)) {
            return customerDAO.findById(recipientId)
                    .map(c -> c.getEmail())
                    .orElse(null);
        } else if ("RESTAURANT_USER".equals(recipientType)) {
            return rUserDAO.findById(recipientId)
                    .map(r -> r.getEmail())
                    .orElse(null);
        }
        // TODO: Aggiungere ADMIN e AGENCY se hanno email nel sistema
        return null;
    }
}
