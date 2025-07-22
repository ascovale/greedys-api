package com.application.restaurant.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.web.dto.get.CustomerStatisticsDTO;
import com.application.customer.service.CustomerService;

import lombok.RequiredArgsConstructor;

/**
 * Service dedicato alle operazioni restaurant sui customer.
 * Astrae e delega le chiamate al CustomerService.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RestaurantCustomerService {
    
    private final CustomerService customerService;

    // Metodi specifici per le operazioni restaurant sui customer
    public CustomerStatisticsDTO getCustomerStatistics(Long customerId) {
        return customerService.getCustomerStatistics(customerId);
    }
}
