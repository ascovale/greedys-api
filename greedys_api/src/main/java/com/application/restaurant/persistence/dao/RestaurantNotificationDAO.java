package com.application.restaurant.persistence.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.application.restaurant.persistence.model.RestaurantNotification;

public interface RestaurantNotificationDAO extends JpaRepository<RestaurantNotification, Long>{

    Page<RestaurantNotification> findByReadFalse(Pageable pageable);
}