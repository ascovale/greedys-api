package com.application.restaurant.service;

import java.time.Instant;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.common.persistence.mapper.ReservationMapper;
import com.application.restaurant.web.dto.reservation.ReservationEventDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ RESERVATION WEBSOCKET PUBLISHER
 * 
 * Sends real-time WebSocket events to all connected restaurant staff when:
 * 1. A new reservation arrives (RESERVATION_CREATED)
 * 2. A reservation is accepted (RESERVATION_ACCEPTED)
 * 3. A reservation is rejected (RESERVATION_REJECTED)
 * 
 * Topic: /topic/restaurants/{restaurantId}/reservations
 * 
 * All staff connected to this topic will see the update in real-time.
 * 
 * @author Greedy's System
 * @since 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationWebSocketPublisher {
    
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ReservationMapper reservationMapper;
    
    /**
     * ⭐ Publish RESERVATION_CREATED event to all staff in a restaurant.
     * 
     * Called when a customer books a new reservation.
     * All staff of that restaurant will see it in the reservations list instantly.
     * 
     * Topic: /topic/restaurants/{restaurantId}/reservations
     * 
     * Message format:
     * {
     *   "type": "RESERVATION_CREATED",
     *   "reservation": { ...ReservationDTO... },
     *   "timestamp": "2025-01-15T14:30:00Z"
     * }
     * 
     * @param reservation The newly created reservation
     */
    public void publishReservationCreated(Reservation reservation) {
        try {
            Long restaurantId = reservation.getRestaurant().getId();
            ReservationDTO reservationDTO = reservationMapper.toDTO(reservation);
            
            ReservationEventDTO event = ReservationEventDTO.builder()
                    .type("RESERVATION_CREATED")
                    .reservation(reservationDTO)
                    .timestamp(Instant.now())
                    .build();
            
            String topic = "/topic/restaurants/" + restaurantId + "/reservations";
            simpMessagingTemplate.convertAndSend(topic, event);
            
            log.info("Published RESERVATION_CREATED event: restaurant={}, reservation={}", 
                    restaurantId, reservation.getId());
            
        } catch (Exception e) {
            log.error("Failed to publish RESERVATION_CREATED event", e);
            // Don't throw - WebSocket is best-effort
        }
    }
    
    /**
     * ⭐ Publish RESERVATION_ACCEPTED event to all staff in a restaurant.
     * 
     * Called when a restaurant staff accepts a reservation.
     * All connected staff will see the reservation move from PENDING to ACCEPTED.
     * 
     * Topic: /topic/restaurants/{restaurantId}/reservations
     * 
     * @param reservation The accepted reservation
     */
    public void publishReservationAccepted(Reservation reservation) {
        try {
            Long restaurantId = reservation.getRestaurant().getId();
            ReservationDTO reservationDTO = reservationMapper.toDTO(reservation);
            
            ReservationEventDTO event = ReservationEventDTO.builder()
                    .type("RESERVATION_ACCEPTED")
                    .reservation(reservationDTO)
                    .timestamp(Instant.now())
                    .build();
            
            String topic = "/topic/restaurants/" + restaurantId + "/reservations";
            simpMessagingTemplate.convertAndSend(topic, event);
            
            log.info("Published RESERVATION_ACCEPTED event: restaurant={}, reservation={}", 
                    restaurantId, reservation.getId());
            
        } catch (Exception e) {
            log.error("Failed to publish RESERVATION_ACCEPTED event", e);
            // Don't throw - WebSocket is best-effort
        }
    }
    
    /**
     * ⭐ Publish RESERVATION_REJECTED event to all staff in a restaurant.
     * 
     * Called when a restaurant staff rejects a reservation.
     * All connected staff will see the reservation move from PENDING to REJECTED.
     * 
     * Topic: /topic/restaurants/{restaurantId}/reservations
     * 
     * @param reservation The rejected reservation
     */
    public void publishReservationRejected(Reservation reservation) {
        try {
            Long restaurantId = reservation.getRestaurant().getId();
            ReservationDTO reservationDTO = reservationMapper.toDTO(reservation);
            
            ReservationEventDTO event = ReservationEventDTO.builder()
                    .type("RESERVATION_REJECTED")
                    .reservation(reservationDTO)
                    .timestamp(Instant.now())
                    .build();
            
            String topic = "/topic/restaurants/" + restaurantId + "/reservations";
            simpMessagingTemplate.convertAndSend(topic, event);
            
            log.info("Published RESERVATION_REJECTED event: restaurant={}, reservation={}", 
                    restaurantId, reservation.getId());
            
        } catch (Exception e) {
            log.error("Failed to publish RESERVATION_REJECTED event", e);
            // Don't throw - WebSocket is best-effort
        }
    }
}
