package com.application.common.exception;

/**
 * Eccezione per file immagine non validi o non supportati
 */
public class InvalidImageException extends RuntimeException {
    
    public InvalidImageException(String message) {
        super(message);
    }
    
    public InvalidImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
