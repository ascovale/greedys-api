package com.application.service.rabbitmq.email;
public class ReservationEmail {
    private String email;
    private ReservationType reservationType;

    public ReservationEmail(String email, ReservationType reservationType) {
        this.email = email;
        this.reservationType = reservationType;
    }

    // Getter e Setter
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ReservationType getReservationType() {
        return reservationType;
    }

    public void setReservationType(ReservationType reservationType) {
        this.reservationType = reservationType;
    }

    public String getSubject(){
        switch (reservationType) {
            case NEW_RESERVATION:
                return "Nuova prenotazione";
            case CANCELLED_RESERVATION:
                return "Prenotazione cancellata";
            case REJECTED_RESERVATION:
                return "Prenotazione rifiutata";
            default:
                return "Tipo di prenotazione non riconosciuto";
        }
    }

    public String getText(){
        switch (reservationType) {
            case NEW_RESERVATION:
                return "Hai ricevuto una nuova prenotazione";
            case CANCELLED_RESERVATION:
                return "La prenotazione è stata cancellata";
            case REJECTED_RESERVATION:
                return "La prenotazione è stata rifiutata";
            default:
                return "Tipo di prenotazione non riconosciuto";
        }
    }
    
    public enum ReservationType {
        NEW_RESERVATION,
        CANCELLED_RESERVATION,
        REJECTED_RESERVATION
    }
}
