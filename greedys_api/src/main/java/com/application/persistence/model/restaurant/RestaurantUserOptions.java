package com.application.persistence.model.restaurant;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.HashMap;
import java.util.Map;

import com.application.persistence.model.restaurant.RestaurantNotification.Type;


@Entity
@Table(name = "restaurant_user_options")
public class RestaurantUserOptions {
    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @jakarta.persistence.ElementCollection
    @jakarta.persistence.CollectionTable(name = "restaurant_user_notification_preferences", joinColumns = @jakarta.persistence.JoinColumn(name = "restaurant_user_options_id"))
    @jakarta.persistence.MapKeyColumn(name = "notification_type")
    @jakarta.persistence.Column(name = "enabled")
    private Map<Type, Boolean> notificationPreferences = new HashMap<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Map<Type, Boolean> getNotificationPreferences() {
        return notificationPreferences;
    }

    public void setNotificationPreferences(Map<Type, Boolean> notificationPreferences) {
        this.notificationPreferences = notificationPreferences;
    }

    public void addNotificationPreference(Type type, Boolean enabled) {
        this.notificationPreferences.put(type, enabled);
    }

    public void removeNotificationPreference(Type type) {
        this.notificationPreferences.remove(type);
    }
    

}
