package com.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.application.persistence.dao.Restaurant.RoomDAO;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.Room;
import com.application.web.dto.post.NewRoomDTO;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class RoomService {
    
    @Autowired
    private RoomDAO roomDAO;
    @Autowired
    EntityManager entityManager;

    public void deleteById(Long id) {
        roomDAO.deleteById(id);
    }

    public void deleteAll() {
        roomDAO.deleteAll();
    }

    public void createRoom(NewRoomDTO roomDto) {
        Room room = new Room();
        room.setName(roomDto.getName());
        room.setRestaurant(entityManager.getReference(Restaurant.class, roomDto.getRestaurantId()));
        roomDAO.save(room);
    }

    public Room findById(Long id) {
        return roomDAO.findById(id).get();
    }

    public Iterable<Room> findByRestaurant(Long idRestaurant) {
        return roomDAO.findByRestaurant_Id(idRestaurant);
    }
}
