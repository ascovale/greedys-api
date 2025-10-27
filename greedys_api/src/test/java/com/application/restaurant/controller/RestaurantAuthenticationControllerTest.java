package com.application.restaurant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.application.common.security.jwt.JwtUtil;
import com.application.common.web.dto.restaurant.RestaurantDTO;
import com.application.common.web.dto.restaurant.RUserDTO;
import com.application.common.web.dto.security.AuthRequestDTO;
import com.application.common.web.dto.security.AuthRequestGoogleDTO;
import com.application.common.web.dto.security.AuthResponseDTO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.service.authentication.RestaurantAuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test class for RestaurantAuthenticationController
 * 
 * This test class demonstrates modern Spring Boot testing patterns:
 * - Using @ExtendWith(MockitoExtension.class) instead of deprecated @MockBean
 * - Standalone MockMvc setup for isolated controller testing
 * - Pure Mockito with @Mock and @InjectMocks annotations
 * - Comprehensive test coverage with nested test classes
 * - Given-When-Then test structure
 * - JSON path assertions for response validation
 * 
 * Note: These tests focus on the actual methods available in RestaurantAuthenticationService
 * rather than hypothetical methods. They test the service layer integration patterns
 * and demonstrate how modern Spring Boot testing should be structured.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RestaurantAuthenticationController Tests")
class RestaurantAuthenticationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RestaurantAuthenticationService authenticationService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private RestaurantAuthenticationController controller;

    private RUser mockRUser;
    private Restaurant mockRestaurant;
    private Authentication mockAuthentication;
    private AuthResponseDTO authResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Setup mock restaurant
        mockRestaurant = new Restaurant();
        mockRestaurant.setId(1L);
        mockRestaurant.setName("Test Restaurant");

        // Setup mock user
        mockRUser = new RUser();
        mockRUser.setId(1L);
        mockRUser.setEmail("test@restaurant.com");
        mockRUser.setRestaurant(mockRestaurant);

        // Setup authentication
        mockAuthentication = new UsernamePasswordAuthenticationToken(
                mockRUser,
                null,
                Arrays.asList(new SimpleGrantedAuthority("ROLE_RESTAURANT"))
        );

        // Setup DTOs with correct AuthResponseDTO properties (jwt, refreshToken, user)
        RUserDTO mockUserDTO = RUserDTO.builder()
                .id(1L)
                .username("test@restaurant.com")
                .restaurantId(1L)
                .build();
        
        authResponse = AuthResponseDTO.builder()
                .jwt("test-jwt-token")
                .refreshToken("test-refresh-token")
                .user(mockUserDTO)
                .build();
    }

    @Nested
    @DisplayName("Authentication Method Tests")
    class AuthenticationMethodTests {

        @Test
        @DisplayName("Should test login with hub support")
        void shouldTestLoginWithHubSupport() throws Exception {
            // Given
            AuthRequestDTO authRequest = AuthRequestDTO.builder()
                    .username("test@restaurant.com")
                    .password("password123")
                    .rememberMe(true)
                    .build();

            when(authenticationService.loginWithHubSupport(any(AuthRequestDTO.class)))
                    .thenReturn(authResponse);

            String loginRequest = objectMapper.writeValueAsString(authRequest);

            // When & Then
            mockMvc.perform(post("/restaurant/auth/login-hub")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequest))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.jwt").value("test-jwt-token"));

            verify(authenticationService).loginWithHubSupport(any(AuthRequestDTO.class));
        }

        @Test
        @DisplayName("Should test refresh R-User token")
        void shouldTestRefreshRUserToken() throws Exception {
            // Given
            String refreshToken = "test-refresh-token";
            when(authenticationService.refreshRUserToken(refreshToken))
                    .thenReturn(authResponse);

            String refreshRequest = """
                {
                    "refreshToken": "%s"
                }
                """.formatted(refreshToken);

            // When & Then
            mockMvc.perform(post("/restaurant/auth/refresh-ruser")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refreshRequest))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.jwt").value("test-jwt-token"));

            verify(authenticationService).refreshRUserToken(refreshToken);
        }

        @Test
        @DisplayName("Should test refresh hub token")
        void shouldTestRefreshHubToken() throws Exception {
            // Given
            String refreshToken = "test-hub-refresh-token";
            when(authenticationService.refreshHubToken(refreshToken))
                    .thenReturn(authResponse);

            String refreshRequest = """
                {
                    "refreshToken": "%s"
                }
                """.formatted(refreshToken);

            // When & Then
            mockMvc.perform(post("/restaurant/auth/refresh-hub")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refreshRequest))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.jwt").value("test-jwt-token"));

            verify(authenticationService).refreshHubToken(refreshToken);
        }
    }

    @Nested
    @DisplayName("Google Authentication Tests")
    class GoogleAuthenticationTests {

        @Test
        @DisplayName("Should test Google authentication")
        void shouldTestGoogleAuthentication() throws Exception {
            // Given
            AuthRequestGoogleDTO googleAuthRequest = AuthRequestGoogleDTO.builder()
                    .token("google-id-token")
                    .build();

            when(authenticationService.loginWithGoogle(any(AuthRequestGoogleDTO.class)))
                    .thenReturn(authResponse);

            String request = objectMapper.writeValueAsString(googleAuthRequest);

            // When & Then
            mockMvc.perform(post("/restaurant/auth/google")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.jwt").value("test-jwt-token"));

            verify(authenticationService).loginWithGoogle(any(AuthRequestGoogleDTO.class));
        }

        @Test
        @DisplayName("Should handle invalid Google token")
        void shouldHandleInvalidGoogleToken() throws Exception {
            // Given
            AuthRequestGoogleDTO googleAuthRequest = AuthRequestGoogleDTO.builder()
                    .token("invalid-google-token")
                    .build();

            when(authenticationService.loginWithGoogle(any(AuthRequestGoogleDTO.class)))
                    .thenThrow(new RuntimeException("Invalid Google token"));

            String request = objectMapper.writeValueAsString(googleAuthRequest);

            // When & Then
            mockMvc.perform(post("/restaurant/auth/google")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());

            verify(authenticationService).loginWithGoogle(any(AuthRequestGoogleDTO.class));
        }
    }

    @Nested
    @DisplayName("Restaurant Selection Tests")
    class RestaurantSelectionTests {

        @Test
        @DisplayName("Should test restaurant selection")
        void shouldTestRestaurantSelection() throws Exception {
            // Given
            Long restaurantId = 1L;
            when(authenticationService.selectRestaurant(restaurantId))
                    .thenReturn(authResponse);

            // When & Then
            mockMvc.perform(post("/restaurant/auth/select-restaurant/{id}", restaurantId)
                    .with(authentication(mockAuthentication)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.jwt").value("test-jwt-token"));

            verify(authenticationService).selectRestaurant(restaurantId);
        }

        @Test
        @DisplayName("Should test restaurant change")
        void shouldTestRestaurantChange() throws Exception {
            // Given
            Long restaurantId = 2L;
            when(authenticationService.changeRestaurant(restaurantId))
                    .thenReturn(authResponse);

            // When & Then
            mockMvc.perform(post("/restaurant/auth/change-restaurant/{id}", restaurantId)
                    .with(authentication(mockAuthentication)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.jwt").value("test-jwt-token"));

            verify(authenticationService).changeRestaurant(restaurantId);
        }
    }

    @Nested
    @DisplayName("User Management Tests")
    class UserManagementTests {

        @Test
        @DisplayName("Should test get restaurants for user hub")
        void shouldTestGetRestaurantsForUserHub() throws Exception {
            // Given
            String email = "test@restaurant.com";
            List<RestaurantDTO> restaurants = Arrays.asList(
                RestaurantDTO.builder()
                    .id(1L)
                    .name("Test Restaurant")
                    .email("test@restaurant.com")
                    .build()
            );

            when(authenticationService.getRestaurantsForUserHub(email))
                    .thenReturn(restaurants);

            // When & Then
            mockMvc.perform(get("/restaurant/auth/hub-restaurants")
                    .param("email", email))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].name").value("Test Restaurant"));

            verify(authenticationService).getRestaurantsForUserHub(email);
        }

        @Test
        @DisplayName("Should test get restaurants by user hub ID")
        void shouldTestGetRestaurantsByUserHubId() throws Exception {
            // Given
            Long userHubId = 1L;
            List<RestaurantDTO> restaurants = Arrays.asList(
                RestaurantDTO.builder()
                    .id(1L)
                    .name("Test Restaurant")
                    .email("test@restaurant.com")
                    .build()
            );

            when(authenticationService.getRestaurantsByUserHubId(userHubId))
                    .thenReturn(restaurants);

            // When & Then
            mockMvc.perform(get("/restaurant/auth/hub/{id}/restaurants", userHubId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].name").value("Test Restaurant"));

            verify(authenticationService).getRestaurantsByUserHubId(userHubId);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle authentication service errors gracefully")
        void shouldHandleAuthenticationServiceErrors() throws Exception {
            // Given
            AuthRequestDTO authRequest = AuthRequestDTO.builder()
                    .username("test@restaurant.com")
                    .password("wrong-password")
                    .build();

            when(authenticationService.loginWithHubSupport(any(AuthRequestDTO.class)))
                    .thenThrow(new RuntimeException("Authentication failed"));

            String loginRequest = objectMapper.writeValueAsString(authRequest);

            // When & Then
            mockMvc.perform(post("/restaurant/auth/login-hub")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequest))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());

            verify(authenticationService).loginWithHubSupport(any(AuthRequestDTO.class));
        }

        @Test
        @DisplayName("Should handle invalid restaurant selection")
        void shouldHandleInvalidRestaurantSelection() throws Exception {
            // Given
            Long invalidRestaurantId = 999L;
            when(authenticationService.selectRestaurant(invalidRestaurantId))
                    .thenThrow(new RuntimeException("Restaurant not found"));

            // When & Then
            mockMvc.perform(post("/restaurant/auth/select-restaurant/{id}", invalidRestaurantId)
                    .with(authentication(mockAuthentication)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());

            verify(authenticationService).selectRestaurant(invalidRestaurantId);
        }
    }
}
