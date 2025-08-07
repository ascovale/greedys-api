package com.application.restaurant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.service.reservation.ReservationService;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.service.RestaurantNotificationService;
import com.application.restaurant.web.dto.reservation.RestaurantNewReservationDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("RestaurantReservationController Tests")
class RestaurantReservationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ReservationService reservationService;

    @Mock
    private RestaurantNotificationService restaurantNotificationService;

    @InjectMocks
    private RestaurantReservationController controller;

    private RUser mockRUser;
    private Restaurant mockRestaurant;
    private Authentication mockAuthentication;
    private RestaurantNewReservationDTO reservationRequest;
    private ReservationDTO reservationResponse;

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

        // Setup authentication with required authority
        mockAuthentication = new UsernamePasswordAuthenticationToken(
                mockRUser,
                null,
                Arrays.asList(new SimpleGrantedAuthority("PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE"))
        );

        // Setup request DTO - using builder
        reservationRequest = RestaurantNewReservationDTO.builder()
                .pax(4)
                .reservationDay(LocalDate.now().plusDays(1))
                .idSlot(1L)
                .kids(0)
                .notes("Test reservation")
                .build();

        // Setup response DTO - using builder
        reservationResponse = ReservationDTO.builder()
                .id(1L)
                .pax(4)
                .reservationDay(LocalDate.now().plusDays(1))
                .name("John Doe")
                .phone("+1234567890")
                .status(Reservation.Status.NOT_ACCEPTED)
                .build();
    }

    @Nested
    @DisplayName("Create Reservation Tests")
    class CreateReservationTests {

        @Test
        @DisplayName("Should create reservation successfully")
        void shouldCreateReservationSuccessfully() throws Exception {
            // Given
            doNothing().when(reservationService).createReservation(any(RestaurantNewReservationDTO.class), any(Restaurant.class));
            doNothing().when(restaurantNotificationService).sendNotificationToAllUsers(anyString(), anyString(), any(), anyLong());

            // When & Then
            mockMvc.perform(post("/restaurant/reservation/new")
                    .with(authentication(mockAuthentication))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reservationRequest)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Reservation created successfully"))
                    .andExpect(jsonPath("$.data").value("success"));

            // Verify interactions
            verify(reservationService).createReservation(any(RestaurantNewReservationDTO.class), any(Restaurant.class));
            verify(restaurantNotificationService).sendNotificationToAllUsers(
                    anyString(), anyString(), any(), anyLong()
            );
        }

        @Test
        @DisplayName("Should handle service exception gracefully")
        void shouldHandleServiceExceptionGracefully() throws Exception {
            // Given
            doThrow(new RuntimeException("Service error")).when(reservationService)
                    .createReservation(any(RestaurantNewReservationDTO.class), any(Restaurant.class));

            // When & Then
            mockMvc.perform(post("/restaurant/reservation/new")
                    .with(authentication(mockAuthentication))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reservationRequest)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("Should validate request body")
        void shouldValidateRequestBody() throws Exception {
            // Given - invalid request (null values)
            RestaurantNewReservationDTO invalidRequest = RestaurantNewReservationDTO.builder().build();

            // When & Then
            mockMvc.perform(post("/restaurant/reservation/new")
                    .with(authentication(mockAuthentication))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Accept Reservation Tests")
    class AcceptReservationTests {

        @Test
        @DisplayName("Should accept reservation successfully")
        void shouldAcceptReservationSuccessfully() throws Exception {
            // Given
            Long reservationId = 1L;
            doNothing().when(reservationService).setStatus(reservationId, Reservation.Status.ACCEPTED);

            // When & Then
            mockMvc.perform(put("/restaurant/reservation/{reservationId}/accept", reservationId)
                    .with(authentication(mockAuthentication)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Reservation accepted successfully"));

            verify(reservationService).setStatus(reservationId, Reservation.Status.ACCEPTED);
        }

        @Test
        @DisplayName("Should handle invalid reservation ID")
        void shouldHandleInvalidReservationId() throws Exception {
            // Given
            Long invalidId = 999L;
            doThrow(new RuntimeException("Reservation not found")).when(reservationService)
                    .setStatus(invalidId, Reservation.Status.ACCEPTED);

            // When & Then
            mockMvc.perform(put("/restaurant/reservation/{reservationId}/accept", invalidId)
                    .with(authentication(mockAuthentication)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Reject Reservation Tests")
    class RejectReservationTests {

        @Test
        @DisplayName("Should reject reservation successfully")
        void shouldRejectReservationSuccessfully() throws Exception {
            // Given
            Long reservationId = 1L;
            doNothing().when(reservationService).setStatus(reservationId, Reservation.Status.REJECTED);

            // When & Then
            mockMvc.perform(put("/restaurant/reservation/{reservationId}/reject", reservationId)
                    .with(authentication(mockAuthentication)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(reservationService).setStatus(reservationId, Reservation.Status.REJECTED);
        }
    }

    @Nested
    @DisplayName("Mark No Show Tests")
    class MarkNoShowTests {

        @Test
        @DisplayName("Should mark reservation as no show successfully")
        void shouldMarkReservationAsNoShowSuccessfully() throws Exception {
            // Given
            Long reservationId = 1L;
            doNothing().when(reservationService).setStatus(reservationId, Reservation.Status.NO_SHOW);

            // When & Then
            mockMvc.perform(put("/restaurant/reservation/{reservationId}/no_show", reservationId)
                    .with(authentication(mockAuthentication)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(reservationService).setStatus(reservationId, Reservation.Status.NO_SHOW);
        }
    }

    @Nested
    @DisplayName("Mark Seated Tests")
    class MarkSeatedTests {

        @Test
        @DisplayName("Should mark reservation as seated successfully")
        void shouldMarkReservationAsSeatedSuccessfully() throws Exception {
            // Given
            Long reservationId = 1L;
            doNothing().when(reservationService).setStatus(reservationId, Reservation.Status.SEATED);

            // When & Then
            mockMvc.perform(put("/restaurant/reservation/{reservationId}/seated", reservationId)
                    .with(authentication(mockAuthentication)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(reservationService).setStatus(reservationId, Reservation.Status.SEATED);
        }
    }

    @Nested
    @DisplayName("Get Reservations Tests")
    class GetReservationsTests {

        @Test
        @DisplayName("Should get reservations successfully")
        void shouldGetReservationsSuccessfully() throws Exception {
            // Given
            LocalDate start = LocalDate.now();
            LocalDate end = LocalDate.now().plusDays(7);
            List<ReservationDTO> reservations = Arrays.asList(reservationResponse);
            
            when(reservationService.getReservations(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(reservations);

            // When & Then
            mockMvc.perform(get("/restaurant/reservation/reservations")
                    .param("start", "07-08-2025")
                    .param("end", "14-08-2025")
                    .with(authentication(mockAuthentication)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(1L));

            verify(reservationService).getReservations(1L, start, end);
        }

        @Test
        @DisplayName("Should handle invalid date format")
        void shouldHandleInvalidDateFormat() throws Exception {
            // When & Then
            mockMvc.perform(get("/restaurant/reservation/reservations")
                    .param("start", "invalid-date")
                    .param("end", "14-08-2025")
                    .with(authentication(mockAuthentication)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Get Accepted Reservations Tests")
    class GetAcceptedReservationsTests {

        @Test
        @DisplayName("Should get accepted reservations successfully")
        void shouldGetAcceptedReservationsSuccessfully() throws Exception {
            // Given
            LocalDate start = LocalDate.now();
            LocalDate end = LocalDate.now().plusDays(7);
            reservationResponse.setStatus(Reservation.Status.ACCEPTED);
            List<ReservationDTO> reservations = Arrays.asList(reservationResponse);
            
            when(reservationService.getAcceptedReservations(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(reservations);

            // When & Then
            mockMvc.perform(get("/restaurant/reservation/accepted/get")
                    .param("start", "07-08-2025")
                    .param("end", "14-08-2025")
                    .with(authentication(mockAuthentication)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].status").value("ACCEPTED"));

            verify(reservationService).getAcceptedReservations(1L, start, end);
        }
    }

    @Nested
    @DisplayName("Get Reservations Pageable Tests")
    class GetReservationsPageableTests {

        @Test
        @DisplayName("Should get reservations with pagination successfully")
        void shouldGetReservationsWithPaginationSuccessfully() throws Exception {
            // Given
            LocalDate start = LocalDate.now();
            LocalDate end = LocalDate.now().plusDays(7);
            List<ReservationDTO> reservations = Arrays.asList(reservationResponse);
            Page<ReservationDTO> page = new PageImpl<>(reservations, PageRequest.of(0, 10), 1);
            
            when(reservationService.getReservationsPageable(anyLong(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(page);

            // When & Then
            mockMvc.perform(get("/restaurant/reservation/pageable")
                    .param("start", "07-08-2025")
                    .param("end", "14-08-2025")
                    .param("page", "0")
                    .param("size", "10")
                    .with(authentication(mockAuthentication)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.totalPages").value(1));

            verify(reservationService).getReservationsPageable(
                    eq(1L), eq(start), eq(end), any(Pageable.class)
            );
        }
    }

    @Nested
    @DisplayName("Get Pending Reservations Tests")
    class GetPendingReservationsTests {

        @Test
        @DisplayName("Should get pending reservations without date filter")
        void shouldGetPendingReservationsWithoutDateFilter() throws Exception {
            // Given
            reservationResponse.setStatus(Reservation.Status.NOT_ACCEPTED);
            List<ReservationDTO> reservations = Arrays.asList(reservationResponse);
            
            when(reservationService.getPendingReservations(anyLong(), any(), any()))
                    .thenReturn(reservations);

            // When & Then
            mockMvc.perform(get("/restaurant/reservation/pending/get")
                    .with(authentication(mockAuthentication)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].status").value("NOT_ACCEPTED"));

            verify(reservationService).getPendingReservations(1L, null, null);
        }

        @Test
        @DisplayName("Should get pending reservations with date filter")
        void shouldGetPendingReservationsWithDateFilter() throws Exception {
            // Given
            LocalDate start = LocalDate.now();
            LocalDate end = LocalDate.now().plusDays(7);
            reservationResponse.setStatus(Reservation.Status.NOT_ACCEPTED);
            List<ReservationDTO> reservations = Arrays.asList(reservationResponse);
            
            when(reservationService.getPendingReservations(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(reservations);

            // When & Then
            mockMvc.perform(get("/restaurant/reservation/pending/get")
                    .param("start", "07-08-2025")
                    .param("end", "14-08-2025")
                    .with(authentication(mockAuthentication)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());

            verify(reservationService).getPendingReservations(1L, start, end);
        }
    }

    @Nested
    @DisplayName("Accept Modification Request Tests")
    class AcceptModificationRequestTests {

        @Test
        @DisplayName("Should accept modification request successfully")
        void shouldAcceptModificationRequestSuccessfully() throws Exception {
            // Given
            Long modId = 1L;
            doNothing().when(reservationService).AcceptReservatioModifyRequest(modId);

            // When & Then
            mockMvc.perform(put("/restaurant/reservation/accept_modification/{modId}", modId)
                    .with(authentication(mockAuthentication)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(reservationService).AcceptReservatioModifyRequest(modId);
        }
    }
}
