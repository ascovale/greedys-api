package com.application.admin.service.authentication;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.security.jwt.JwtUtil;
import com.application.common.web.dto.get.CustomerDTO;
import com.application.common.web.dto.post.AuthResponseDTO;
import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.persistence.model.Customer;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdminCustomerAuthenticationService {

    private final CustomerDAO customerDAO;
    private final JwtUtil jwtUtil;

    /**
     * Admin login to customer - allows admin to authenticate as a customer
     * @param customerId The ID of the customer to authenticate as
     * @param request The HTTP servlet request
     * @return AuthResponseDTO containing JWT token and customer details
     */
    public AuthResponseDTO adminLoginToCustomer(Long customerId) {
        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("No customer found with ID: " + customerId));

        // Create an authentication token bypassing the password
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                customer.getEmail(), null);
        authToken.setDetails(customer);

        // Authenticate without password
        SecurityContextHolder.getContext().setAuthentication(authToken);

        final String jwt = jwtUtil.generateToken(customer);
        return new AuthResponseDTO(jwt, new CustomerDTO(customer));
    }
}
