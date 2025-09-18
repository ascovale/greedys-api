package com.application.restaurant.service;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.mapper.RoomMapper;
import com.application.common.persistence.mapper.TableMapper;
import com.application.common.web.dto.restaurant.RoomDTO;
import com.application.common.web.dto.restaurant.TableDTO;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.dao.RoomDAO;
import com.application.restaurant.persistence.model.Room;
import com.application.restaurant.web.dto.restaurant.NewRoomDTO;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RoomService {
    
    private final RoomDAO roomDAO;
    private final RestaurantDAO restaurantDAO;
    private final RoomMapper roomMapper;
    private final TableMapper tableMapper;

    public void deleteById(Long id) {
        roomDAO.deleteById(id);
    }

    public void deleteAll() {
        roomDAO.deleteAll();
    }

    public RoomDTO createRoom(NewRoomDTO roomDto, Long restaurantId) {
        Room room = new Room();
        room.setName(roomDto.getName());
        room.setRestaurant(restaurantDAO.findById(restaurantId).orElseThrow(() -> new IllegalArgumentException("Restaurant not found")));
        return roomMapper.toDTO(roomDAO.save(room));
    }

    public Room findById(Long id) {
        return roomDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + id));
    }
    
    public RoomDTO findByIdAsDTO(Long id) {
        Room room = findById(id);
        return roomMapper.toDTO(room);
    }

    public Collection<RoomDTO> findByRestaurant(Long idRestaurant) {
        return roomDAO.findByRestaurant_Id(idRestaurant).stream()
                .map(roomMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Page<RoomDTO> findByRestaurant(Long idRestaurant, Pageable pageable) {
        return roomDAO.findByRestaurant_Id(idRestaurant, pageable)
                .map(roomMapper::toDTO);
    }

    public void deleteRoom(Long roomId) {
        
        Room room = roomDAO.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found"));
        roomDAO.delete(room);
    }

    public Collection<TableDTO> findTablesByRoomId(Long roomId) {
        Room room = roomDAO.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + roomId));
        return room.getTables().stream()
            .map(tableMapper::toDTO)
            .collect(Collectors.toList());
    }
}
