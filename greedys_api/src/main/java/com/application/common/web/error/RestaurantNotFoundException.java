package com.application.common.web.error;

public final class RestaurantNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RestaurantNotFoundException() {
        super();
    }

    public RestaurantNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public RestaurantNotFoundException(final String message) {
        super(message);
    }

    public RestaurantNotFoundException(final Throwable cause) {
        super(cause);
    }
}
