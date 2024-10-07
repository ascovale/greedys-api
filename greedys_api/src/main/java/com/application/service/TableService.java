package com.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.application.persistence.dao.Restaurant.TableDAO;
import com.application.persistence.model.restaurant.Table;
import com.application.web.dto.post.NewTableDTO;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class TableService {
    
    @Autowired
    private TableDAO tableDAO;

    public void deleteById(Long id) {
        tableDAO.deleteById(id);
    }

    public void deleteAll() {
        tableDAO.deleteAll();
    }

    public void createTable(NewTableDTO tableDto) {
        Table table = new Table();
        table.setName(tableDto.getName());
        tableDAO.save(table);
    }

    public Table findById(Long id) {
        return tableDAO.findById(id).get();
    }

    public Iterable<Table> findByRoom(Long idRoom) {
        return tableDAO.findByRoom_Id(idRoom);
    }

}
