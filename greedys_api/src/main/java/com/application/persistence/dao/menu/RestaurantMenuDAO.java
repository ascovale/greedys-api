package com.application.persistence.dao.menu;

import java.util.Optional;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.application.persistence.model.menu.RestaurantMenu;
import com.application.web.dto.get.PricedMenuItemDTO;

public interface RestaurantMenuDAO extends JpaRepository<RestaurantMenu, Long> {

    Optional<RestaurantMenu> findById(Long id);


    

    @Query("""
            SELECT m 
            FROM RestaurantMenu m
            WHERE m.restaurant.id = :id
            """)
    Collection<RestaurantMenu> findByRestaurantId(Long id);
}
