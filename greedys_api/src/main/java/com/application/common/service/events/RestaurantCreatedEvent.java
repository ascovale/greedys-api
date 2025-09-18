package com.application.common.service.events;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

/**
 * Evento pubblicato quando viene creata una nuova prenotazione
 * Questo è un esempio più realistico - quando si crea una prenotazione:
 * - Il ristorante deve essere notificato
 * - Il cliente deve ricevere conferma  
 * - Le statistiche devono essere aggiornate
 */
@Getter
public class RestaurantCreatedEvent extends ApplicationEvent {
    
    private final Long reservationId;
    private final Long customerId;
    private final Long restaurantId;
    private final String customerEmail;
    private final String reservationDate;

    public RestaurantCreatedEvent(Object source, Long reservationId, Long customerId, 
                                 Long restaurantId, String customerEmail, String reservationDate) {
        super(source);
        this.reservationId = reservationId;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.customerEmail = customerEmail;
        this.reservationDate = reservationDate;
    }
}
