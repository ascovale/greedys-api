package com.application.restaurant.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.restaurant.dao.RestaurantRoleDAO;
import com.application.restaurant.model.user.RestaurantRole;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class RestaurantRoleService {
    
    @Autowired
    private RestaurantRoleDAO roleDAO;

    public RestaurantRole findById(Long id) {
        return roleDAO.findById(id).orElseThrow(() -> new EntityNotFoundException("RestaurantRole not found with id: " + id));
    }
}
