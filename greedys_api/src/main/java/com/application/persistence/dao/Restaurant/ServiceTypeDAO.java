package com.application.persistence.dao.Restaurant;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.model.reservation.ServiceType;

@Transactional(readOnly = true)
@Repository
public interface ServiceTypeDAO extends JpaRepository<ServiceType, Long> {
	@Query(value = "SELECT st FROM ServiceType st WHERE EXISTS (SELECT s FROM Service s WHERE s.serviceType.id = st.id AND s.restaurant.id = :id)")
	List<ServiceType> findServiceTypesWithServicesByRestaurant(Long id);

    ServiceType findByName(String name);
}