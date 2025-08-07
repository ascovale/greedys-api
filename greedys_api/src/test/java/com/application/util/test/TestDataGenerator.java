package com.application.util.test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.application.customer.web.dto.customer.NewCustomerDTO;
import com.application.customer.web.dto.reservations.CustomerNewReservationDTO;

/**
 * Utility per generare dati di test italiani casuali
 */
public class TestDataGenerator {
    
    private static final Random random = new Random();
    
    // Nomi italiani tipici
    private static final List<String> ITALIAN_FIRST_NAMES = Arrays.asList(
        "Marco", "Luigi", "Giuseppe", "Francesco", "Antonio", "Alessandro", "Andrea", "Giovanni", "Roberto", "Stefano",
        "Maria", "Anna", "Francesca", "Laura", "Paola", "Carla", "Giulia", "Chiara", "Sara", "Valentina",
        "Matteo", "Lorenzo", "Luca", "Davide", "Fabio", "Simone", "Federico", "Riccardo", "Nicola", "Emanuele",
        "Elisabetta", "Silvia", "Monica", "Roberta", "Daniela", "Alessandra", "Claudia", "Barbara", "Teresa", "Martina"
    );
    
    // Cognomi italiani tipici
    private static final List<String> ITALIAN_LAST_NAMES = Arrays.asList(
        "Rossi", "Russo", "Ferrari", "Esposito", "Bianchi", "Romano", "Colombo", "Ricci", "Marino", "Greco",
        "Bruno", "Gallo", "Conti", "De Luca", "Mancini", "Costa", "Giordano", "Rizzo", "Lombardi", "Moretti",
        "Barbieri", "Fontana", "Santoro", "Mariani", "Rinaldi", "Caruso", "Ferrara", "Galli", "Martini", "Leone",
        "Longo", "Gentile", "Martinelli", "Vitale", "Lombardo", "Serra", "Coppola", "De Santis", "D'Angelo", "Marchetti"
    );
    
    // Città italiane
    private static final List<String> ITALIAN_CITIES = Arrays.asList(
        "Roma", "Milano", "Napoli", "Torino", "Palermo", "Genova", "Bologna", "Firenze", "Bari", "Catania",
        "Venezia", "Verona", "Messina", "Padova", "Trieste", "Brescia", "Taranto", "Prato", "Modena", "Reggio Calabria",
        "Perugia", "Livorno", "Cagliari", "Foggia", "Rimini", "Salerno", "Ferrara", "Sassari", "Latina", "Giugliano"
    );
    
    // Tipi di ristoranti italiani
    private static final List<String> RESTAURANT_TYPES = Arrays.asList(
        "Pizzeria", "Trattoria", "Osteria", "Ristorante", "Taverna", "Locanda", "Enoteca con Cucina", "Braceria"
    );
    
    // Nomi di ristoranti tipici
    private static final List<String> RESTAURANT_NAMES = Arrays.asList(
        "Bella Vista", "Il Gambero Rosso", "La Tavola della Nonna", "Il Castello", "Da Mario", "La Piazzetta",
        "Il Convivio", "Osteria del Borgo", "Trattoria del Porto", "La Cantina", "Il Sole", "Da Antonio",
        "La Pergola", "Il Giardino", "Hosteria del Centro", "La Terrazza", "Il Melograno", "Da Giuseppe"
    );
    
    /**
     * Genera un customer italiano casuale
     */
    public static NewCustomerDTO generateRandomCustomer() {
        NewCustomerDTO customer = new NewCustomerDTO();
        
        String firstName = getRandomItem(ITALIAN_FIRST_NAMES);
        String lastName = getRandomItem(ITALIAN_LAST_NAMES);
        
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@gmail.com");
        customer.setPassword("TestPassword123!");
        customer.setMatchingPassword("TestPassword123!");
        
        return customer;
    }
    
    /**
     * Genera una prenotazione italiana casuale
     */
    public static CustomerNewReservationDTO generateRandomReservation() {
        return generateReservationWithData(
            getRandomRestaurantId(),
            getRandomSlotId(),
            2 + random.nextInt(9), // 2-10 persone
            random.nextInt(3), // 0-2 bambini
            generateRandomNotes(),
            LocalDateTime.now().toLocalDate().plusDays(random.nextInt(30) + 1) // 1-30 giorni nel futuro
        );
    }
    
    /**
     * Genera una prenotazione con dati specifici
     */
    public static CustomerNewReservationDTO generateReservationWithData(
            Long restaurantId, 
            Long slotId, 
            int adults, 
            int kids, 
            String notes, 
            java.time.LocalDate reservationDay) {
        
        String userName = getRandomItem(ITALIAN_FIRST_NAMES) + " " + getRandomItem(ITALIAN_LAST_NAMES);
        
        return CustomerNewReservationDTO.builder()
                .restaurantId(restaurantId)
                .userName(userName)
                .idSlot(slotId)
                .pax(adults)
                .kids(kids)
                .notes(notes)
                .reservationDay(reservationDay)
                .build();
    }
    
    /**
     * Genera un batch di prenotazioni
     */
    public static List<CustomerNewReservationDTO> generateBatchReservations(int count) {
        List<CustomerNewReservationDTO> reservations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            reservations.add(generateRandomReservation());
        }
        return reservations;
    }
    
    /**
     * Genera un ID ristorante casuale
     */
    private static Long getRandomRestaurantId() {
        return 1L + random.nextInt(100); // ID da 1 a 100
    }
    
    /**
     * Genera un ID slot casuale
     */
    private static Long getRandomSlotId() {
        return 1L + random.nextInt(50); // ID slot da 1 a 50
    }
    
    /**
     * Genera note casuali per la prenotazione
     */
    private static String generateRandomNotes() {
        List<String> notes = Arrays.asList(
            "Tavolo vicino alla finestra",
            "Intolleranza al glutine",
            "Compleanno - servire dolce",
            "Tavolo silenzioso",
            "Allergia ai frutti di mare",
            "Cena romantica",
            "Tavolo per bambini",
            "Festa aziendale",
            null // Alcune prenotazioni senza note
        );
        return getRandomItem(notes);
    }
    
    /**
     * Metodo di utilità per selezionare un elemento casuale da una lista
     */
    private static <T> T getRandomItem(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }
}
