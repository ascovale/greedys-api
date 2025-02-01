package com.application.service.utils;

import java.util.HashMap;
import java.util.Map;

import com.application.persistence.model.user.Notification.Type;
import com.application.persistence.model.restaurant.RestaurantNotification;

public class NotificatioUtils { 
    public static final Map<Type, NotificationTemplate> USER_TEMPLATES = new HashMap<>() {{
        put(Type.NO_SHOW, new NotificationTemplate("no_show", "è stato segnato un no_show"));
        put(Type.SEATED, new NotificationTemplate("seated", "You have been seated successfully."));
        put(Type.UNSEATED, new NotificationTemplate("unseated", "You have been unseated."));
        put(Type.CANCEL, new NotificationTemplate("cancel", "Your reservation has been cancelled."));
        put(Type.CONFIRMED, new NotificationTemplate("confirmed", "Your reservation has been confirmed."));
        put(Type.ALTERED, new NotificationTemplate("altered", "Your reservation details have been altered."));
        put(Type.NEW_RESERVATION, new NotificationTemplate("new_reservation", "Your reservation has been created by the restaurant."));
    }};

    public static final Map<RestaurantNotification.Type, NotificationTemplate> RESTAURANT_TEMPLATES = new HashMap<>() {{
        put(RestaurantNotification.Type.NEW_RESERVATION, new NotificationTemplate("new_reservation", "A new reservation has been made."));
        put(RestaurantNotification.Type.REQUEST, new NotificationTemplate("request", "A new request has been made."));
        put(RestaurantNotification.Type.MODIFICATION, new NotificationTemplate("modification", "A reservation has been modified."));
        put(RestaurantNotification.Type.REVIEW, new NotificationTemplate("review", "A new review has been submitted."));
        put(RestaurantNotification.Type.REVIEW_ALTERED, new NotificationTemplate("review_modify", "A review has been modified."));
        put(RestaurantNotification.Type.SEATED, new NotificationTemplate("seated", "A reservation has been seated."));
        put(RestaurantNotification.Type.NO_SHOW, new NotificationTemplate("no_show", "A reservation has been marked as no_show."));
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

    public static Map<Type, NotificationTemplate> getUserTemplates() {
        return USER_TEMPLATES;
    }

    public static Map<RestaurantNotification.Type, NotificationTemplate> getRestaurantTemplates() {
        return RESTAURANT_TEMPLATES;
    }
    
}
