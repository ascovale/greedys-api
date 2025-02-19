package com.application.persistence.dao.restaurant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.restaurant.user.RestaurantRole;

@Repository
public interface RestaurantRoleDAO extends JpaRepository<RestaurantRole, Long>{

    public RestaurantRole findByName(String name);
}