package com.application.restaurant.persistence.dao.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.restaurant.persistence.model.menu.Brand;

@Repository
public interface BrandDAO extends JpaRepository<Brand, Long> {
    // Additional query methods if needed
}
