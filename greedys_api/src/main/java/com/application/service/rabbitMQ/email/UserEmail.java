package com.application.service.rabbitMQ.email;

public class UserEmail {
    private String email;
    private EmailType emailType;

    public UserEmail(String email, EmailType emailType) {
        this.email = email;
        this.emailType = emailType;
    }

    // Getter e Setter
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public EmailType getEmailType() {
        return emailType;
    }

    public void setEmailType(EmailType emailType) {
        this.emailType = emailType;
    }

    public String getText(){
        switch (emailType) {
            case CONFIRMATION:
                return "Conferma email";
            case CANCEL_USER:
                return "Cancellazione utente";
            case ALTER_USER:
                return "Modifica utente";
            default:
                return "Tipo di email non riconosciuto";
        }
    }

    public String getSubject(){
        switch (emailType) {
            case CONFIRMATION:
                return "Conferma email";
            case CANCEL_USER:
                return "Cancellazione utente";
            case ALTER_USER:
                return "Modifica utente";
            default:
                return "Tipo di email non riconosciuto";
        }
    }

    public enum EmailType {
        CONFIRMATION,
        CANCEL_USER,
        ALTER_USER
    }
}
