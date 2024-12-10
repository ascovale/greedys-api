package com.application.persistence.dao.user;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.application.persistence.model.user.UserFcmToken;

@Repository
public interface UserFcmTokenDAO extends JpaRepository<UserFcmToken, Long> {

    UserFcmToken findByFcmToken(String oldToken);

    UserFcmToken findByFcmTokenAndUserId(String oldToken, Long userId);

    List<UserFcmToken> findByUserId(Long id);

    boolean existsByDeviceId(String deviceId);

    boolean existsByDeviceIdAndCreatedAtBefore(String deviceId, LocalDateTime expiryDate);

    UserFcmToken findByDeviceId(String deviceId);

}
