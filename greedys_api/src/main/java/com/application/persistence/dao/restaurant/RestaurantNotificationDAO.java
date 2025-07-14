package com.application.persistence.dao.restaurant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.application.persistence.model.notification.RestaurantNotification;

public interface RestaurantNotificationDAO extends JpaRepository<RestaurantNotification, Long>{

    Page<RestaurantNotification> findByReadFalse(Pageable pageable);
}