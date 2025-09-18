package com.application.customer.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.reservation.ReservationRequest;



@Repository
public interface ReservationRequestDAO extends JpaRepository<ReservationRequest, Long> {
}
