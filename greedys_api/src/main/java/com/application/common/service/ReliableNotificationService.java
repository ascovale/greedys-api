package com.application.common.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service with retry capabilities for reliable operations
 */
@Service
@ConditionalOnProperty(name = "notifications.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class ReliableNotificationService {

    /**
     * Sends email with manual retry on failure
     * Will retry 3 times with 1 second delay between attempts
     */
    public void sendEmailWithRetry(String email, Long reservationId) {
        int maxAttempts = 3;
        int attempt = 1;
        
        while (attempt <= maxAttempts) {
            try {
                log.info("Attempting to send email to {} for reservation {} (attempt {}/{})", 
                        email, reservationId, attempt, maxAttempts);
                
                // Call actual email service here
                sendEmail(email, reservationId);
                
                log.info("✅ Email sent successfully on attempt {}", attempt);
                return; // Success, exit the retry loop
                
            } catch (Exception e) {
                log.warn("❌ Failed to send email on attempt {}/{}: {}", 
                        attempt, maxAttempts, e.getMessage());
                
                if (attempt == maxAttempts) {
                    // Final attempt failed, log and give up
                    log.error("🚨 All {} attempts failed to send email to {} for reservation {}", 
                            maxAttempts, email, reservationId);
                    // TODO: Save to failed_notifications table for later retry
                    return;
                }
                
                // Wait before next attempt (exponential backoff: 1s, 2s, 4s)
                try {
                    long delay = 1000L * (long) Math.pow(2, attempt - 1);
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Retry interrupted");
                    return;
                }
                
                attempt++;
            }
        }
    }
    
    /**
     * Actual email sending logic - replace with your EmailService call
     */
    private void sendEmail(String email, Long reservationId) {
        // Simulate email sending with potential failure
        if (Math.random() < 0.3) { // 30% failure rate for demo
            throw new RuntimeException("Email service temporarily unavailable");
        }
        
        // TODO: Replace with actual email service call
        // emailService.sendReservationConfirmation(email, reservationId);
    }
}
