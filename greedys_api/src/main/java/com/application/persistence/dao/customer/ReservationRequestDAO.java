package com.application.persistence.dao.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.reservation.ReservationRequest;



@Repository
public interface ReservationRequestDAO extends JpaRepository<ReservationRequest, Long> {
}
