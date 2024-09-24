package com.application.persistence.dao.Restaurant;

import org.springframework.data.jpa.repository.JpaRepository;

import com.application.persistence.model.restaurant.RestaurantNotification;

public interface RestaurantNotificationDAO extends JpaRepository<RestaurantNotification, Long>{
}