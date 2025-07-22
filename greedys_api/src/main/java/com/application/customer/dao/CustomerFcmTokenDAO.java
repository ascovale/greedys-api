package com.application.customer.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.customer.CustomerFcmToken;

@Repository
public interface CustomerFcmTokenDAO extends JpaRepository<CustomerFcmToken, Long> {

    CustomerFcmToken findByFcmToken(String oldToken);

    CustomerFcmToken findByFcmTokenAndCustomerId(String oldToken, Long customerId);

    List<CustomerFcmToken> findByCustomerId(Long customerId);

    boolean existsByDeviceId(String deviceId);

    boolean existsByDeviceIdAndCreatedAtBefore(String deviceId, LocalDateTime expiryDate);

    CustomerFcmToken findByDeviceId(String deviceId);

    CustomerFcmToken findByCustomerIdAndDeviceId(Long customerId, String deviceId);
}
