package com.application.admin.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.admin.model.AdminFcmToken;

@Repository
public interface AdminFcmTokenDAO extends JpaRepository<AdminFcmToken, Long> {

    AdminFcmToken findByFcmToken(String oldToken);

    AdminFcmToken findByFcmTokenAndAdminId(String oldToken, Long adminId);

    List<AdminFcmToken> findByAdminId(Long adminId);

    boolean existsByDeviceId(String deviceId);

    boolean existsByDeviceIdAndCreatedAtBefore(String deviceId, LocalDateTime expiryDate);

    AdminFcmToken findByDeviceId(String deviceId);

    AdminFcmToken findByAdminIdAndDeviceId(Long adminId, String deviceId);
}
