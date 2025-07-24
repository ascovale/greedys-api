package com.application.common.service.events.listeners;

import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.application.common.service.ReliableNotificationService;
import com.application.common.service.events.ReservationCreatedEvent;
import com.application.restaurant.service.RestaurantNotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener that reacts when a reservation is created
 * This is the real advantage of events: decoupling!
 * 
 * ⚠️ IMPORTANT: Handles errors to ensure that email/notification failures 
 * don't compromise reservation creation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationEventListener {
    
    private final ReliableNotificationService reliableNotificationService;
    private final RestaurantNotificationService restaurantNotificationService;

    /**
     * When a reservation is created, sends confirmation email to customer
     * ⚠️ ERROR HANDLING: If email fails, logs it but doesn't block the system
     */
    @EventListener
    @Async // Executed asynchronously to not slow down reservation creation
    public void handleCustomerNotification(ReservationCreatedEvent event) {
        try {
            log.info("Sending confirmation email to customer {} for reservation {}", 
                     event.getCustomerEmail(), event.getReservationId());
            
            // Use reliable service with automatic retry
            reliableNotificationService.sendEmailWithRetry(
                event.getCustomerEmail(), 
                event.getReservationId()
            );
            
        } catch (Exception e) {
            // ⚠️ ERROR HANDLING: Even retry service failed
            log.error("❌ Failed to send confirmation email to customer {} for reservation {} after all retries: {}", 
                      event.getCustomerEmail(), event.getReservationId(), e.getMessage());
            
            // TODO: Save to failed_notifications table for manual intervention
        }
    }

    /**
     * When a reservation is created, notifies the restaurant
     * ⚠️ ERROR HANDLING: If notification fails, logs it but doesn't block the system
     */
    @EventListener
    @Async
    public void handleRestaurantNotification(ReservationCreatedEvent event) {
        try {
            log.info("Sending notification to restaurant {} for new reservation {}", 
                     event.getRestaurantId(), event.getReservationId());
            
            // Notify restaurant of new reservation
            restaurantNotificationService.sendNotificationToAllUsers(
                "New Reservation", 
                "A new reservation has been created for " + event.getReservationDate(),
                Map.of("reservationId", event.getReservationId().toString()),
                event.getRestaurantId()
            );
            
            log.info("✅ Restaurant notification sent successfully");
            
        } catch (Exception e) {
            // ⚠️ ERROR HANDLING: Notification failed, but reservation already saved  
            log.error("❌ Failed to send notification to restaurant {} for reservation {}: {}", 
                      event.getRestaurantId(), event.getReservationId(), e.getMessage());
            
            // TODO: Implement retry logic
        }
    }
}
