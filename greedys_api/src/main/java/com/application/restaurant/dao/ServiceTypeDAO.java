package com.application.restaurant.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.ServiceType;

@Transactional(readOnly = true)
@Repository
public interface ServiceTypeDAO extends JpaRepository<ServiceType, Long> {

    ServiceType findByName(String name);
}