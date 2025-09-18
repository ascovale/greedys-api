package com.application.restaurant.persistence.dao;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.restaurant.persistence.model.Table;

@Repository
public interface TableDAO extends JpaRepository<Table, Long> {
    
    public Collection<Table> findByRoom_Id(Long idRoom);
    
    public Page<Table> findByRoom_Id(Long idRoom, Pageable pageable);
}
