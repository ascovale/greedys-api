package com.application.persistence.dao.restaurant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.application.persistence.model.restaurant.user.RestaurantNotification;

public interface RestaurantNotificationDAO extends JpaRepository<RestaurantNotification, Long>{

    Page<String> findAllByOpenedFalse(Pageable pageable);

    Page<RestaurantNotification> findByOpenedFalse(Pageable pageable);
}