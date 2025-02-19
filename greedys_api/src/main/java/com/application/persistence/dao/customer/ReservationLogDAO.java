package com.application.persistence.dao.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.reservation.ReservationLog;



@Repository
public interface ReservationLogDAO extends JpaRepository<ReservationLog, Long> {
	
}
