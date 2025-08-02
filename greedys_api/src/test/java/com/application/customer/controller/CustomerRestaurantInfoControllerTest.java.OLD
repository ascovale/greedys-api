package com.application.customer.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.application.common.service.RestaurantService;
import com.application.customer.controller.restaurant.CustomerRestaurantInfoController;
import com.application.restaurant.persistence.model.Restaurant;

/**
 * Test per CustomerRestaurantInfoController
 * 
 * Questo test dimostra come usare i mock per testare un controller senza:
 * - Database reale
 * - Autenticazione/sicurezza
 * - Dipendenze esterne
 */
@ExtendWith(MockitoExtension.class)
class CustomerRestaurantInfoControllerTest {

    @Mock
    private RestaurantService restaurantService;

    @InjectMocks
    private CustomerRestaurantInfoController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Configurazione di MockMvc per simulare le chiamate HTTP
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @Test
    void getRestaurants_ShouldReturnListOfRestaurants_WhenRestaurantsExist() throws Exception {
        // ARRANGE - Prepara i dati mock
        Restaurant restaurant1 = createMockRestaurant(1L, "Ristorante Da Mario", "via Roma 1");
        Restaurant restaurant2 = createMockRestaurant(2L, "Pizzeria Bella Vista", "via Milano 5");
        
        List<Restaurant> mockRestaurants = Arrays.asList(restaurant1, restaurant2);
        
        // Mock del comportamento del service
        when(restaurantService.findAll()).thenReturn(mockRestaurants);

        // ACT & ASSERT - Esegui la chiamata e verifica il risultato
        mockMvc.perform(get("/customer/restaurant")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Ristorante Da Mario"))
                .andExpect(jsonPath("$.data[1].name").value("Pizzeria Bella Vista"));
    }

    @Test
    void getRestaurants_ShouldReturnEmptyList_WhenNoRestaurantsExist() throws Exception {
        // ARRANGE
        when(restaurantService.findAll()).thenReturn(Arrays.asList());

        // ACT & ASSERT
        mockMvc.perform(get("/customer/restaurant")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void getOpenDays_ShouldReturnOpenDays_WhenValidDateRange() throws Exception {
        // ARRANGE
        Long restaurantId = 1L;
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);
        
        Collection<String> mockOpenDays = Arrays.asList("2025-01-02", "2025-01-03", "2025-01-04");
        
        when(restaurantService.getOpenDays(restaurantId, startDate, endDate))
                .thenReturn(mockOpenDays);

        // ACT & ASSERT
        mockMvc.perform(get("/customer/restaurant/{restaurantId}/open-days", restaurantId)
                .param("start", "01-01-2025")
                .param("end", "31-01-2025")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    /**
     * Test per verificare la gestione degli errori
     * Simula un'eccezione nel service
     */
    @Test
    void getRestaurants_ShouldReturnError_WhenServiceThrowsException() throws Exception {
        // ARRANGE - Simula un errore nel service
        when(restaurantService.findAll()).thenThrow(new RuntimeException("Database connection failed"));

        // ACT & ASSERT
        mockMvc.perform(get("/customer/restaurant")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"));
    }

    /**
     * Helper method per creare oggetti Restaurant mock
     */
    private Restaurant createMockRestaurant(Long id, String name, String address) {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(id);
        restaurant.setName(name);
        restaurant.setAddress(address);
        restaurant.setEmail(name.toLowerCase().replace(" ", "") + "@test.com");
        restaurant.setVatNumber("IT12345678901");
        restaurant.setPostCode("00100");
        restaurant.setStatus(Restaurant.Status.ENABLED);
        restaurant.setCreationDate(LocalDate.now());
        return restaurant;
    }
}
