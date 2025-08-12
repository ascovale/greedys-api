package com.application.restaurant.service;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.mapper.TableMapper;
import com.application.common.web.dto.restaurant.TableDTO;
import com.application.restaurant.persistence.dao.RoomDAO;
import com.application.restaurant.persistence.dao.TableDAO;
import com.application.restaurant.persistence.model.Table;
import com.application.restaurant.web.dto.restaurant.NewTableDTO;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class TableService {
    
    private final TableDAO tableDAO;
    private final RoomDAO roomDAO;
    private final TableMapper tableMapper;

    public void deleteById(Long id) {
        tableDAO.deleteById(id);
    }

    public void deleteAll() {
        tableDAO.deleteAll();
    }

    public TableDTO createTable(NewTableDTO tableDto) {
        Table table = new Table();
        table.setName(tableDto.getName());
        table.setCapacity(tableDto.getCapacity());
        table.setPositionX(tableDto.getPositionX());
        table.setPositionY(tableDto.getPositionY());
        table.setRoom(roomDAO.findById(tableDto.getRoomId()).orElseThrow(() -> new IllegalArgumentException("Room not found")));
        Table savedTable = tableDAO.save(table);
        return tableMapper.toDTO(savedTable);
    }

    public Table findById(Long id) {
        return tableDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Table not found with id: " + id));
    }
    
    public TableDTO findByIdAsDTO(Long id) {
        Table table = findById(id);
        return tableMapper.toDTO(table);
    }

    public Collection<TableDTO> findByRoom(Long idRoom) {
        return tableDAO.findByRoom_Id(idRoom).stream()
                .map(tableMapper::toDTO)
                .collect(Collectors.toList());
    }

    public void deleteTable(Long tableId) {
        Table table = tableDAO.findById(tableId).orElseThrow(() -> new IllegalArgumentException("Table not found with id: " + tableId));
        tableDAO.delete(table);
    }

}
