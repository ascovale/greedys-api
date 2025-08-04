package com.application.persistence.dao.restaurant;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.fcm.RUserFcmToken;

@Repository
public interface RUserFcmTokenDAO extends JpaRepository<RUserFcmToken, Long> {

    RUserFcmToken findByFcmToken(String oldToken);

    @Query("SELECT r FROM RUserFcmToken r WHERE r.fcmToken = :fcmToken AND r.rUser.id = :rUserId")
    RUserFcmToken findByFcmTokenAndRUserId(@Param("fcmToken") String oldToken, @Param("rUserId") Long rUserId);

    @Query("SELECT r FROM RUserFcmToken r WHERE r.rUser.id = :id")
    List<RUserFcmToken> findByRUserId(@Param("id") Long id);

    boolean existsByDeviceId(String deviceId);

    boolean existsByDeviceIdAndCreatedAtBefore(String deviceId, LocalDateTime expiryDate);

    RUserFcmToken findByDeviceId(String deviceId);

    @Query("SELECT r FROM RUserFcmToken r WHERE r.rUser.id = :rUserId AND r.deviceId = :deviceId")
    RUserFcmToken findByRUserIdAndDeviceId(@Param("rUserId") Long rUserId, @Param("deviceId") String deviceId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM RUserFcmToken r WHERE r.rUser.id = :userId AND r.deviceId = :deviceId")
    boolean existsByRUserIdAndDeviceId(@Param("userId") Long userId, @Param("deviceId") String deviceId);
}
