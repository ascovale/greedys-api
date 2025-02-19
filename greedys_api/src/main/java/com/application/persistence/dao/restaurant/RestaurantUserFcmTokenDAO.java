package com.application.persistence.dao.restaurant;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.restaurant.user.RestaurantUserFcmToken;

@Repository
public interface RestaurantUserFcmTokenDAO extends JpaRepository<RestaurantUserFcmToken, Long> {

    RestaurantUserFcmToken findByFcmToken(String oldToken);

    RestaurantUserFcmToken findByFcmTokenAndUserId(String oldToken, Long userId);

    List<RestaurantUserFcmToken> findByUserId(Long id);

    boolean existsByDeviceId(String deviceId);

    boolean existsByDeviceIdAndCreatedAtBefore(String deviceId, LocalDateTime expiryDate);

    RestaurantUserFcmToken findByDeviceId(String deviceId);

}
