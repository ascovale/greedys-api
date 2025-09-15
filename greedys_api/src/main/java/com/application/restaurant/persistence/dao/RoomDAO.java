package com.application.restaurant.persistence.dao;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.application.restaurant.persistence.model.Room;

@Repository
public interface RoomDAO extends JpaRepository<Room, Long> {
    
    @Query("SELECT r FROM Room r WHERE r.restaurant.id = ?1")
    public Collection<Room> findByRestaurant_Id(Long idRestaurant);
    
    @Query("SELECT r FROM Room r WHERE r.restaurant.id = ?1")
    public Page<Room> findByRestaurant_Id(Long idRestaurant, Pageable pageable);

}
