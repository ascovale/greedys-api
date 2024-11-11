package com.application.persistence.dao.menu;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.application.persistence.model.menu.MenuItem;

public interface MenuItemDAO extends JpaRepository<MenuItem, Long> {

  
    Optional<MenuItem> findById(Long id);

    @Query("""
            SELECT i
            FROM MenuItem i
            WHERE i.restaurant.id = :id
            """)
    Collection<MenuItem> findByRestaurant(Long id);
    
}
