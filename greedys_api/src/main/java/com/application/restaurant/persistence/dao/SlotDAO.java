package com.application.restaurant.persistence.dao;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.Slot;

@Transactional(readOnly = true)
@Repository
public interface SlotDAO extends JpaRepository<Slot, Long> {
    public Collection<Slot> findByService_Id(Long idService);
    
    @Query("SELECT s FROM Slot s WHERE s.service.restaurant.id = :restaurantId AND s.deleted = false")
    List<Slot> findSlotsByRestaurantId(@Param("restaurantId") Long restaurantId);
    
    /**
     * Trova slot attivi per ristorante in una data specifica
     */
    @Query("SELECT s FROM Slot s WHERE s.service.restaurant.id = :restaurantId " +
           "AND s.deleted = false AND s.active = true " +
           "AND s.validFrom <= :date AND s.validTo >= :date")
    List<Slot> findActiveSlotsByRestaurantAndDate(@Param("restaurantId") Long restaurantId, 
                                                  @Param("date") LocalDate date);
    
    /**
     * Trova slot validi per servizio in una data specifica
     */
    @Query("SELECT s FROM Slot s WHERE s.service.id = :serviceId " +
           "AND s.deleted = false AND s.active = true " +
           "AND s.validFrom <= :date AND s.validTo >= :date")
    List<Slot> findActiveSlotsByServiceAndDate(@Param("serviceId") Long serviceId, 
                                               @Param("date") LocalDate date);
                                               
    /**
     * Trova slot che scadono in una data specifica (per cleanup automatico)
     */
    @Query("SELECT s FROM Slot s WHERE s.validTo = :date")
    List<Slot> findSlotsExpiringOnDate(@Param("date") LocalDate date);
}
