package com.application.common.utils;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Component;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.reservation.Slot;
import com.application.common.web.dto.restaurant.RestaurantDTO;
import com.application.customer.persistence.model.Customer;
import com.application.customer.web.dto.customer.NewCustomerDTO;
import com.application.customer.web.dto.reservations.CustomerNewReservationDTO;
import com.application.restaurant.persistence.model.Restaurant;

/**
 * Utility class per generare dati di test random
 * Utile per testare l'applicazione con scenari diversi
 */
@Component
public class TestDataGenerator {

    private final Random random = new Random();
    
    // Dati statici per la generazione
    private final List<String> firstNames = List.of("Mario", "Luigi", "Giuseppe", "Francesco", "Antonio", 
            "Alessandro", "Andrea", "Marco", "Matteo", "Davide", "Francesca", "Giulia", "Anna", 
            "Sara", "Chiara", "Elena", "Laura", "Marta", "Valentina", "Silvia");
    
    private final List<String> lastNames = List.of("Rossi", "Russo", "Ferrari", "Esposito", "Bianchi", 
            "Romano", "Colombo", "Ricci", "Marino", "Greco", "Bruno", "Gallo", "Conti", "De Luca", 
            "Mancini", "Costa", "Giordano", "Rizzo", "Lombardi", "Moretti");
    
    private final List<String> cities = List.of("Milano", "Roma", "Napoli", "Torino", "Palermo", 
            "Genova", "Bologna", "Firenze", "Bari", "Catania", "Venezia", "Verona", "Messina", 
            "Padova", "Trieste");
    
    private final List<String> streets = List.of("Via Roma", "Via Milano", "Corso Italia", 
            "Via Garibaldi", "Piazza Duomo", "Via Dante", "Corso Buenos Aires", "Via Veneto", 
            "Via del Corso", "Piazza San Marco");
    
    private final List<String> notes = List.of("Tavolo vicino alla finestra", "Allergia ai crostacei", 
            "Compleanno", "Anniversario", "Cena romantica", "Business dinner", "Famiglia numerosa", 
            "Tavolo tranquillo", "Vista mare", "Menu vegetariano");

    /**
     * Genera un customer random per i test
     */
    public NewCustomerDTO generateRandomCustomer() {
        return NewCustomerDTO.builder()
                .email(generateRandomEmail())
                .firstName(getRandomElement(firstNames))
                .lastName(getRandomElement(lastNames))
                .password("password123")  // Password di default per i test
                .build();
    }

    /**
     * Genera una prenotazione random con ID di default (per i test)
     */
    public CustomerNewReservationDTO generateRandomReservation() {
        return generateRandomReservation(1L, 1L); // Usa ID di default per i test
    }

    /**
     * Genera una prenotazione random per un customer
     */
    public CustomerNewReservationDTO generateRandomReservation(Long restaurantId, Long slotId) {
        LocalDate futureDate = LocalDate.now().plusDays(random.nextInt(30) + 1);
        
        return CustomerNewReservationDTO.builder()
                .restaurantId(restaurantId)
                .idSlot(slotId)
                .reservationDay(futureDate)
                .pax(random.nextInt(8) + 1) // 1-8 persone
                .kids(random.nextInt(4)) // 0-3 bambini
                .notes(getRandomElement(notes))
                .userName(getRandomElement(firstNames) + " " + getRandomElement(lastNames))
                .build();
    }

    /**
     * Genera una prenotazione con dati specifici
     */
    public CustomerNewReservationDTO generateReservationWithData(
            Long restaurantId, 
            Long slotId, 
            LocalDate date, 
            int pax, 
            String notes) {
        
        return CustomerNewReservationDTO.builder()
                .restaurantId(restaurantId)
                .idSlot(slotId)
                .reservationDay(date)
                .pax(pax)
                .kids(random.nextInt(pax > 2 ? 2 : 1)) // Bambini massimo 2 o pax/2
                .notes(notes)
                .userName(getRandomElement(firstNames) + " " + getRandomElement(lastNames))
                .build();
    }

    /**
     * Genera un ristorante random
     */
    public RestaurantDTO generateRandomRestaurant() {
        return RestaurantDTO.builder()
                .name(generateRestaurantName())
                .address(generateRandomAddress())
                .email(generateRandomEmail())
                .build();
    }

    /**
     * Genera un Customer entity per i test
     */
    public Customer generateCustomerEntity() {
        return Customer.builder()
                .id((long) random.nextInt(1000))
                .email(generateRandomEmail())
                .name(getRandomElement(firstNames))
                .surname(getRandomElement(lastNames))
                .phoneNumber(generateItalianPhoneNumber())
                .status(Customer.Status.ENABLED)
                .build();
    }

    /**
     * Genera un Restaurant entity per i test
     */
    public Restaurant generateRestaurantEntity() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId((long) random.nextInt(1000));
        restaurant.setName(generateRestaurantName());
        restaurant.setAddress(generateRandomAddress());
        restaurant.setPhoneNumber(generateItalianPhoneNumber());
        restaurant.setEmail(generateRandomEmail());
        return restaurant;
    }

    /**
     * Genera una prenotazione entity per i test
     */
    public Reservation generateReservationEntity(Customer customer, Slot slot) {
        LocalDate futureDate = LocalDate.now().plusDays(random.nextInt(30) + 1);
        
        return Reservation.builder()
                .id((long) random.nextInt(1000))
                .customer(customer)
                .slot(slot)
                .date(futureDate)
                .pax(random.nextInt(8) + 1)
                .kids(random.nextInt(4))
                .notes(getRandomElement(notes))
                .userName(customer.getName() + " " + customer.getSurname())
                .status(Reservation.Status.ACCEPTED)
                .build();
    }

    /**
     * Genera un batch di prenotazioni per stress test
     */
    public List<CustomerNewReservationDTO> generateBatchReservations(
            int count, 
            Long restaurantId, 
            Long slotId) {
        
        return random.ints(count, 1, 31) // Giorni futuri da 1 a 30
                .mapToObj(days -> {
                    LocalDate date = LocalDate.now().plusDays(days);
                    return generateReservationWithData(
                            restaurantId, 
                            slotId, 
                            date, 
                            random.nextInt(6) + 2, // 2-7 persone
                            "Prenotazione batch test #" + random.nextInt(1000)
                    );
                })
                .toList();
    }

    /**
     * Genera scenari di test completi
     */
    public TestScenario generateCompleteTestScenario() {
        NewCustomerDTO customer = generateRandomCustomer();
        RestaurantDTO restaurant = generateRandomRestaurant();
        Long slotId = 1L; // Assumiamo che esista uno slot con ID 1
        CustomerNewReservationDTO reservation = generateRandomReservation(1L, slotId);
        
        return TestScenario.builder()
                .customer(customer)
                .restaurant(restaurant)
                .reservation(reservation)
                .testDescription("Scenario completo: " + customer.getFirstName() + 
                               " prenota al " + restaurant.getName())
                .build();
    }

    /**
     * Genera scenari problematici per test edge cases
     */
    public List<CustomerNewReservationDTO> generateEdgeCaseReservations(Long restaurantId, Long slotId) {
        LocalDate today = LocalDate.now();
        
        return List.of(
                // Prenotazione per oggi (potrebbe essere non valida)
                generateReservationWithData(restaurantId, slotId, today, 1, "Prenotazione stesso giorno"),
                
                // Prenotazione per molte persone
                generateReservationWithData(restaurantId, slotId, today.plusDays(1), 15, "Gruppo numeroso"),
                
                // Prenotazione senza note
                generateReservationWithData(restaurantId, slotId, today.plusDays(2), 2, ""),
                
                // Prenotazione solo bambini
                CustomerNewReservationDTO.builder()
                        .restaurantId(restaurantId)
                        .idSlot(slotId)
                        .reservationDay(today.plusDays(3))
                        .pax(0) // Solo bambini
                        .kids(3)
                        .notes("Solo bambini")
                        .userName("Test Solo Bambini")
                        .build()
        );
    }

    // Metodi helper privati
    
    private String getRandomElement(List<String> list) {
        return list.get(random.nextInt(list.size()));
    }
    
    private String generateRandomEmail() {
        String firstName = getRandomElement(firstNames).toLowerCase();
        String lastName = getRandomElement(lastNames).toLowerCase();
        List<String> domains = List.of("gmail.com", "libero.it", "yahoo.it", "hotmail.com", "outlook.com");
        return firstName + "." + lastName + "@" + getRandomElement(domains);
    }
    
    private String generateRandomAddress() {
        String street = getRandomElement(streets);
        int number = random.nextInt(200) + 1;
        String city = getRandomElement(cities);
        return street + " " + number + ", " + city;
    }
    
    private String generateRestaurantName() {
        List<String> prefixes = List.of("Ristorante", "Trattoria", "Osteria", "Pizzeria", "Taverna");
        List<String> names = List.of("Bella Vista", "Da Mario", "Il Borgo", "La Terrazza", "Al Castello", 
                                   "La Piazzetta", "Il Gabbiano", "Sole Mio", "La Pergola", "Il Convivio");
        
        String prefix = getRandomElement(prefixes);
        String name = getRandomElement(names);
        
        return prefix + " " + name;
    }

    private String generateItalianPhoneNumber() {
        // Genera un numero di telefono italiano realistico
        List<String> prefixes = List.of("338", "339", "340", "347", "348", "349", "380", "383", "388", "389");
        String prefix = getRandomElement(prefixes);
        String number = String.format("%07d", random.nextInt(10000000));
        return prefix + number;
    }

    /**
     * Classe per raggruppare uno scenario di test completo
     */
    @lombok.Data
    @lombok.Builder
    public static class TestScenario {
        private NewCustomerDTO customer;
        private RestaurantDTO restaurant;
        private CustomerNewReservationDTO reservation;
        private String testDescription;
    }
}
