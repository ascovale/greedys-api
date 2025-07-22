package com.application.admin.service;

import java.util.Collection;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.web.dto.RestaurantCategoryDTO;
import com.application.common.web.dto.get.RestaurantDTO;
import com.application.common.web.dto.get.ServiceDTO;
import com.application.restaurant.service.RestaurantService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminRestaurantService {
    
    private final RestaurantService restaurantService;

    public Collection<ServiceDTO> getRestaurantServices(Long restaurantId) {
        return restaurantService.getServices(restaurantId);
    }

    public void setNoShowTimeLimit(Long restaurantId, int minutes) {
        restaurantService.setNoShowTimeLimit(restaurantId, minutes);
    }

    public void createRestaurantCategory(RestaurantCategoryDTO categoryDto) {
        restaurantService.createRestaurantCategory(categoryDto);
    }

    public void deleteRestaurantCategory(Long categoryId) {
        restaurantService.deleteRestaurantCategory(categoryId);
    }

    public void updateRestaurantCategory(Long categoryId, RestaurantCategoryDTO categoryDto) {
        restaurantService.updateRestaurantCategory(categoryId, categoryDto);
    }

    public void enableRestaurant(Long restaurantId) {
        restaurantService.enableRestaurant(restaurantId);
    }

    public void createRestaurant(RestaurantDTO restaurantDto) {
        restaurantService.createRestaurant(restaurantDto);
    }

    public void changeRestaurantEmail(Long restaurantId, String newEmail) {
        restaurantService.changeRestaurantEmail(restaurantId, newEmail);
    }

    public void setRestaurantDeleted(Long restaurantId, boolean deleted) {
        restaurantService.setRestaurantDeleted(restaurantId, deleted);
    }
}
