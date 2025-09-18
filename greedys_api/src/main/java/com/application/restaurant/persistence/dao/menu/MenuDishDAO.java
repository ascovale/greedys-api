package com.application.restaurant.persistence.dao.menu;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.restaurant.persistence.model.menu.MenuDish;

@Repository
public interface MenuDishDAO extends JpaRepository<MenuDish, Long> {
    // Additional query methods if needed
    void deleteById(Long id);
    List<MenuDish> findByMenuId(Long menuId);

}
