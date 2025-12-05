package com.application.common.exception;

/**
 * Eccezione per errori durante l'elaborazione delle immagini
 */
public class ImageProcessingException extends RuntimeException {
    
    public ImageProcessingException(String message) {
        super(message);
    }
    
    public ImageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
