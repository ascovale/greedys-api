package com.application.persistence.dao.restaurant;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.model.reservation.Slot;

@Transactional(readOnly = true)
@Repository
public interface SlotDAO extends JpaRepository<Slot, Long> {
    public Collection<Slot> findByService_Id(Long idService);
    @Query("SELECT s FROM Slot s WHERE s.service.restaurant.id = :restaurantId AND s.deleted = false")
    List<Slot> findSlotsByRestaurantId(@Param("restaurantId") Long restaurantId);
}