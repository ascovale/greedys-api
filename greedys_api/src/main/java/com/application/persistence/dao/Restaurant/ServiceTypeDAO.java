package com.application.persistence.dao.Restaurant;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.model.reservation.ServiceType;

@Transactional(readOnly = true)
@Repository
public interface ServiceTypeDAO extends JpaRepository<ServiceType, Long> {

    ServiceType findByName(String name);
}