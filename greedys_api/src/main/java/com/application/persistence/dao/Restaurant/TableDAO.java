package com.application.persistence.dao.Restaurant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.restaurant.Table;

import java.util.Collection;

@Repository
public interface TableDAO extends JpaRepository<Table, Long> {
    
    public Collection<Table> findByRoom_Id(Long idRoom);
}