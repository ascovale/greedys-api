package com.application.service;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.customer.CustomerFcmTokenDAO;
import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.fcm.CustomerFcmToken;
import com.application.web.dto.post.FcmTokenDTO;
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

    public void saveUserFcmToken(FcmTokenDTO userFcmTokenDTO) {
        Customer customer = (Customer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String token = userFcmTokenDTO.getFcmToken();
        String deviceId = userFcmTokenDTO.getDeviceId();

        CustomerFcmToken userFcmToken = new CustomerFcmToken(customer, token, deviceId);
        customerFcmTokenRepository.save(userFcmToken);
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

    public CustomerFcmToken findByUserIdAndDeviceId(Long userId, String deviceId) {
        return customerFcmTokenRepository.findByCustomerIdAndDeviceId(userId, deviceId);
    }
}