package com.application.persistence.model.restaurant.user;

import java.util.HashMap;
import java.util.Map;

import com.application.persistence.model.restaurant.user.RestaurantNotification.Type;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;


@Entity
@Table(name = "restaurant_user_options")
public class RestaurantUserOptions {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    
    @ElementCollection
    @CollectionTable(name = "restaurant_user_notification_preferences", joinColumns = @jakarta.persistence.JoinColumn(name = "restaurant_user_options_id"))
    @MapKeyColumn(name = "notification_type")
    @Column(name = "enabled")
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
