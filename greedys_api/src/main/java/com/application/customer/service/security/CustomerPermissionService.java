package com.application.customer.service.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.customer.persistence.model.Customer;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomerPermissionService {

    private final ReservationDAO reservationRepository;

    public boolean hasPermissionOnReservation(Long idReservation) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        Long customerId = null; 
        Customer customer =null;
        if (principal instanceof Customer) {
            customer = (Customer) principal;
            customerId = customer.getId();
        } else {
            return false;
        }
        // Check if the reservation exists and if the customer is the owner of the reservation
        // and if the customer is enabled
        Optional<Reservation> reservation = reservationRepository.findById(idReservation);
        return reservation.isPresent() && reservation.get().getCreatedBy().getId().equals(customerId) && customer.isEnabled();
    }

    public boolean hasAllergy(Long idAllergy) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        Customer customer = null;
        if (principal instanceof Customer) {
            customer = (Customer) principal;
        } else {
            return false;
        }
        // Check if the customer has the allergy with the given id
        return customer.getAllergies().stream()
                       .anyMatch(allergy -> allergy.getId().equals(idAllergy));
    }

    
}
