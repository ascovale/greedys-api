package com.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.restaurant.Restaurant;
import com.application.service.RestaurantService;

@RestController
public class SearchController {

    @Autowired
    private RestaurantService restaurantService;

    //TODO implementare la ricerca nel sistema
/* 
    @GetMapping("/search")
    public Page<Restaurant> searchRestaurants(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String dishType,
            Pageable pageable) {
        return restaurantService.searchRestaurants(query, category, dishType, pageable);
    }*/
}
