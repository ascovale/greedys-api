package com.application.persistence.dao.customer;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.customer.CustomerFcmToken;

@Repository
public interface CustomerFcmTokenDAO extends JpaRepository<CustomerFcmToken, Long> {

    CustomerFcmToken findByFcmToken(String oldToken);

    CustomerFcmToken findByFcmTokenAndUserId(String oldToken, Long userId);

    List<CustomerFcmToken> findByUserId(Long id);

    boolean existsByDeviceId(String deviceId);

    boolean existsByDeviceIdAndCreatedAtBefore(String deviceId, LocalDateTime expiryDate);

    CustomerFcmToken findByDeviceId(String deviceId);

}
