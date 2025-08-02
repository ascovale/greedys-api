package com.application.customer.service;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.web.dto.shared.FcmTokenDTO;
import com.application.customer.persistence.dao.CustomerFcmTokenDAO;
import com.application.customer.persistence.model.Customer;
import com.application.customer.persistence.model.CustomerFcmToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomerFcmTokenService {
    private final CustomerFcmTokenDAO customerFcmTokenRepository;

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
