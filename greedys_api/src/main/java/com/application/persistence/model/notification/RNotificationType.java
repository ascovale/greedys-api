package com.application.persistence.model.notification;

import com.application.persistence.model.restaurant.user.RUser;

public enum RNotificationType {
    RESERVATION_REQUEST
       {@Override
        public RestaurantNotification create(RUser user) {
            String title = "New Reservation Request";
            String body = "You have a new reservation request.";
            // Assuming no additional information in data
            return RestaurantNotification.builder()
                    .type(this)
                    .RUser(user)
                    .title(title)
                    .body(body)
                    .build();
        }}
    ,
    RESERVATION_ACCEPTED
       {@Override
        public RestaurantNotification create(RUser user) {
            String title = "Reservation Accepted";
            String body = "A reservation has been accepted.";
            // Assuming no additional information in data
            return RestaurantNotification.builder()
                                .type(this)
                                .RUser(user)
                                .title(title)
                                .body(body)
                                .build();        }},
    RESERVATION_REJECTED{
        @Override
        public RestaurantNotification create(RUser user) {
            String title = "Reservation Rejected";
            String body = "A reservation has been rejected.";
            // Assuming no additional information in data
            return RestaurantNotification.builder()
                                .type(this)
                                .RUser(user)
                                .title(title)
                                .body(body)
                                .build();        }
    },
    RESERVATION_NO_SHOW{
        @Override
        public RestaurantNotification create(RUser user) {
            String title = "Reservation No Show";
            String body = "A customer didn't show at the time of the reservation.";
            // Assuming no additional information in data
            return RestaurantNotification.builder()
                    .type(this)
                    .RUser(user)
                    .title(title)
                    .body(body)
                    .build();
        }
    },
    RESERVATION_SEATED{
        @Override
        public RestaurantNotification create(RUser user) {
            String title = "Reservation Seated";
            String body = "A customer has been seated.";
            // Assuming no additional information in data
            return RestaurantNotification.builder()
                    .type(this)
                    .RUser(user)
                    .title(title)
                    .body(body)
                    .build();
        }
    };

public abstract RestaurantNotification create(RUser user);
}
