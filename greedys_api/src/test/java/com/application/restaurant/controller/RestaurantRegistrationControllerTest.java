package com.application.restaurant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.application.common.service.RestaurantService;
import com.application.common.web.dto.restaurant.RestaurantDTO;
import com.application.restaurant.service.authentication.RestaurantAuthenticationService;
import com.application.restaurant.web.dto.restaurant.NewRestaurantDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("RestaurantRegistrationController Tests")
class RestaurantRegistrationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RestaurantAuthenticationService authenticationService;

    @Mock
    private RestaurantService restaurantService;

    @InjectMocks
    private RestaurantRegistrationController controller;

    private NewRestaurantDTO newRestaurantRequest;
    private RestaurantDTO restaurantResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Setup request DTO
        newRestaurantRequest = NewRestaurantDTO.builder()
                .name("New Test Restaurant")
                .address("Via Roma 123")
                .city("Milano")
                .email("newrest@test.com")
                .build();

        // Setup response DTO
        restaurantResponse = RestaurantDTO.builder()
                .id(1L)
                .name("New Test Restaurant")
                .address("Via Roma 123")
                .email("newrest@test.com")
                .build();
    }

    @Nested
    @DisplayName("Restaurant Registration Tests")
    class RestaurantRegistrationTests {

        @Test
        @DisplayName("Should register restaurant successfully")
        void shouldRegisterRestaurantSuccessfully() throws Exception {
            // Given
            when(restaurantService.registerRestaurant(any(NewRestaurantDTO.class)))
                    .thenReturn(restaurantResponse);

            // When & Then
            mockMvc.perform(post("/restaurant/registration/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newRestaurantRequest)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Restaurant registered successfully"))
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andExpect(jsonPath("$.data.name").value("New Test Restaurant"));

            verify(restaurantService).registerRestaurant(any(NewRestaurantDTO.class));
        }

        @Test
        @DisplayName("Should handle registration validation errors")
        void shouldHandleRegistrationValidationErrors() throws Exception {
            // Given - invalid request (missing required fields)
            NewRestaurantDTO invalidRequest = NewRestaurantDTO.builder()
                    .name("") // Empty name should trigger validation error
                    .build();

            // When & Then
            mockMvc.perform(post("/restaurant/registration/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle duplicate restaurant email")
        void shouldHandleDuplicateRestaurantEmail() throws Exception {
            // Given
            when(restaurantService.registerRestaurant(any(NewRestaurantDTO.class)))
                    .thenThrow(new RuntimeException("Restaurant with this email already exists"));

            // When & Then
            mockMvc.perform(post("/restaurant/registration/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newRestaurantRequest)))
                    .andDo(print())
                    .andExpect(status().isConflict());

            verify(restaurantService).registerRestaurant(any(NewRestaurantDTO.class));
        }

        @Test
        @DisplayName("Should handle service errors during registration")
        void shouldHandleServiceErrorsDuringRegistration() throws Exception {
            // Given
            when(restaurantService.registerRestaurant(any(NewRestaurantDTO.class)))
                    .thenThrow(new RuntimeException("Internal service error"));

            // When & Then
            mockMvc.perform(post("/restaurant/registration/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newRestaurantRequest)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());

            verify(restaurantService).registerRestaurant(any(NewRestaurantDTO.class));
        }
    }

    @Nested
    @DisplayName("Password Reset Tests") 
    class PasswordResetTests {

        @Test
        @DisplayName("Should handle forgot password request")
        void shouldHandleForgotPasswordRequest() throws Exception {
            // Given
            String email = "test@restaurant.com";
            doNothing().when(authenticationService).forgotPassword(any(), any());

            // When & Then
            mockMvc.perform(post("/restaurant/registration/forgot-password")
                    .param("email", email))
                    .andDo(print())
                    .andExpect(status().isOk());

            verify(authenticationService).forgotPassword(email, any());
        }
    }

    // Nota: I test per verifyEmail, resendVerificationEmail, getAccountStatus, e acceptTermsAndConditions
    // sono stati rimossi perché questi metodi non esistono nel RestaurantAuthenticationService.
    // Per implementare questi test, dovremmo prima implementare questi metodi nel servizio
    // o creare mock più specifici che non assumano l'esistenza di metodi non implementati.
}
