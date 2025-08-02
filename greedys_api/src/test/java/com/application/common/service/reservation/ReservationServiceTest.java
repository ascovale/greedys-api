package com.application.common.service.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.customer.persistence.dao.ReservationRequestDAO;
import com.application.restaurant.persistence.dao.ClosedDayDAO;
import com.application.restaurant.persistence.dao.ServiceDAO;
import com.application.restaurant.persistence.dao.SlotDAO;
import com.application.restaurant.persistence.model.Restaurant;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService Unit Tests")
class ReservationServiceTest {

    @Mock private ReservationDAO reservationDAO;
    @Mock private ReservationRequestDAO reservationRequestDAO;
    @Mock private ServiceDAO serviceDAO;
    @Mock private ClosedDayDAO closedDaysDAO;
    @Mock private SlotDAO slotDAO;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReservationService reservationService;

    private Restaurant testRestaurant;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        testRestaurant = new Restaurant();
        testRestaurant.setId(1L);
        testRestaurant.setName("Test Restaurant");

        testReservation = Reservation.builder()
                .id(1L)
                .pax(4)
                .kids(1)
                .date(LocalDate.now().plusDays(1))
                .status(Reservation.Status.ACCEPTED)
                .build();
    }

    @Test
    @DisplayName("Should find closed days successfully")
    void shouldFindClosedDays() {
        // Given
        Long restaurantId = 1L;
        List<LocalDate> expectedClosedDays = Arrays.asList(
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(2)
        );
        when(closedDaysDAO.findUpcomingClosedDay()).thenReturn(expectedClosedDays);

        // When
        List<LocalDate> result = reservationService.findClosedDays(restaurantId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedClosedDays, result);
        verify(closedDaysDAO).findUpcomingClosedDay();
    }

    @Test
    @DisplayName("Should get day reservations successfully")
    void shouldGetDayReservations() {
        // Given
        LocalDate date = LocalDate.now();
        List<Reservation> mockReservations = Arrays.asList(testReservation);
        
        when(reservationDAO.findDayReservation(testRestaurant.getId(), date))
            .thenReturn(mockReservations);

        // When
        List<ReservationDTO> result = reservationService.getDayReservations(testRestaurant, date);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(reservationDAO).findDayReservation(testRestaurant.getId(), date);
    }

    @Test
    @DisplayName("Should create new reservation and publish event")
    void shouldCreateNewReservationAndPublishEvent() {
        // Given
        when(reservationDAO.save(any(Reservation.class))).thenReturn(testReservation);

        // When
        Reservation result = reservationService.createNewReservation(testReservation);

        // Then
        assertNotNull(result);
        assertEquals(testReservation.getId(), result.getId());
        verify(reservationDAO).save(testReservation);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("Should find reservation by id")
    void shouldFindReservationById() {
        // Given
        Long reservationId = 1L;
        when(reservationDAO.findById(reservationId)).thenReturn(Optional.of(testReservation));

        // When
        Reservation result = reservationService.findById(reservationId);

        // Then
        assertNotNull(result);
        assertEquals(testReservation.getId(), result.getId());
        verify(reservationDAO).findById(reservationId);
    }

    @Test
    @DisplayName("Should return null when reservation not found")
    void shouldReturnNullWhenReservationNotFound() {
        // Given
        Long reservationId = 999L;
        when(reservationDAO.findById(reservationId)).thenReturn(Optional.empty());

        // When
        Reservation result = reservationService.findById(reservationId);

        // Then
        assertNull(result);
        verify(reservationDAO).findById(reservationId);
    }
}
