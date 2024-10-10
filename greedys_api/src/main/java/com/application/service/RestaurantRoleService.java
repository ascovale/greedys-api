package com.application.service;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.application.persistence.dao.restaurant.RestaurantRoleDAO;
import com.application.persistence.model.restaurant.RestaurantRole;

@Service
public class RestaurantRoleService {
    
    @Autowired
    private RestaurantRoleDAO roleDAO;

    public RestaurantRole findById(Long id) {
        return roleDAO.findById(id).orElseThrow(() -> new EntityNotFoundException("RestaurantRole not found with id: " + id));
    }
}
