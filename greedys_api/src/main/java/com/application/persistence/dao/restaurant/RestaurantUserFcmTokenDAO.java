package com.application.persistence.dao.restaurant;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.notification.RestaurantUserFcmToken;

@Repository
public interface RestaurantUserFcmTokenDAO extends JpaRepository<RestaurantUserFcmToken, Long> {

    RestaurantUserFcmToken findByFcmToken(String oldToken);

    RestaurantUserFcmToken findByFcmTokenAndRestaurantUserId(String oldToken, Long restaurantUserId);

    List<RestaurantUserFcmToken> findByRestaurantUserId(Long id);

    boolean existsByDeviceId(String deviceId);

    boolean existsByDeviceIdAndCreatedAtBefore(String deviceId, LocalDateTime expiryDate);

    RestaurantUserFcmToken findByDeviceId(String deviceId);

}
