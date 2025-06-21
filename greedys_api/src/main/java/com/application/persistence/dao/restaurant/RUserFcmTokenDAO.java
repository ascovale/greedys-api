package com.application.persistence.dao.restaurant;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.notification.RUserFcmToken;

@Repository
public interface RUserFcmTokenDAO extends JpaRepository<RUserFcmToken, Long> {

    RUserFcmToken findByFcmToken(String oldToken);

    RUserFcmToken findByFcmTokenAndRUserId(String oldToken, Long RUserId);

    List<RUserFcmToken> findByRUserId(Long id);

    boolean existsByDeviceId(String deviceId);

    boolean existsByDeviceIdAndCreatedAtBefore(String deviceId, LocalDateTime expiryDate);

    RUserFcmToken findByDeviceId(String deviceId);

}
