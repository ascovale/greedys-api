package com.application.common.persistence.dao.group;

import com.application.common.persistence.model.group.GroupBookingDietaryNeeds;
import com.application.common.persistence.model.group.enums.DietaryRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO per GroupBookingDietaryNeeds - Esigenze alimentari del gruppo
 */
@Repository
public interface GroupBookingDietaryNeedsDAO extends JpaRepository<GroupBookingDietaryNeeds, Long> {

    List<GroupBookingDietaryNeeds> findByGroupBookingId(Long groupBookingId);

    Optional<GroupBookingDietaryNeeds> findByGroupBookingIdAndDietaryRequirement(
        Long groupBookingId, 
        DietaryRequirement dietaryRequirement);

    void deleteByGroupBookingId(Long groupBookingId);

    @Query("SELECT SUM(d.paxCount) FROM GroupBookingDietaryNeeds d WHERE d.groupBooking.id = :bookingId")
    Integer sumPaxWithDietaryNeeds(@Param("bookingId") Long bookingId);

    @Query("SELECT d FROM GroupBookingDietaryNeeds d WHERE d.groupBooking.id = :bookingId AND d.isCritical = true")
    List<GroupBookingDietaryNeeds> findCriticalByBooking(@Param("bookingId") Long bookingId);
}
