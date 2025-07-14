package com.application.persistence.model.notification;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.MapKeyColumn;

@MappedSuperclass
public abstract class ANotification {
    
    @Column(name = "title", nullable = false)
    String title;

    @Column(name = "body", nullable = false)
    String body;

    @ElementCollection
    @CollectionTable(name = "notification_properties", joinColumns = @JoinColumn(name = "notification_id"))
    @MapKeyColumn(name = "property_key")
    @Column(name = "property_value")
    private Map<String, String> properties = new HashMap<>();

    private Boolean read = false;


    public ANotification(String title, String body, Map<String, String> data) {
        this.title = title;
        this.body = body;
        this.properties = data;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getData() {
        return properties;
    }

    public Boolean isRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }
}
