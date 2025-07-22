package com.application.customer.service;

import java.time.LocalDate;
import java.util.Collection;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.web.dto.get.MenuDTO;
import com.application.common.web.dto.get.RestaurantDTO;
import com.application.common.web.dto.get.RoomDTO;
import com.application.common.web.dto.get.ServiceDTO;
import com.application.common.web.dto.get.SlotDTO;
import com.application.common.web.dto.get.TableDTO;
import com.application.restaurant.service.RestaurantMenuService;
import com.application.restaurant.service.RestaurantService;
import com.application.restaurant.service.RoomService;
import com.application.restaurant.service.SlotService;
import com.application.restaurant.service.TableService;

import lombok.RequiredArgsConstructor;

/**
 * Service dedicato alle operazioni customer sui ristoranti.
 * Astrae e delega le chiamate ai service restaurant.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CustomerRestaurantService {
    
    private final RestaurantService restaurantService;
    private final RestaurantMenuService restaurantMenuService;
    private final RoomService roomService;
    private final TableService tableService;
    private final SlotService slotService;

    // Metodi read-only per i customer sui ristoranti
    public Collection<RestaurantDTO> getAllRestaurants() {
        return restaurantService.findAll().stream()
            .map(RestaurantDTO::new)
            .toList();
    }

    public Collection<String> getOpenDays(Long restaurantId, LocalDate start, LocalDate end) {
        return restaurantService.getOpenDays(restaurantId, start, end);
    }

    public Collection<LocalDate> getClosedDays(Long restaurantId, LocalDate start, LocalDate end) {
        return restaurantService.getClosedDays(restaurantId, start, end);
    }

    public Collection<SlotDTO> getDaySlots(Long restaurantId, LocalDate date) {
        return restaurantService.getDaySlots(restaurantId, date);
    }

    public Collection<RestaurantDTO> searchRestaurants(String searchTerm) {
        return restaurantService.findBySearchTerm(searchTerm);
    }

    public Collection<ServiceDTO> getActiveEnabledServices(Long restaurantId, LocalDate date) {
        return restaurantService.getActiveEnabledServices(restaurantId, date);
    }

    public Collection<ServiceDTO> getActiveEnabledServicesInPeriod(Long restaurantId, LocalDate start, LocalDate end) {
        return restaurantService.findActiveEnabledServicesInPeriod(restaurantId, start, end);
    }

    // Metodi per i menu
    public Collection<MenuDTO> getMenusByRestaurant(Long restaurantId) {
        return restaurantMenuService.getMenusByRestaurant(restaurantId);
    }

    public Collection<MenuDTO> getMenusWithServicesValidInPeriod(Long restaurantId, LocalDate startDate, LocalDate endDate) {
        return restaurantMenuService.getMenusWithServicesValidInPeriod(restaurantId, startDate, endDate);
    }

    public Collection<MenuDTO> getActiveEnabledMenusByServiceId(Long serviceId, LocalDate date) {
        return restaurantMenuService.getActiveEnabledMenusByServiceId(serviceId, date);
    }

    // Metodi per rooms e tables (read-only)
    public Collection<RoomDTO> getRoomsByRestaurant(Long restaurantId) {
        return roomService.findByRestaurant(restaurantId);
    }

    public Collection<TableDTO> getTablesByRoom(Long roomId) {
        return tableService.findByRoom(roomId);
    }
}
