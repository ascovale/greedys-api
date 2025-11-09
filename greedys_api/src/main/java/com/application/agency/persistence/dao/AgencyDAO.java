package com.application.agency.persistence.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.agency.persistence.model.Agency;

@Repository
public interface AgencyDAO extends JpaRepository<Agency, Long> {

    /**
     * Find agency by name
     */
    Optional<Agency> findByName(String name);

    /**
     * Find agency by email
     */
    Optional<Agency> findByEmail(String email);

    /**
     * Find agency by tax code
     */
    Optional<Agency> findByTaxCode(String taxCode);

    /**
     * Find agency by VAT number
     */
    Optional<Agency> findByVatNumber(String vatNumber);

    /**
     * Find agencies by status
     */
    List<Agency> findByStatus(Agency.Status status);

    /**
     * Find agencies by type
     */
    List<Agency> findByAgencyType(Agency.AgencyType agencyType);

    /**
     * Find active agencies
     */
    @Query("SELECT a FROM Agency a WHERE a.status = 'ACTIVE'")
    List<Agency> findActiveAgencies();

    /**
     * Find agencies by status with pagination
     */
    Page<Agency> findByStatus(Agency.Status status, Pageable pageable);

    /**
     * Search agencies by name or city
     */
    @Query("SELECT a FROM Agency a WHERE " +
           "LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.city) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Agency> searchAgencies(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find agencies in a specific city
     */
    List<Agency> findByCity(String city);

    /**
     * Find agencies in a specific country
     */
    List<Agency> findByCountry(String country);

    /**
     * Check if agency name exists
     */
    boolean existsByName(String name);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if tax code exists
     */
    boolean existsByTaxCode(String taxCode);

    /**
     * Count agencies by status
     */
    @Query("SELECT COUNT(a) FROM Agency a WHERE a.status = :status")
    Long countByStatus(@Param("status") Agency.Status status);

    /**
     * Find agencies with specific user
     */
    @Query("SELECT DISTINCT a FROM Agency a JOIN a.agencyUsers au WHERE au.email = :userEmail")
    List<Agency> findAgenciesByUserEmail(@Param("userEmail") String userEmail);
}