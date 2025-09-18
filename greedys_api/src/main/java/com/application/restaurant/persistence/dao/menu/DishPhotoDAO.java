package com.application.restaurant.persistence.dao.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.restaurant.persistence.model.menu.DishPhoto;

@Repository
public interface DishPhotoDAO extends JpaRepository<DishPhoto, Long> {
    // Additional query methods if needed
}
