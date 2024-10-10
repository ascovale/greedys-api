package com.application.persistence.dao.restaurant;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.model.reservation.Slot;

@Transactional(readOnly = true)
@Repository
public interface SlotDAO extends JpaRepository<Slot, Long> {
    public Collection<Slot> findByService_Id(Long idService);
}