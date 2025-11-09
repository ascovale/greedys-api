package com.application.restaurant.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.customer.persistence.model.Customer;
import com.application.restaurant.web.dto.customer.RestaurantCustomerContactDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class RestaurantCustomerContactService {

    private final CustomerDAO customerDAO;
    private final ReservationDAO reservationDAO;

    /**
     * Get all customer contacts for a restaurant
     * This includes both registered and unregistered customers who have made reservations
     */
    public List<RestaurantCustomerContactDTO> getRestaurantContacts(Long restaurantId) {
        log.debug("Getting all customer contacts for restaurant {}", restaurantId);
        
        // Get all customers who have made reservations at this restaurant
        List<Customer> customers = reservationDAO.findCustomersByRestaurantId(restaurantId);
        
        return customers.stream()
                .map(customer -> mapToContactDTO(customer, restaurantId))
                .collect(Collectors.toList());
    }

    /**
     * Get customer contacts with pagination
     */
    public Page<RestaurantCustomerContactDTO> getRestaurantContactsPageable(Long restaurantId, int page, int size, String sortBy) {
        log.debug("Getting paginated customer contacts for restaurant {}", restaurantId);
        
        Sort sort = Sort.by(Sort.Direction.ASC, sortBy != null ? sortBy : "name");
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Get customers with reservations at this restaurant
        Page<Customer> customers = reservationDAO.findCustomersByRestaurantIdPageable(restaurantId, pageable);
        
        return customers.map(customer -> mapToContactDTO(customer, restaurantId));
    }

    /**
     * Search customer contacts by name, email, or phone
     */
    public List<RestaurantCustomerContactDTO> searchRestaurantContacts(Long restaurantId, String searchTerm) {
        log.debug("Searching customer contacts for restaurant {} with term: {}", restaurantId, searchTerm);
        
        List<Customer> customers = reservationDAO.searchCustomersByRestaurantId(restaurantId, searchTerm);
        
        return customers.stream()
                .map(customer -> mapToContactDTO(customer, restaurantId))
                .collect(Collectors.toList());
    }

    /**
     * Get unregistered contacts only (customers who haven't created online accounts)
     */
    public List<RestaurantCustomerContactDTO> getUnregisteredContacts(Long restaurantId) {
        log.debug("Getting unregistered customer contacts for restaurant {}", restaurantId);
        
        List<Customer> unregisteredCustomers = reservationDAO.findUnregisteredCustomersByRestaurantId(restaurantId);
        
        return unregisteredCustomers.stream()
                .map(customer -> mapToContactDTO(customer, restaurantId))
                .collect(Collectors.toList());
    }

    /**
     * Get contact statistics for the restaurant
     */
    public ContactStatistics getContactStatistics(Long restaurantId) {
        long totalCustomers = reservationDAO.countCustomersByRestaurantId(restaurantId);
        long registeredCustomers = reservationDAO.countRegisteredCustomersByRestaurantId(restaurantId);
        long unregisteredCustomers = reservationDAO.countUnregisteredCustomersByRestaurantId(restaurantId);
        
        return ContactStatistics.builder()
                .totalContacts(totalCustomers)
                .registeredCustomers(registeredCustomers)
                .unregisteredCustomers(unregisteredCustomers)
                .build();
    }

    /**
     * Get detailed customer information
     */
    public RestaurantCustomerContactDTO getCustomerContactDetails(Long restaurantId, Long customerId) {
        log.debug("Getting customer contact details: restaurant={}, customer={}", restaurantId, customerId);
        
        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        
        // Verify customer has reservations at this restaurant
        boolean hasReservations = reservationDAO.existsByCustomerIdAndRestaurantId(customerId, restaurantId);
        if (!hasReservations) {
            throw new IllegalArgumentException("Customer has no reservations at this restaurant");
        }
        
        return mapToContactDTO(customer, restaurantId);
    }

    /**
     * Map Customer entity to RestaurantCustomerContactDTO
     */
    private RestaurantCustomerContactDTO mapToContactDTO(Customer customer, Long restaurantId) {
        // Get reservation statistics for this customer at this restaurant
        Long reservationCount = reservationDAO.countByCustomerIdAndRestaurantId(customer.getId(), restaurantId);
        java.time.LocalDate lastReservationDate = reservationDAO.findLastReservationDateByCustomerAndRestaurant(
                customer.getId(), restaurantId);
        
        // Map customer status to contact status
        RestaurantCustomerContactDTO.CustomerRegistrationStatus contactStatus = 
                mapCustomerStatusToContactStatus(customer.getStatus());
        
        return RestaurantCustomerContactDTO.builder()
                .id(customer.getId())
                .firstName(customer.getName())
                .lastName(customer.getSurname())
                .fullName(customer.getName() + " " + (customer.getSurname() != null ? customer.getSurname() : ""))
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .status(contactStatus)
                .reservationCount(reservationCount)
                .lastReservationDate(lastReservationDate)
                .dateOfBirth(customer.getDateOfBirth() != null ? 
                        customer.getDateOfBirth().toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate() : null)
                .build();
    }

    /**
     * Map Customer.Status to RestaurantCustomerContactDTO.CustomerRegistrationStatus
     */
    private RestaurantCustomerContactDTO.CustomerRegistrationStatus mapCustomerStatusToContactStatus(Customer.Status status) {
        switch (status) {
            case UNREGISTERED:
                return RestaurantCustomerContactDTO.CustomerRegistrationStatus.UNREGISTERED;
            case VERIFY_TOKEN:
                return RestaurantCustomerContactDTO.CustomerRegistrationStatus.REGISTERED;
            case ENABLED:
                return RestaurantCustomerContactDTO.CustomerRegistrationStatus.VERIFIED;
            case BLOCKED:
                return RestaurantCustomerContactDTO.CustomerRegistrationStatus.BLOCKED;
            default:
                return RestaurantCustomerContactDTO.CustomerRegistrationStatus.UNREGISTERED;
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class ContactStatistics {
        private long totalContacts;
        private long registeredCustomers;
        private long unregisteredCustomers;
        
        public double getRegistrationRate() {
            return totalContacts > 0 ? (double) registeredCustomers / totalContacts * 100 : 0;
        }
    }
}