package com.application.persistence.dao.restaurant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.restaurant.Room;

import java.util.Collection;

@Repository
public interface RoomDAO extends JpaRepository<Room, Long> {
    
    @Query("SELECT r FROM Room r WHERE r.restaurant.id = ?1")
    public Collection<Room> findByRestaurant_Id(Long idRestaurant);

}