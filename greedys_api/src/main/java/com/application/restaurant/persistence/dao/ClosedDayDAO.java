package com.application.restaurant.persistence.dao;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.restaurant.persistence.model.ClosedDay;

@Transactional(readOnly = true) 
@Repository
public interface ClosedDayDAO extends JpaRepository<ClosedDay, Long> {

    @Query(value = "SELECT day FROM ClosedDay cd WHERE cd.day >= CURDATE()", nativeQuery = true)
    List<LocalDate> findUpcomingClosedDay();
}
