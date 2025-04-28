package com.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.customer.CustomerFcmTokenDAO;
import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.customer.CustomerFcmToken;
import com.application.web.dto.post.UserFcmTokenDTO;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import jakarta.persistence.EntityManager;

@Service
@Transactional
public class CustomerFcmTokenService {
    private final CustomerFcmTokenDAO customerFcmTokenRepository;
    private final EntityManager entityManager;

    public CustomerFcmTokenService(CustomerFcmTokenDAO customerFcmTokenRepository, EntityManager entityManager) {
        this.customerFcmTokenRepository = customerFcmTokenRepository;
        this.entityManager = entityManager;
    }

    public void saveUserFcmToken(UserFcmTokenDTO userFcmTokenDTO) {
        CustomerFcmToken userFcmToken = new CustomerFcmToken();
        userFcmToken.setCustomer(entityManager.getReference(Customer.class, userFcmTokenDTO.getUserId()));
        userFcmToken.setFcmToken(userFcmTokenDTO.getFcmToken());
        userFcmToken.setCreatedAt(userFcmTokenDTO.getCreatedAt() != null ? userFcmTokenDTO.getCreatedAt() : LocalDateTime.now());
        userFcmToken.setDeviceId(userFcmTokenDTO.getDeviceId());
        customerFcmTokenRepository.save(userFcmToken);
    }

    public CustomerFcmToken updateUserFcmToken(String oldToken, UserFcmTokenDTO newToken) {
        CustomerFcmToken existingToken = customerFcmTokenRepository.findByFcmTokenAndCustomerId(oldToken, newToken.getUserId());
        if (existingToken != null) {
            existingToken.setFcmToken(newToken.getFcmToken());
            return customerFcmTokenRepository.save(existingToken);
        } else {
            throw new RuntimeException("UserFcmToken not found for user");
        }
    }

    public List<CustomerFcmToken> getTokensByCustomerId(Long id) {
        return customerFcmTokenRepository.findByCustomerId(id);
    }

    public CustomerFcmToken getTokenByDeviceId(String deviceId) {
        return customerFcmTokenRepository.findByDeviceId(deviceId);
    }

    public boolean isDeviceTokenPresent(String deviceId) {
        return customerFcmTokenRepository.existsByDeviceId(deviceId);
    }

    public String verifyTokenByDeviceId(String deviceId) {
        CustomerFcmToken userFcmToken = customerFcmTokenRepository.findByDeviceId(deviceId);
        if (userFcmToken == null) {
            return "NOT FOUND";
        }
        FirebaseToken decodedToken = verifyToken(userFcmToken.getFcmToken());
        if (decodedToken == null) {
            return "EXPIRED";
        }
        return "OK";
    }

    public FirebaseToken verifyToken(String idToken) {
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (Exception e) {
            // Handle token verification error
            return null;
        }
    }

    
}