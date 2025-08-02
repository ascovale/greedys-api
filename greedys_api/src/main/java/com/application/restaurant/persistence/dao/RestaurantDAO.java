package com.application.restaurant.persistence.dao;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.Service;
import com.application.common.persistence.model.reservation.Slot;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.model.RestaurantImage;

@Transactional(readOnly = true)
@Repository
public interface RestaurantDAO extends JpaRepository<Restaurant, Long> {

  @Query(value =  """
                    SELECT cd.closedDate 
                    FROM ClosedDay cd 
                    WHERE cd.restaurant.id = :idrestaurant 
                      AND cd.closedDate BETWEEN :start AND :end 

                    UNION 

                    SELECT cs.closedDate 
                      FROM ClosedSlot cs 
                      JOIN cs.slot s 
                      JOIN s.service sv 
                      WHERE sv.restaurant.id = :idrestaurant 
                        AND cs.closedDate BETWEEN :start AND :end 
                      GROUP BY cs.closedDate 
                      HAVING COUNT(cs.id) = (SELECT COUNT(s2.id) 
                                           FROM Slot s2 
                                           WHERE cs.slot.service = s2.service.id 
                                           AND cs.slot.weekday = s2.weekday) 
                    """, 
          nativeQuery = true)
  public Collection<LocalDate> findClosedDays(@Param("idrestaurant") Long idrestaurant, @Param("start") LocalDate start, @Param("end") LocalDate end);

  @Query(value = """
        WITH RECURSIVE DateRange AS (
            SELECT :startDate AS date
            UNION ALL
            SELECT DATE_ADD(date, INTERVAL 1 DAY)
            FROM DateRange
            WHERE DATE_ADD(date, INTERVAL 1 DAY) <= :endDate
        )
        SELECT dr.date as available_date
        FROM DateRange dr
        WHERE dr.date NOT IN (
            SELECT cd.closedDate
            FROM closed_day cd
            WHERE cd.restaurant_id = :restaurantId
          )
          AND EXISTS (
              SELECT 1
              FROM service s
              JOIN slot sl ON s.id = sl.service_id
              WHERE s.restaurant_id = :restaurantId
                AND sl.weekday = DAYNAME(dr.date)
                AND NOT EXISTS (
                    SELECT 1
                    FROM closed_slot cs
                    WHERE cs.slot_id = sl.id
                      AND cs.date = dr.date
                )
        )
        ORDER BY dr.date
        """, nativeQuery = true)
    Collection<?> findOpenDaysInRange(@Param("restaurantId") Long restaurantId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);


  public Restaurant findByName(String restaurant);

  public Restaurant findByEmail(String restaurant);

  @Query(value = "SELECT i.id,i.name,u.id FROM Restaurant r, RestaurantImage i WHERE "
      + "r.email=:email and r.id=i.restaurant_id", nativeQuery = true)
  public List<?> findImages(@Param("email") String email);

  // public Page<Restaurant> findBySearchTermNamed(@Param("searchTerm") String
  // searchTerm, Pageable pageRequest);
  // public List<Restaurant> findBySearchTermNamedNative(@Param("searchTerm")
  // String searchTerm);
  @Query("SELECT r FROM Restaurant r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%',:searchTerm, '%'))")
  public Collection<Restaurant> findBySearchTerm(@Param("searchTerm") String searchTerm);

  @Query("SELECT r FROM Restaurant r JOIN FETCH r.restaurantImages WHERE r.id = (:id)")
  public Restaurant findByIdFullDetailsRestaurant(@Param("id") Long id);

  @Query("SELECT ri FROM RestaurantImage ri WHERE ri.restaurant = (:id)")
  public Page<RestaurantImage> getRestaurantImages(@Param("id") Long id, Pageable pageRequest);
  // @Query("SELECT ri FROM RestaurantReservation ri WHERE ri.restaurant = (:id)")
  // public Page<CompanyReservation> getCompanyReservation(@Param("id") Long id,
  // Pageable pageRequest);

  @Query("""
    SELECT s FROM Slot s 
      WHERE 
        s NOT IN (
          SELECT cs.slot 
          FROM ClosedSlot cs 
          WHERE cs.date = :date
        ) AND
        s.service.restaurant.id = :id AND
        s.weekday = FUNCTION('DAYNAME', :date)
    """)
  public Collection<Slot> finDaySlots(Long id, LocalDate date);


  @Query("SELECT s FROM Service s WHERE s.restaurant.id = :id")
  public Collection<Service> findServices(Long id);

}
