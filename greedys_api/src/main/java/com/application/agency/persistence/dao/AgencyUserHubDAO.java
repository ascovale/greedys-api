package com.application.agency.persistence.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.agency.persistence.model.user.AgencyUserHub;

@Repository
public interface AgencyUserHubDAO extends JpaRepository<AgencyUserHub, Long> {

    /**
     * Find agency user hub by email
     */
    Optional<AgencyUserHub> findByEmail(String email);

    /**
     * Find agency user hub by email and accepted status
     */
    @Query("SELECT auh FROM AgencyUserHub auh WHERE auh.email = :email AND auh.accepted = :accepted")
    Optional<AgencyUserHub> findByEmailAndAccepted(@Param("email") String email, @Param("accepted") boolean accepted);

    /**
     * Find all accepted agency user hubs
     */
    List<AgencyUserHub> findByAcceptedTrue();

    /**
     * Find all pending agency user hubs (not accepted)
     */
    List<AgencyUserHub> findByAcceptedFalse();

    /**
     * Find agency user hubs by status
     */
    List<AgencyUserHub> findByStatus(AgencyUserHub.Status status);

    /**
     * Search agency user hubs by name or email
     */
    @Query("SELECT auh FROM AgencyUserHub auh WHERE " +
           "LOWER(auh.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(auh.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(auh.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<AgencyUserHub> searchByNameOrEmail(@Param("searchTerm") String searchTerm);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find enabled and accepted hubs
     */
    @Query("SELECT auh FROM AgencyUserHub auh WHERE auh.status = 'ENABLED' AND auh.accepted = true")
    List<AgencyUserHub> findEnabledAndAccepted();

    /**
     * Count hubs by status
     */
    @Query("SELECT COUNT(auh) FROM AgencyUserHub auh WHERE auh.status = :status")
    Long countByStatus(@Param("status") AgencyUserHub.Status status);

    /**
     * Find hubs by phone number
     */
    Optional<AgencyUserHub> findByPhoneNumber(String phoneNumber);
}