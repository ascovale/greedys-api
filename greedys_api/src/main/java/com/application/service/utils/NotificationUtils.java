package com.application.service.utils;

import java.util.HashMap;
import java.util.Map;

import com.application.persistence.model.restaurant.user.RestaurantNotification;

public class NotificationUtils {

    public enum CustomerNotificationType {
        NO_SHOW,
        SEATED,
        UNSEATED,
        CANCEL,
        CONFIRMED,
        ALTERED,
        NEW_RESERVATION,
        ACCEPTED,
        REJECTED
    }

    public static final NotificationTemplate customerTemplate(CustomerNotificationType type) {
        switch (type) {
            case NO_SHOW:
                return new NotificationTemplate("no_show", "A reservation has been marked as no_show.");
            case SEATED:
                return new NotificationTemplate("seated", "A reservation has been seated.");
            case UNSEATED:
                return new NotificationTemplate("unseated", "A reservation has been unseated.");
            case CANCEL:
                return new NotificationTemplate("cancel", "A reservation has been cancelled.");
            case CONFIRMED:
                return new NotificationTemplate("confirmed", "A reservation has been confirmed.");
            case ALTERED:
                return new NotificationTemplate("altered", "A reservation has been altered.");
            case NEW_RESERVATION:
                return new NotificationTemplate("new_reservation", "A new reservation has been made.");
            case ACCEPTED:
                return new NotificationTemplate("accepted", "A reservation has been accepted.");
            case REJECTED:
                return new NotificationTemplate("rejected", "A reservation has been rejected.");
            default:
                throw new IllegalArgumentException("Unknown notification type: " + type);
        }
    }

    public static final Map<RestaurantNotification.Type, NotificationTemplate> RESTAURANT_TEMPLATES = new HashMap<>() {{
        put(RestaurantNotification.Type.NEW_RESERVATION, new NotificationTemplate("new_reservation", "A new reservation has been made."));
        put(RestaurantNotification.Type.REQUEST, new NotificationTemplate("request", "A new request has been made."));
        put(RestaurantNotification.Type.MODIFICATION, new NotificationTemplate("modification", "A reservation has been modified."));
        put(RestaurantNotification.Type.REVIEW, new NotificationTemplate("review", "A new review has been submitted."));
        put(RestaurantNotification.Type.REVIEW_ALTERED, new NotificationTemplate("review_modify", "A review has been modified."));
        put(RestaurantNotification.Type.SEATED, new NotificationTemplate("seated", "A reservation has been seated."));
        put(RestaurantNotification.Type.NO_SHOW, new NotificationTemplate("no_show", "A reservation has been marked as no_show."));
        put(RestaurantNotification.Type.CANCEL, new NotificationTemplate("cancel", "A reservation has been cancelled."));
        put(RestaurantNotification.Type.ALTERED, new NotificationTemplate("altered", "A reservation has been altered."));    
        put(RestaurantNotification.Type.ACCEPTED, new NotificationTemplate("accepted", "A reservation has been accepted."));
        put(RestaurantNotification.Type.REJECTED, new NotificationTemplate("rejected", "A reservation has been rejected."));
        put(RestaurantNotification.Type.USERNOTACCEPTEDRESERVATION, new NotificationTemplate("user_not_accepted_reservation", "A user has not accepted a reservation."));
    }};

    public static class NotificationTemplate {
        private final String title;
        private final String message;

        public NotificationTemplate(String title, String message) {
            this.title = title;
            this.message = message;
        }

        public String getTitle() {
            return title;
        }

        public String getMessage() {
            return message;
        }
    }

    public static Map<RestaurantNotification.Type, NotificationTemplate> getRestaurantTemplates() {
        return RESTAURANT_TEMPLATES;
    }
    
}
