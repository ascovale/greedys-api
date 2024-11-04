package com.application.persistence.dao.user;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.reservation.Reservation;
import com.application.web.dto.get.ReservationDTO;



@Repository
public interface ReservationDAO extends JpaRepository<Reservation, Long> {
	List<ReservationDTO> findAllByUser_id(Long user_id);

	@Query(value = "SELECT day FROM "
					+ "(SELECT day "
					+ "FROM service_full "
					+ "WHERE idservice IN "
						+ "(SELECT id "
						+ "FROM service "
						+ "WHERE idrestaurant = :idrestaurant) "
					+ "UNION ALL "
						+ "SELECT day "
						+ "FROM service_closed "
						+ "WHERE idservice IN "
							+ "(SELECT id "
							+ "FROM service "
							+ "WHERE idrestaurant = :idrestaurant)) "
					+ "AS days GROUP BY day HAVING COUNT(*) = "
						+ "(SELECT COUNT(*) "
						+ "FROM service "
						+ "WHERE idrestaurant = :idrestaurant) AND "
							+ "day >= CURRENT_DATE", nativeQuery = true)
	List<LocalDate> findClosedOrFullServices(@Param("idrestaurant") Long idrestaurant);
	
	@Query(value = "WITH date_range AS (" +
		    "SELECT MIN(`from`) AS start_date, MAX(`to`) AS end_date " +
		    "FROM service " +
		    "WHERE idrestaurant = :restaurantId), " +
		    "all_dates AS (" +
		    "SELECT ADDDATE(start_date, @num:=@num+1) the_date " +
		    "FROM date_range " +
		    "JOIN (SELECT @num:=-1) num " +
		    "WHERE ADDDATE(start_date, @num+1) <= end_date) " +
		    "SELECT the_date " +
		    "FROM all_dates " +
		    "WHERE NOT EXISTS (" +
		    "SELECT 1 FROM service " +
		    "WHERE all_dates.the_date BETWEEN service.`s_from` AND service.`s_to` " +
		    "AND idrestaurant = :restaurantId)", nativeQuery = true)
	List<LocalDate> findDatesWithoutAnyService(@Param("restaurantId") Long restaurantId);

 
	@Query(value = "SELECT r FROM Reservation r WHERE r.slot.service.restaurant.id = :idrestaurant AND r.date = :date")
	List<Reservation> findDayReservation(@Param("idrestaurant") Long idRestaurant, @Param("date") LocalDate date);

	/* 
	@Query(value = "SELECT r FROM Reservation r WHERE r.idservice IN "
			+ "(SELECT id FROM Service WHERE idrestaurant = :idrestaurant) AND r.date = :date")
	List<Reservation> findDayReservation(@Param("idrestaurant") Long idrestaurant, @Param("date") LocalDate date);
	
	@Query(value = "SELECT r FROM Reservation r WHERE r.idservice IN "
			+ "(SELECT id FROM Service WHERE idrestaurant = :idrestaurant) AND r.date = :date AND r.status = 1")
	List<Reservation> findDayReservationConfirmed(@Param("idrestaurant") Long idrestaurant, @Param("date") LocalDate date);
	
	@Query(value = "SELECT r FROM Reservation r WHERE r.idservice IN "
			+ "(SELECT id FROM Service WHERE idrestaurant = :idrestaurant) AND r.date = :date AND r.status = 0")
	List<Reservation> findDayReservationPending(@Param("idrestaurant") Long idrestaurant, @Param("date") LocalDate date);
	
	@Query(value = "SELECT r FROM Reservation r WHERE r.idservice IN "
			+ "(SELECT id FROM Service WHERE idrestaurant = :idrestaurant) AND r.date = :date AND r.status = 2")
	List<Reservation> findDayReservationRejected(@Param("idrestaurant") Long idrestaurant, @Param("date") LocalDate date);
	
	@Query(value = "SELECT r FROM Reservation r WHERE r.idservice IN "
			+ "(SELECT id FROM Service WHERE idrestaurant = :idrestaurant) AND r.date = :date AND r.status = 3")
	List<Reservation> findDayReservationCanceled(@Param("idrestaurant") Long idrestaurant, @Param("date") LocalDate date);
	
	@Query(value = "SELECT r FROM Reservation r WHERE r.idservice IN "
			+ "(SELECT id FROM Service WHERE idrestaurant = :idrestaurant) AND r.date = :date AND r.status = 4")
	List<Reservation> findDayReservationExpired(@Param("idrestaurant") Long idrestaurant, @Param("date") LocalDate date);
	
	@Query(value = "SELECT r FROM Reservation r WHERE r.idservice IN "
			+ "(SELECT id FROM Service WHERE idrestaurant = :idrestaurant) AND r.date = :date AND r.status = 5")
	List<Reservation> findDayReservationNoShow(@Param("idrestaurant") Long idrestaurant, @Param("


	*/

	@Query(value = """
			SELECT r FROM Reservation r
			WHERE r.restaurant.id = :restaurant_id
				AND r.date BETWEEN :start_date AND :end_date
				ORDER BY r.date, r.slot.start
			""")
    Collection<Reservation> findByRestaurantAndDateBetween(Long restaurant_id, LocalDate start_date, LocalDate end_date);

	@Query(value = """
			SELECT r FROM Reservation r
			WHERE r.restaurant.id = :restaurant_id
				AND r.date = :date
				ORDER BY r.slot.start
			""")
	Collection<Reservation> findByRestaurantAndDate(Long restaurant_id, LocalDate date);

	@Query(value = """
			SELECT r FROM Reservation r
			WHERE r.restaurant.id = :restaurant_id
				AND r.date BETWEEN :start AND :end
				AND r.accepted = False
				AND r.rejected = False
				ORDER BY r.date, r.slot.start
			""")
    Collection<Reservation> findByRestaurantAndDateBetweenAndPending(Long restaurant_id, LocalDate start, LocalDate end);


	@Query(value = """
	SELECT r FROM Reservation r
			WHERE r.restaurant.id = :restaurant_id
				AND r.date BETWEEN :start AND :end
				AND r.accepted = True
				ORDER BY r.date, r.slot.start
				""")
    Collection<Reservation> findByRestaurantAndDateBetweenAndAccepted(Long restaurant_id, LocalDate start, LocalDate end);

	@Query(value = """
			SELECT r FROM Reservation r
			WHERE r.restaurant.id = :restaurant_id
				AND r.date >= :start
				AND r.accepted = False
				AND r.rejected = False
				ORDER BY r.date, r.slot.start
			""")
    Collection<Reservation> findByRestaurantAndDateAndPending(Long restaurant_id, LocalDate start);

	@Query(value = """
			SELECT r FROM Reservation r
			WHERE r.restaurant.id = :restaurant_id
				AND r.date >= :start
				AND r.accepted = True
				ORDER BY r.date, r.slot.start
			""")
	Collection<Reservation> findByRestaurantAndDateAndAccepted(Long restaurant_id, LocalDate start);

	@Query(value = """
			SELECT r FROM Reservation r
			WHERE r.restaurant.id = :restaurant_id
				AND r.accepted = False
				AND r.rejected = False
				ORDER BY r.creationDate, r.date, r.slot.start
			""")
	Collection<Reservation> findByRestaurantIdAndPending(Long restaurant_id);

	@Query(value = """
			SELECT r FROM Reservation r
			WHERE r.user.id = :userId
				ORDER BY r.date, r.slot.start
			""")
	Collection<Reservation> findByUser(Long userId);

	@Query(value = """
			SELECT r FROM Reservation r
			WHERE r.user.id = :userId
				AND r.accepted = False
				AND r.rejected = False
				ORDER BY r.date, r.slot.start
			""")
	Collection<Reservation> findByUserAndPending(Long userId);

	@Query(value = """
			SELECT r FROM Reservation r
			WHERE r.user.id = :userId
				AND r.accepted = True
				ORDER BY r.date, r.slot.start
			""")
	Collection<Reservation> findByUserAndAccepted(Long userId);
}
