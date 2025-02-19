package com.application.persistence.model.customer;

import java.util.Map;

import com.application.persistence.model.restaurant.user.RestaurantNotification.Type;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;


@Entity
@Table(name = "user_options")
public class CustomerOptions {
    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    
    @jakarta.persistence.ElementCollection
    @jakarta.persistence.CollectionTable(name = "user_email_notification_preferences", joinColumns = @jakarta.persistence.JoinColumn(name = "user_options_id"))
    @jakarta.persistence.MapKeyColumn(name = "notification_type")
    @jakarta.persistence.Column(name = "enabled")
    private Map<Type, Boolean> emailNotificationPreferences;
    
    @jakarta.persistence.ElementCollection
    @jakarta.persistence.CollectionTable(name = "user_notification_preferences", joinColumns = @jakarta.persistence.JoinColumn(name = "restaurant_user_options_id"))
    @jakarta.persistence.MapKeyColumn(name = "notification_type")
    @jakarta.persistence.Column(name = "enabled")
    private Map<Type, Boolean> notificationPreferences;

    public Map<Type, Boolean> getEmailNotificationPreferences() {
        return emailNotificationPreferences;
    }

    public void setEmailNotificationPreferences(Map<Type, Boolean> emailNotificationPreferences) {
        this.emailNotificationPreferences = emailNotificationPreferences;
    }

    public void addEmailNotificationPreference(Type type, Boolean enabled) {
        this.emailNotificationPreferences.put(type, enabled);
    }

    public void removeEmailNotificationPreference(Type type) {
        this.emailNotificationPreferences.remove(type);
    }

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
