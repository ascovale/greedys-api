package com.application.persistence.model.notification;

import java.time.Instant;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;

import com.application.persistence.model.restaurant.user.RUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_restaurant")
public class RestaurantNotification extends ANotification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RUser_id")
    private RUser RUser;

    @Column(name = "creation_time", updatable = false)
    @CreationTimestamp
    private Instant creationTime;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private RNotificationType type;


    public RestaurantNotification(RNotificationType type, RUser RUser, String title, String body, Map<String, String> data) {
        super(title, body, data);
		this.type = type;
        this.RUser = RUser;
    }

    public Long getId() {
        return id;
    }

    public RUser getRUser() {
        return RUser;
    }

    public Instant getCreationTime() {
        return creationTime;
    }
}
