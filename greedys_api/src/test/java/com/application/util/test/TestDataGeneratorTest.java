package com.application.util.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.application.customer.web.dto.customer.NewCustomerDTO;
import com.application.customer.web.dto.reservations.CustomerNewReservationDTO;

/**
 * Test per verificare la generazione di dati italiani casuali
 */
public class TestDataGeneratorTest {

    @Test
    @DisplayName("Dovrebbe generare un customer con dati italiani validi")
    public void testGenerateRandomCustomer() {
        // Given & When
        NewCustomerDTO customer = TestDataGenerator.generateRandomCustomer();

        // Then
        assertNotNull(customer);
        assertNotNull(customer.getFirstName());
        assertNotNull(customer.getLastName());
        assertNotNull(customer.getEmail());
        assertNotNull(customer.getPassword());
        
        // Verifica che i dati siano italiani
        assertTrue(customer.getEmail().contains("@"));
        assertFalse(customer.getFirstName().trim().isEmpty());
        assertFalse(customer.getLastName().trim().isEmpty());
        assertEquals(customer.getPassword(), customer.getMatchingPassword());
        
        System.out.println("Customer generato: " + customer.getFirstName() + " " + customer.getLastName() + 
                         " - " + customer.getEmail());
    }

    @Test
    @DisplayName("Dovrebbe generare una prenotazione con dati italiani validi")
    public void testGenerateRandomReservation() {
        // Given & When
        CustomerNewReservationDTO reservation = TestDataGenerator.generateRandomReservation();

        // Then
        assertNotNull(reservation);
        assertNotNull(reservation.getRestaurantId());
        assertNotNull(reservation.getUserName());
        assertNotNull(reservation.getIdSlot());
        assertNotNull(reservation.getReservationDay());
        assertTrue(reservation.getPax() > 0);
        assertTrue(reservation.getPax() <= 10);
        assertTrue(reservation.getKids() >= 0);
        
        System.out.println("Prenotazione generata: " + reservation.getUserName() + 
                         " per ristorante ID " + reservation.getRestaurantId() + 
                         " - " + reservation.getPax() + " adulti e " + reservation.getKids() + 
                         " bambini il " + reservation.getReservationDay());
    }

    @Test
    @DisplayName("Dovrebbe generare dati diversi ad ogni chiamata")
    public void testRandomDataVariation() {
        // Given & When
        NewCustomerDTO customer1 = TestDataGenerator.generateRandomCustomer();
        NewCustomerDTO customer2 = TestDataGenerator.generateRandomCustomer();
        NewCustomerDTO customer3 = TestDataGenerator.generateRandomCustomer();

        // Then
        // Verifica che i dati siano diversi (almeno uno deve essere diverso)
        boolean isDifferent = !customer1.getFirstName().equals(customer2.getFirstName()) ||
                            !customer1.getLastName().equals(customer2.getLastName()) ||
                            !customer2.getFirstName().equals(customer3.getFirstName()) ||
                            !customer2.getLastName().equals(customer3.getLastName());
        
        assertTrue(isDifferent, "I dati generati dovrebbero essere casuali e diversi");
        
        System.out.println("Customer 1: " + customer1.getFirstName() + " " + customer1.getLastName());
        System.out.println("Customer 2: " + customer2.getFirstName() + " " + customer2.getLastName());
        System.out.println("Customer 3: " + customer3.getFirstName() + " " + customer3.getLastName());
    }

    @Test
    @DisplayName("Dovrebbe generare un batch di prenotazioni")
    public void testGenerateBatchReservations() {
        // Given
        int numberOfReservations = 5;

        // When
        List<CustomerNewReservationDTO> reservations = TestDataGenerator.generateBatchReservations(numberOfReservations);

        // Then
        assertNotNull(reservations);
        assertEquals(numberOfReservations, reservations.size());
        
        reservations.forEach(reservation -> {
            assertNotNull(reservation.getRestaurantId());
            assertNotNull(reservation.getUserName());
            assertNotNull(reservation.getReservationDay());
            assertTrue(reservation.getPax() > 0);
        });

        System.out.println("Batch di " + numberOfReservations + " prenotazioni generate:");
        reservations.forEach(r -> 
            System.out.println("- " + r.getUserName() + " per ristorante ID " + r.getRestaurantId() + 
                             " - " + r.getPax() + " persone il " + r.getReservationDay())
        );
    }
}
