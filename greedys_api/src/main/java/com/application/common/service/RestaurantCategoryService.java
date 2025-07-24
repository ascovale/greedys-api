package com.application.common.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.web.dto.RestaurantCategoryDTO;
import com.application.restaurant.persistence.dao.RestaurantCategoryDAO;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.model.RestaurantCategory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class for managing restaurant categories in the admin context.
 * This service handles category-specific operations with direct DAO access.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RestaurantCategoryService {
    
    private final RestaurantDAO restaurantDAO;
    private final RestaurantCategoryDAO restaurantCategoryDAO;

    /**
     * Retrieves the names of all restaurant types/categories for a specific restaurant.
     * 
     * @param restaurantId the ID of the restaurant
     * @return a list of category names
     * @throws IllegalArgumentException if the restaurant ID is invalid
     */
    public List<String> getRestaurantTypesNames(Long restaurantId) {
        log.debug("Getting restaurant types names for restaurant ID: {}", restaurantId);
        Restaurant restaurant = restaurantDAO.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID"));
        return restaurant.getRestaurantTypes().stream()
                .map(RestaurantCategory::getName)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new restaurant category.
     * 
     * @param restaurantCategoryDto the category data transfer object containing category details
     */
    public void createRestaurantCategory(RestaurantCategoryDTO restaurantCategoryDto) {
        log.debug("Creating new restaurant category: {}", restaurantCategoryDto.getName());
        RestaurantCategory restaurantCategory = new RestaurantCategory();
        restaurantCategory.setName(restaurantCategoryDto.getName());
        restaurantCategory.setDescription(restaurantCategoryDto.getDescription());
        restaurantCategoryDAO.save(restaurantCategory);
    }

    /**
     * Updates an existing restaurant category.
     * 
     * @param categoryId the ID of the category to update
     * @param restaurantCategoryDto the updated category data
     * @throws IllegalArgumentException if the category ID is invalid
     */
    public void updateRestaurantCategory(Long categoryId, RestaurantCategoryDTO restaurantCategoryDto) {
        log.debug("Updating restaurant category ID: {} with data: {}", categoryId, restaurantCategoryDto.getName());
        RestaurantCategory restaurantCategory = restaurantCategoryDAO.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category ID"));
        restaurantCategory.setName(restaurantCategoryDto.getName());
        restaurantCategory.setDescription(restaurantCategoryDto.getDescription());
        restaurantCategoryDAO.save(restaurantCategory);
    }

    /**
     * Deletes a restaurant category by its ID.
     * 
     * @param categoryId the ID of the category to delete
     * @throws IllegalArgumentException if the category ID is invalid
     */
    public void deleteRestaurantCategory(Long categoryId) {
        log.debug("Deleting restaurant category ID: {}", categoryId);
        RestaurantCategory restaurantCategory = restaurantCategoryDAO.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category ID"));
        restaurantCategoryDAO.delete(restaurantCategory);
    }
}
