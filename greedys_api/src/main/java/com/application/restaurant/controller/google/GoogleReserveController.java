package com.application.restaurant.controller.google;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.reservation.ReservationRequest;
import com.application.common.web.ResponseWrapper;
import com.application.restaurant.service.google.after.GoogleReserveService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/restaurant/google_reserve/reservation")
@CrossOrigin(origins = "*")
@Tag(name = "Restaurant Reservations", description = "API per gestire prenotazioni ristoranti tramite Google Reserve API. " +
     "Permette di creare, modificare e cancellare prenotazioni.")
public class GoogleReserveController extends BaseController {

    private final GoogleReserveService googleReserveService;

    /**
     * Crea una nuova prenotazione
     */
    @Operation(
        summary = "Crea prenotazione", 
        description = "Crea una nuova prenotazione del ristorante tramite Google Reserve API. " +
                     "Restituisce i dettagli della prenotazione e l'ID di conferma."
    )
    @PostMapping("/book")
    public ResponseEntity<ResponseWrapper<Reservation>> createReservation(
            @RequestBody ReservationRequest reservationRequest) {
        
        return execute("create reservation", "Prenotazione creata con successo", 
            new OperationSupplier<Reservation>() {
                @Override
                public Reservation get() {
                    return googleReserveService.createReservation(reservationRequest);
                }
            });
    }

    /**
     * Modifica una prenotazione esistente
     */
    @Operation(
        summary = "Modifica prenotazione", 
        description = "Modifica una prenotazione esistente con nuovi dettagli. " +
                     "Può cambiare data, ora, numero di persone e richieste speciali."
    )
    @PutMapping("/{reservationId}")
    public ResponseEntity<ResponseWrapper<Reservation>> modifyReservation(
            @PathVariable String reservationId,
            @RequestBody ReservationRequest newDetails) {
        
        return execute("modify reservation", "Prenotazione modificata con successo", 
            new OperationSupplier<Reservation>() {
                @Override
                public Reservation get() {
                    return googleReserveService.modifyReservation(reservationId, newDetails);
                }
            });
    }

    /**
     * Cancella una prenotazione
     */
    @Operation(
        summary = "Cancella prenotazione", 
        description = "Cancella una prenotazione esistente. Richiede l'ID della prenotazione e un motivo opzionale."
    )
    @DeleteMapping("/{reservationId}")
    public ResponseEntity<ResponseWrapper<Boolean>> cancelReservation(
            @PathVariable String reservationId,
            @RequestParam(required = false) String reason) {
        
        return execute("cancel reservation", "Prenotazione cancellata con successo", 
            new OperationSupplier<Boolean>() {
                @Override
                public Boolean get() {
                    return googleReserveService.cancelReservation(reservationId, reason != null ? reason : "Cancellazione del cliente");
                }
            });
    }
}
