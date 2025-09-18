package com.application.admin.persistence.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.admin.persistence.model.RestaurantValidation;
import com.application.admin.persistence.model.RestaurantValidation.ValidationStatus;
import com.application.restaurant.persistence.model.Restaurant;

/**
 * DAO for RestaurantValidation entity
 */
@Repository
public interface RestaurantValidationDAO extends JpaRepository<RestaurantValidation, Long> {
    
    /**
     * Find the most recent validation for a restaurant
     */
    Optional<RestaurantValidation> findTopByRestaurantOrderByValidationDateDesc(Restaurant restaurant);
    
    /**
     * Find all validations for a specific restaurant ordered by validation date
     */
    List<RestaurantValidation> findByRestaurantOrderByValidationDateDesc(Restaurant restaurant);
    
    /**
     * Find validations by status
     */
    List<RestaurantValidation> findByStatusOrderByValidationDateDesc(ValidationStatus status);
    
    /**
     * Find validations between dates
     */
    List<RestaurantValidation> findByValidationDateBetweenOrderByValidationDateDesc(
            LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find validations by restaurant ID
     */
    List<RestaurantValidation> findByRestaurantIdOrderByValidationDateDesc(Long restaurantId);
    
    /**
     * Find validations that have errors
     */
    @Query("SELECT rv FROM RestaurantValidation rv WHERE " +
           "rv.placeIdError IS NOT NULL OR " +
           "rv.nameError IS NOT NULL OR " +
           "rv.addressError IS NOT NULL OR " +
           "rv.phoneError IS NOT NULL OR " +
           "rv.websiteError IS NOT NULL " +
           "ORDER BY rv.validationDate DESC")
    List<RestaurantValidation> findValidationsWithErrors();
    
    /**
     * Find validations where all data fields are valid
     */
    @Query("SELECT rv FROM RestaurantValidation rv WHERE " +
           "rv.nameValid = true AND " +
           "rv.addressValid = true AND " +
           "rv.phoneValid = true AND " +
           "rv.websiteValid = true " +
           "ORDER BY rv.validationDate DESC")
    List<RestaurantValidation> findValidationsWithAllDataValid();
    
    /**
     * Count validations by status
     */
    long countByStatus(ValidationStatus status);
    
    /**
     * Find validations performed by a specific admin
     */
    @Query("SELECT rv FROM RestaurantValidation rv WHERE rv.validatedBy.id = :adminId " +
           "ORDER BY rv.validationDate DESC")
    List<RestaurantValidation> findByValidatedByAdminId(@Param("adminId") Long adminId);
    
    /**
     * Check if a restaurant has been validated recently (within specified hours)
     */
    @Query("SELECT COUNT(rv) > 0 FROM RestaurantValidation rv WHERE " +
           "rv.restaurant = :restaurant AND " +
           "rv.validationDate > :sinceDate")
    boolean existsRecentValidation(@Param("restaurant") Restaurant restaurant, 
                                 @Param("sinceDate") LocalDateTime sinceDate);
}
