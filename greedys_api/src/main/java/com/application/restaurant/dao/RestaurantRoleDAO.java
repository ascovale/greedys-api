package com.application.restaurant.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.restaurant.model.user.RestaurantRole;

@Repository
public interface RestaurantRoleDAO extends JpaRepository<RestaurantRole, Long>{

    public RestaurantRole findByName(String name);
}