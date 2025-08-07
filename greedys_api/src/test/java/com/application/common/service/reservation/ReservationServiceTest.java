package com.application.common.service.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
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
import com.application.common.persistence.model.reservation.Service;
import com.application.common.persistence.model.reservation.Slot;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.customer.persistence.dao.ReservationRequestDAO;
import com.application.customer.persistence.model.Customer;
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
    private Service testService;
    private Slot testSlot;
    private Customer testCustomer;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        // Setup Restaurant
        testRestaurant = new Restaurant();
        testRestaurant.setId(1L);
        testRestaurant.setName("Test Restaurant");

        // Setup Service
        testService = new Service();
        testService.setId(1L);
        testService.setName("Test Service");
        testService.setRestaurant(testRestaurant);

        // Setup Slot
        testSlot = new Slot();
        testSlot.setId(1L);
        testSlot.setService(testService);

        // Setup Customer
        testCustomer = new Customer();
        testCustomer.setId(1L);
        testCustomer.setEmail("test@example.com");
        testCustomer.setName("Test Customer");

        // Setup Reservation with all required relationships
        testReservation = Reservation.builder()
                .id(1L)
                .pax(4)
                .kids(1)
                .date(LocalDate.now().plusDays(1))
                .status(Reservation.Status.ACCEPTED)
                .customer(testCustomer)
                .slot(testSlot)
                .restaurant(testRestaurant)
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
    @DisplayName("Should return empty list when no closed days found")
    void shouldReturnEmptyListWhenNoClosedDaysFound() {
        // Given
        Long restaurantId = 1L;
        when(closedDaysDAO.findUpcomingClosedDay()).thenReturn(Arrays.asList());

        // When
        List<LocalDate> result = reservationService.findClosedDays(restaurantId);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
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
    @DisplayName("Should return empty list when no reservations found for date")
    void shouldReturnEmptyListWhenNoReservationsFoundForDate() {
        // Given
        LocalDate date = LocalDate.now();
        when(reservationDAO.findDayReservation(testRestaurant.getId(), date))
            .thenReturn(Arrays.asList());

        // When
        List<ReservationDTO> result = reservationService.getDayReservations(testRestaurant, date);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(reservationDAO).findDayReservation(testRestaurant.getId(), date);
    }

    @Test
    @DisplayName("Should create new reservation and publish event")
    void shouldCreateNewReservationAndPublishEvent() {
        // Given
        when(reservationDAO.save(any(Reservation.class))).thenReturn(testReservation);
        doNothing().when(eventPublisher).publishEvent(any());

        // When
        Reservation result = reservationService.createNewReservation(testReservation);

        // Then
        assertNotNull(result);
        assertEquals(testReservation.getId(), result.getId());
        verify(reservationDAO).save(testReservation);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("Should create new reservation without customer and not publish event")
    void shouldCreateNewReservationWithoutCustomerAndNotPublishEvent() {
        // Given - Create reservation without customer
        Reservation reservationWithoutCustomer = Reservation.builder()
                .id(2L)
                .pax(2)
                .kids(0)
                .date(LocalDate.now().plusDays(1))
                .status(Reservation.Status.ACCEPTED)
                .slot(testSlot)
                .restaurant(testRestaurant)
                .build();

        when(reservationDAO.save(any(Reservation.class))).thenReturn(reservationWithoutCustomer);

        // When & Then - Should throw exception when trying to publish event
        assertThrows(NullPointerException.class, () -> {
            reservationService.createNewReservation(reservationWithoutCustomer);
        });

        verify(reservationDAO).save(reservationWithoutCustomer);
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
        assertEquals(testReservation.getPax(), result.getPax());
        assertEquals(testReservation.getKids(), result.getKids());
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

    @Test
    @DisplayName("Should save reservation successfully")
    void shouldSaveReservationSuccessfully() {
        // Given
        when(reservationDAO.save(testReservation)).thenReturn(testReservation);

        // When
        reservationService.save(testReservation);

        // Then
        verify(reservationDAO).save(testReservation);
    }

    @Test
    @DisplayName("Should set reservation status successfully")
    void shouldSetReservationStatusSuccessfully() {
        // Given
        Long reservationId = 1L;
        Reservation.Status newStatus = Reservation.Status.REJECTED;
        
        when(reservationDAO.findById(reservationId)).thenReturn(Optional.of(testReservation));
        when(reservationDAO.save(any(Reservation.class))).thenReturn(testReservation);

        // When
        reservationService.setStatus(reservationId, newStatus);

        // Then
        verify(reservationDAO).findById(reservationId);
        verify(reservationDAO).save(testReservation);
        assertEquals(newStatus, testReservation.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when setting status on non-existent reservation")
    void shouldThrowExceptionWhenSettingStatusOnNonExistentReservation() {
        // Given
        Long reservationId = 999L;
        Reservation.Status newStatus = Reservation.Status.REJECTED;
        
        when(reservationDAO.findById(reservationId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(java.util.NoSuchElementException.class, () -> {
            reservationService.setStatus(reservationId, newStatus);
        });

        verify(reservationDAO).findById(reservationId);
        verify(reservationDAO, never()).save(any());
    }

    @Test
    @DisplayName("Should find not available days successfully")
    void shouldFindNotAvailableDaysSuccessfully() {
        // Given
        Long restaurantId = 1L;
        List<LocalDate> expectedDays = Arrays.asList(
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(3)
        );
        when(serviceDAO.findClosedOrFullDays(restaurantId)).thenReturn(expectedDays);

        // When
        List<LocalDate> result = reservationService.findNotAvailableDays(restaurantId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedDays, result);
        verify(serviceDAO).findClosedOrFullDays(restaurantId);
    }

    @Test
    @DisplayName("Should get reservations by restaurant and date range")
    void shouldGetReservationsByRestaurantAndDateRange() {
        // Given
        Long restaurantId = 1L;
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(7);
        List<Reservation> expectedReservations = Arrays.asList(testReservation);
        
        when(reservationDAO.findByRestaurantAndDateBetween(restaurantId, startDate, endDate))
            .thenReturn(expectedReservations);

        // When
        var result = reservationService.getReservations(restaurantId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(reservationDAO).findByRestaurantAndDateBetween(restaurantId, startDate, endDate);
    }

    @Test
    @DisplayName("Should get accepted reservations by restaurant and date range")
    void shouldGetAcceptedReservationsByRestaurantAndDateRange() {
        // Given
        Long restaurantId = 1L;
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(7);
        List<Reservation> expectedReservations = Arrays.asList(testReservation);
        
        when(reservationDAO.findByRestaurantAndDateBetweenAndStatus(
            restaurantId, startDate, endDate, Reservation.Status.ACCEPTED))
            .thenReturn(expectedReservations);

        // When
        var result = reservationService.getAcceptedReservations(restaurantId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(reservationDAO).findByRestaurantAndDateBetweenAndStatus(
            restaurantId, startDate, endDate, Reservation.Status.ACCEPTED);
    }
}
