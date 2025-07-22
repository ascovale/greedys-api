package com.application.restaurant.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.restaurant.dao.RestaurantRoleDAO;
import com.application.restaurant.model.user.RestaurantRole;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RestaurantRoleService {
    
    private final RestaurantRoleDAO roleDAO;

    public RestaurantRole findById(Long id) {
        return roleDAO.findById(id).orElseThrow(() -> new EntityNotFoundException("RestaurantRole not found with id: " + id));
    }
}
