package com.application.service;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.application.persistence.dao.restaurant.TableDAO;
import com.application.persistence.model.restaurant.Room;
import com.application.persistence.model.restaurant.Table;
import com.application.web.dto.get.TableDTO;
import com.application.web.dto.post.NewTableDTO;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class TableService {
    
    private final TableDAO tableDAO;

    private final EntityManager entityManager;
    
    public TableService(TableDAO tableDAO, EntityManager entityManager) {
        this.tableDAO = tableDAO;
        this.entityManager = entityManager;
    }

    public void deleteById(Long id) {
        tableDAO.deleteById(id);
    }

    public void deleteAll() {
        tableDAO.deleteAll();
    }

    public void createTable(NewTableDTO tableDto) {
        Table table = new Table();
        table.setName(tableDto.getName());
        table.setCapacity(tableDto.getCapacity());
        table.setPositionX(tableDto.getPositionX());
        table.setPositionY(tableDto.getPositionY());
        table.setRoom(entityManager.getReference(Room.class, tableDto.getRoomId()));
        tableDAO.save(table);
    }

    public Table findById(Long id) {
        return tableDAO.findById(id).get();
    }

    public Collection<TableDTO> findByRoom(Long idRoom) {
        return tableDAO.findByRoom_Id(idRoom).stream().map(table -> new TableDTO(table) ).collect(Collectors.toList());
    }

}
