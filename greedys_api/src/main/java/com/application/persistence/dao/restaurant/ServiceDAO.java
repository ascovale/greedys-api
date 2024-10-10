package com.application.persistence.dao.restaurant;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.model.reservation.Service;

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

}
