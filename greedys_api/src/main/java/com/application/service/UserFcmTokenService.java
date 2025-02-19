package com.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.application.persistence.dao.customer.CustomerFcmTokenDAO;
import com.application.persistence.model.user.Customer;
import com.application.persistence.model.user.CustomerFcmToken;
import com.application.web.dto.post.UserFcmTokenDTO;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import jakarta.persistence.EntityManager;

@Service
public class UserFcmTokenService {
    private final CustomerFcmTokenDAO userFcmTokenRepository;
    private final EntityManager entityManager;

    public UserFcmTokenService(CustomerFcmTokenDAO userFcmTokenRepository, EntityManager entityManager) {
        this.userFcmTokenRepository = userFcmTokenRepository;
        this.entityManager = entityManager;
    }

    public void saveUserFcmToken(UserFcmTokenDTO userFcmTokenDTO) {
        CustomerFcmToken userFcmToken = new CustomerFcmToken();
        userFcmToken.setUser(entityManager.getReference(Customer.class, userFcmTokenDTO.getUserId()));
        userFcmToken.setFcmToken(userFcmTokenDTO.getFcmToken());
        userFcmToken.setCreatedAt(userFcmTokenDTO.getCreatedAt() != null ? userFcmTokenDTO.getCreatedAt() : LocalDateTime.now());
        userFcmToken.setDeviceId(userFcmTokenDTO.getDeviceId());
        userFcmTokenRepository.save(userFcmToken);
    }

    public CustomerFcmToken updateUserFcmToken(String oldToken, UserFcmTokenDTO newToken) {
        CustomerFcmToken existingToken = userFcmTokenRepository.findByFcmTokenAndUserId(oldToken, newToken.getUserId());
        if (existingToken != null) {
            existingToken.setFcmToken(newToken.getFcmToken());
            return userFcmTokenRepository.save(existingToken);
        } else {
            throw new RuntimeException("UserFcmToken not found for user");
        }
    }

    public List<CustomerFcmToken> getTokensByUserId(Long id) {
        return userFcmTokenRepository.findByUserId(id);
    }

    public CustomerFcmToken getTokenByDeviceId(String deviceId) {
        return userFcmTokenRepository.findByDeviceId(deviceId);
    }

    public boolean isDeviceTokenPresent(String deviceId) {
        return userFcmTokenRepository.existsByDeviceId(deviceId);
    }

    public String verifyTokenByDeviceId(String deviceId) {
        CustomerFcmToken userFcmToken = userFcmTokenRepository.findByDeviceId(deviceId);
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