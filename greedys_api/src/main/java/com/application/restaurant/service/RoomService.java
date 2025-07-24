package com.application.restaurant.service;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.application.common.web.dto.get.RoomDTO;
import com.application.restaurant.persistence.dao.RoomDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.model.Room;
import com.application.restaurant.web.dto.post.NewRoomDTO;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RoomService {
    
    private final RoomDAO roomDAO;
    private final EntityManager entityManager;

    public void deleteById(Long id) {
        roomDAO.deleteById(id);
    }

    public void deleteAll() {
        roomDAO.deleteAll();
    }

    public Room createRoom(NewRoomDTO roomDto) {
        Room room = new Room();
        room.setName(roomDto.getName());
        room.setRestaurant(entityManager.getReference(Restaurant.class, roomDto.getRestaurantId()));
        return roomDAO.save(room);
    }

    public Room findById(Long id) {
        return roomDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + id));
    }

    public Collection<RoomDTO> findByRestaurant(Long idRestaurant) {
        return roomDAO.findByRestaurant_Id(idRestaurant).stream().map(room -> new RoomDTO(room) ).collect(Collectors.toList());
    }

    public void deleteRoom(Long roomId) {
        
        Room room = roomDAO.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found"));
        roomDAO.delete(room);
    }
}
