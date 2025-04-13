package com.application.persistence.dao.menu;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.menu.Menu;

@Repository
public interface MenuDAO extends JpaRepository<Menu, Long> {
    @Query("SELECT m FROM Menu m JOIN m.services s JOIN s.restaurant r WHERE r.id = :restaurantId")
    List<Menu> findByRestaurantId(@Param("restaurantId") Long restaurantId);
}
