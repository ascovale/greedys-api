package com.application.restaurant.service;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.application.common.web.dto.get.TableDTO;
import com.application.restaurant.persistence.dao.TableDAO;
import com.application.restaurant.persistence.model.Room;
import com.application.restaurant.persistence.model.Table;
import com.application.restaurant.web.dto.post.NewTableDTO;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class TableService {
    
    private final TableDAO tableDAO;
    private final EntityManager entityManager;

    public void deleteById(Long id) {
        tableDAO.deleteById(id);
    }

    public void deleteAll() {
        tableDAO.deleteAll();
    }

    public Table createTable(NewTableDTO tableDto) {
        Table table = new Table();
        table.setName(tableDto.getName());
        table.setCapacity(tableDto.getCapacity());
        table.setPositionX(tableDto.getPositionX());
        table.setPositionY(tableDto.getPositionY());
        table.setRoom(entityManager.getReference(Room.class, tableDto.getRoomId()));
        return tableDAO.save(table);
    }

    public Table findById(Long id) {
        return tableDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Table not found with id: " + id));
    }

    public Collection<TableDTO> findByRoom(Long idRoom) {
        return tableDAO.findByRoom_Id(idRoom).stream().map(table -> new TableDTO(table) ).collect(Collectors.toList());
    }

    public void deleteTable(Long tableId) {
        Table table = tableDAO.findById(tableId).orElseThrow(() -> new IllegalArgumentException("Table not found with id: " + tableId));
        tableDAO.delete(table);
    }

}
