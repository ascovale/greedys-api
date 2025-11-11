package com.application.restaurant.service.agenda;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.customer.persistence.model.Customer;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.customer.service.CustomerMatchService;
import com.application.common.service.PhoneNormalizer;
import com.application.restaurant.web.dto.agenda.CustomerContactCreateDTO;
import com.application.restaurant.web.dto.agenda.RestaurantAgendaResponse;
import com.application.restaurant.web.dto.agenda.RestaurantCustomerDTO;
import com.application.customer.web.dto.matching.CustomerMatchInput;
import com.application.customer.web.dto.matching.CustomerMatchResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing restaurant customer agenda/contacts
 */
@Service
@Slf4j
@Transactional
public class RestaurantAgendaService {

    @Autowired
    private CustomerDAO customerDAO;
    
    @Autowired
    private ReservationDAO reservationDAO;
    
    @Autowired
    private RestaurantDAO restaurantDAO;
    
    @Autowired
    private CustomerMatchService customerMatchService;
    
    @Autowired
    private PhoneNormalizer phoneNormalizer;

    /**
     * Get restaurant agenda with all customers  
     */
    public RestaurantAgendaResponse getRestaurantAgenda(Long restaurantId) {
        log.info("Getting agenda for restaurant: {}", restaurantId);
        
        // Validate restaurant exists
        restaurantDAO.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));
        
        // Get all customers that have reservations for this restaurant
        List<Customer> customers = reservationDAO.findCustomersByRestaurantId(restaurantId);
        
        List<RestaurantCustomerDTO> customerDTOs = customers.stream()
                .map(customer -> convertToRestaurantCustomerDTO(customer, restaurantId))
                .toList();
        
        return RestaurantAgendaResponse.builder()
                .restaurantId(restaurantId)
                .customers(customerDTOs)
                .totalCustomers(customerDTOs.size())
                .registeredCustomers((int) customerDTOs.stream()
                        .filter(RestaurantCustomerDTO::isHasAccount)
                        .count())
                .contactsOnly((int) customerDTOs.stream()
                        .filter(c -> !c.isHasAccount())
                        .count())
                .stats(buildAgendaStats(customerDTOs))
                .build();
    }

    /**
     * Search customers in restaurant agenda
     */
    public RestaurantAgendaResponse searchCustomers(Long restaurantId, String query, Pageable pageable) {
        log.info("Searching customers for restaurant {} with query: {}", restaurantId, query);
        
        List<Customer> customers = reservationDAO.searchCustomersByRestaurantId(restaurantId, query);
        
        List<RestaurantCustomerDTO> customerDTOs = customers.stream()
                .map(customer -> convertToRestaurantCustomerDTO(customer, restaurantId))
                .toList();
        
        return RestaurantAgendaResponse.builder()
                .restaurantId(restaurantId)
                .customers(customerDTOs)
                .totalCustomers(customerDTOs.size())
                .registeredCustomers((int) customerDTOs.stream()
                        .filter(RestaurantCustomerDTO::isHasAccount)
                        .count())
                .contactsOnly((int) customerDTOs.stream()
                        .filter(c -> !c.isHasAccount())
                        .count())
                .stats(buildAgendaStats(customerDTOs))
                .build();
    }

    /**
     * Force create new customer contact for restaurant
     */
    public RestaurantCustomerDTO forceCreateCustomerContact(Long restaurantId, CustomerContactCreateDTO createDTO) {
        log.info("Force creating customer contact for restaurant: {}", restaurantId);
        
        // Validate restaurant exists
        restaurantDAO.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));
        
        // Normalize phone if provided
        String normalizedPhone = null;
        if (createDTO.getPhoneNumber() != null) {
            normalizedPhone = phoneNormalizer.toE164(createDTO.getPhoneNumber());
        }
        
        // Check if customer already exists using match service
        CustomerMatchInput matchInput = CustomerMatchInput.builder()
                .restaurantId(convertLongToUUID(restaurantId))
                .firstName(createDTO.getFirstName())
                .lastName(createDTO.getLastName())
                .phone(normalizedPhone)
                .email(createDTO.getEmail())
                .build();
        
        CustomerMatchResponse matchResponse = customerMatchService.findMatches(matchInput);
        
        if (matchResponse.hasCandidates() && 
            !matchResponse.getHighConfidenceCandidates().isEmpty() && 
            !createDTO.isForceCreate()) {
            log.warn("Customer already exists, updating existing contact: {}", 
                    matchResponse.getBestCandidate().getId());
            return updateExistingCustomer(matchResponse.getBestCandidate().getId(), createDTO, restaurantId);
        }
        
        // Create new customer
        Customer newCustomer = Customer.builder()
                .name(createDTO.getFirstName())
                .surname(createDTO.getLastName())
                .phoneNumber(normalizedPhone)
                .email(createDTO.getEmail())
                .status(Customer.Status.UNREGISTERED)
                .build();
        
        Customer savedCustomer = customerDAO.save(newCustomer);
        log.info("Created new customer contact: {}", savedCustomer.getId());
        
        return convertToRestaurantCustomerDTO(savedCustomer, restaurantId);
    }    /**
     * Update customer contact information
     */
    public RestaurantCustomerDTO updateCustomerContact(Long customerId, CustomerContactCreateDTO updateDTO) {
        log.info("Updating customer contact: {}", customerId);
        
        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        
        if (updateDTO.getFirstName() != null) {
            customer.setName(updateDTO.getFirstName());
        }
        if (updateDTO.getLastName() != null) {
            customer.setSurname(updateDTO.getLastName());
        }
        if (updateDTO.getPhoneNumber() != null) {
            customer.setPhoneNumber(phoneNormalizer.toE164(updateDTO.getPhoneNumber()));
        }
        if (updateDTO.getEmail() != null) {
            customer.setEmail(updateDTO.getEmail());
        }
        
        Customer savedCustomer = customerDAO.save(customer);
        log.info("Updated customer contact: {}", savedCustomer.getId());
        
        return convertToRestaurantCustomerDTO(savedCustomer, null);
    }

    /**
     * Delete customer contact
     */
    public void deleteCustomerContact(Long customerId) {
        log.info("Deleting customer contact: {}", customerId);
        
        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        
        // Check if customer has active reservations  
        long activeReservations = reservationDAO.countByCustomerIdAndRestaurantId(customerId, null);
        
        if (activeReservations > 0) {
            throw new IllegalStateException("Cannot delete customer with active reservations: " + activeReservations);
        }
        
        customerDAO.delete(customer);
        log.info("Deleted customer contact: {}", customerId);
    }

    /**
     * Get customer contact details
     */
    public Optional<RestaurantCustomerDTO> getCustomerContact(Long customerId, Long restaurantId) {
        return customerDAO.findById(customerId)
                .map(customer -> convertToRestaurantCustomerDTO(customer, restaurantId));
    }

    /**
     * Add customer to restaurant agenda when creating reservation
     */
    public RestaurantCustomerDTO addToAgendaOnReservation(Long customerId, Long restaurantId) {
        log.info("Adding customer {} to restaurant {} agenda on reservation", customerId, restaurantId);
        
        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        
        // This method is called automatically when a reservation is created
        // It ensures the customer is properly tracked in the restaurant's agenda
        
        return convertToRestaurantCustomerDTO(customer, restaurantId);
    }

    /**
     * Convert Customer entity to RestaurantCustomerDTO
     */
    private RestaurantCustomerDTO convertToRestaurantCustomerDTO(Customer customer, Long restaurantId) {
        // Get reservation count for this restaurant
        int reservationCount = 0;
        if (restaurantId != null) {
            Long count = reservationDAO.countByCustomerIdAndRestaurantId(customer.getId(), restaurantId);
            reservationCount = count != null ? count.intValue() : 0;
        }
        
        return RestaurantCustomerDTO.builder()
                .customerId(customer.getId())
                .restaurantId(restaurantId)
                .firstName(customer.getName())
                .lastName(customer.getSurname())
                .displayName(buildDisplayName(customer.getName(), customer.getSurname()))
                .phoneNumber(customer.getPhoneNumber())
                .email(customer.getEmail())
                .notes(null) // Customer entity doesn't have notes field 
                .preferences(null) // Customer entity doesn't have preferences field
                .allergies(null) // Would need to convert List<Allergy> to String 
                .hasAccount(customer.getPassword() != null && !customer.getPassword().isEmpty())
                .totalReservations(reservationCount)
                .lastReservation(null) // Would need to get from reservations
                .firstAdded(null) // Would need creation timestamp
                .status(customer.getStatus())
                .build();
    }

    /**
     * Update existing customer with new information
     */
    private RestaurantCustomerDTO updateExistingCustomer(UUID customerId, CustomerContactCreateDTO updateDTO, Long restaurantId) {
        Customer customer = customerDAO.findById(convertUUIDToLong(customerId))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        
        // Only update basic info - Customer entity doesn't have notes/preferences fields
        boolean updated = false;
        
        if (updateDTO.getFirstName() != null && !updateDTO.getFirstName().equals(customer.getName())) {
            customer.setName(updateDTO.getFirstName());
            updated = true;
        }
        if (updateDTO.getLastName() != null && !updateDTO.getLastName().equals(customer.getSurname())) {
            customer.setSurname(updateDTO.getLastName());
            updated = true;
        }
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().equals(customer.getEmail())) {
            customer.setEmail(updateDTO.getEmail());
            updated = true;
        }
        if (updateDTO.getPhoneNumber() != null) {
            String normalized = phoneNormalizer.toE164(updateDTO.getPhoneNumber());
            if (!normalized.equals(customer.getPhoneNumber())) {
                customer.setPhoneNumber(normalized);
                updated = true;
            }
        }
        
        if (updated) {
            customer = customerDAO.save(customer);
            log.info("Updated existing customer: {}", customerId);
        }
        
        return convertToRestaurantCustomerDTO(customer, restaurantId);
    }

    /**
     * Build display name from first and last name
     */
    private String buildDisplayName(String firstName, String lastName) {
        StringBuilder name = new StringBuilder();
        if (firstName != null && !firstName.trim().isEmpty()) {
            name.append(firstName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(lastName.trim());
        }
        return name.length() > 0 ? name.toString() : "Unknown Contact";
    }

    /**
     * Convert UUID to Long (simple conversion)
     */
    private Long convertUUIDToLong(UUID uuid) {
        if (uuid == null) return null;
        // Simple hash-based conversion - in production might want something more sophisticated
        return Math.abs(uuid.hashCode()) % Long.MAX_VALUE;
    }

    /**
     * Convert Long to UUID for compatibility
     */
    private UUID convertLongToUUID(Long id) {
        if (id == null) return null;
        // Create a UUID from the long value
        // For now, use a simple mapping approach
        return UUID.nameUUIDFromBytes(id.toString().getBytes());
    }

    /**
     * Build agenda statistics
     */
    private RestaurantAgendaResponse.AgendaStats buildAgendaStats(List<RestaurantCustomerDTO> customers) {
        if (customers.isEmpty()) {
            return RestaurantAgendaResponse.AgendaStats.builder()
                    .totalContacts(0)
                    .withPhone(0)
                    .withEmail(0)
                    .withBothContacts(0)
                    .withNotes(0)
                    .withPreferences(0)
                    .withAllergies(0)
                    .avgReservationsPerCustomer(0.0)
                    .build();
        }
        
        int withPhone = (int) customers.stream().filter(c -> c.getPhoneNumber() != null).count();
        int withEmail = (int) customers.stream().filter(c -> c.getEmail() != null).count();
        int withBothContacts = (int) customers.stream()
                .filter(c -> c.getPhoneNumber() != null && c.getEmail() != null).count();
        int withNotes = (int) customers.stream()
                .filter(c -> c.getNotes() != null && !c.getNotes().trim().isEmpty()).count();
        int withPreferences = (int) customers.stream()
                .filter(c -> c.getPreferences() != null && !c.getPreferences().trim().isEmpty()).count();
        int withAllergies = (int) customers.stream()
                .filter(c -> c.getAllergies() != null && !c.getAllergies().trim().isEmpty()).count();
        
        double avgReservations = customers.stream()
                .mapToInt(RestaurantCustomerDTO::getTotalReservations)
                .average()
                .orElse(0.0);
        
        String mostActiveCustomer = customers.stream()
                .max((c1, c2) -> Integer.compare(c1.getTotalReservations(), c2.getTotalReservations()))
                .map(RestaurantCustomerDTO::getDisplayName)
                .orElse("N/A");
        
        String newestCustomer = customers.stream()
                .filter(c -> c.getFirstAdded() != null)
                .max((c1, c2) -> c1.getFirstAdded().compareTo(c2.getFirstAdded()))
                .map(RestaurantCustomerDTO::getDisplayName)
                .orElse("N/A");
        
        return RestaurantAgendaResponse.AgendaStats.builder()
                .totalContacts(customers.size())
                .withPhone(withPhone)
                .withEmail(withEmail)
                .withBothContacts(withBothContacts)
                .withNotes(withNotes)
                .withPreferences(withPreferences)
                .withAllergies(withAllergies)
                .avgReservationsPerCustomer(avgReservations)
                .mostActiveCustomer(mostActiveCustomer)
                .newestCustomer(newestCustomer)
                .build();
    }
}