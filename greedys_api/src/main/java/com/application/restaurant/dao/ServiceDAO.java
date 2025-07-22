package com.application.restaurant.dao;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.Service;

@Transactional(readOnly = true)
@Repository
public interface ServiceDAO extends JpaRepository<Service, Long> {
	public Optional<Service> findById(Long id);

	@Query(value = "SELECT s.day " +
			"FROM service s " +
			"LEFT JOIN service_closed sc ON s.id = sc.idservice AND s.idrestaurant = :idrestaurant " +
			"GROUP BY s.day " +
			"HAVING COUNT(s.id) = COUNT(sc.idservice) " +
			"AND s.day >= CURDATE()", nativeQuery = true)
	List<LocalDate> findClosedOrFullDays(@Param("idrestaurant") Long idrestaurant);

	@Query(value = "SELECT day " +
			"FROM closed_day " +
			"WHERE idrestaurant = :idrestaurant " +
			"AND day >= CURDATE()", nativeQuery = true)
	List<LocalDate> findClosedDays(@Param("idrestaurant") Long idrestaurant);

	@Query(value = """
			SELECT *
			FROM service s
			WHERE s.idrestaurant = :idrestaurant """, nativeQuery = true)
	List<Service> findServicesByRestaurant(@Param("idrestaurant") Long idrestaurant);

	@Query("""
            SELECT s FROM Service s
            WHERE s.restaurant.id = :restaurantId
              AND s.enabled = true
              AND s.active = true
              AND (:date IS NULL OR (s.validFrom <= :date AND (s.validTo IS NULL OR s.validTo >= :date)))
        """)
    Collection<Service> findActiveEnabledServices(@Param("restaurantId") Long restaurantId,
            @Param("date") LocalDate date);

	@Query("""
		SELECT s FROM Service s
		WHERE s.restaurant.id = :restaurantId
		  AND s.enabled = true
		  AND s.active = true
		  AND (
			(:startDate IS NULL OR s.validTo IS NULL OR s.validTo >= :startDate)
			AND (:endDate IS NULL OR s.validFrom <= :endDate)
		  )
	""")
	Collection<Service> findActiveEnabledServicesInPeriod(
		@Param("restaurantId") Long restaurantId,
		@Param("startDate") LocalDate startDate,
		@Param("endDate") LocalDate endDate
	);

}
