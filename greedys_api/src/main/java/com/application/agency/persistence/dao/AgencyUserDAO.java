package com.application.agency.persistence.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.agency.persistence.model.user.AgencyUser;

@Repository
public interface AgencyUserDAO extends JpaRepository<AgencyUser, Long> {

    /**
     * Find agency user by email
     */
    Optional<AgencyUser> findByEmail(String email);

    /**
     * Find agency users by agency ID
     */
    List<AgencyUser> findByAgencyId(Long agencyId);

    /**
     * Find agency users by agency ID with pagination
     */
    Page<AgencyUser> findByAgencyId(Long agencyId, Pageable pageable);

    /**
     * Find agency users by status
     */
    List<AgencyUser> findByStatus(AgencyUser.Status status);

    /**
     * Find agency users by agency and status
     */
    List<AgencyUser> findByAgencyIdAndStatus(Long agencyId, AgencyUser.Status status);

    /**
     * Search agency users by name or email within an agency
     */
    @Query("SELECT au FROM AgencyUser au WHERE au.agency.id = :agencyId AND " +
           "(LOWER(au.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(au.surname) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(au.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<AgencyUser> searchAgencyUsers(@Param("agencyId") Long agencyId, 
                                      @Param("searchTerm") String searchTerm, 
                                      Pageable pageable);

    /**
     * Find users by employee ID
     */
    Optional<AgencyUser> findByEmployeeId(String employeeId);

    /**
     * Find users by department within an agency
     */
    List<AgencyUser> findByAgencyIdAndDepartment(Long agencyId, String department);

    /**
     * Find users by position within an agency
     */
    List<AgencyUser> findByAgencyIdAndPosition(Long agencyId, String position);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if employee ID exists within an agency
     */
    boolean existsByAgencyIdAndEmployeeId(Long agencyId, String employeeId);

    /**
     * Count users in an agency
     */
    @Query("SELECT COUNT(au) FROM AgencyUser au WHERE au.agency.id = :agencyId")
    Long countByAgencyId(@Param("agencyId") Long agencyId);

    /**
     * Count active users in an agency
     */
    @Query("SELECT COUNT(au) FROM AgencyUser au WHERE au.agency.id = :agencyId AND au.status = 'ENABLED'")
    Long countActiveByAgencyId(@Param("agencyId") Long agencyId);

    /**
     * Find users with specific role in agency
     */
    @Query("SELECT au FROM AgencyUser au JOIN au.agencyRoles ar WHERE au.agency.id = :agencyId AND ar.name = :roleName")
    List<AgencyUser> findByAgencyIdAndRoleName(@Param("agencyId") Long agencyId, @Param("roleName") String roleName);

    /**
     * Find all agencies for a user (through different AgencyUser records)
     */
    @Query("SELECT au FROM AgencyUser au WHERE au.email = :email")
    List<AgencyUser> findAgencyUsersByEmail(@Param("email") String email);

    /**
     * Find agency user by email and agency ID - for multi-agency support
     */
    @Query("SELECT au FROM AgencyUser au WHERE au.email = :email AND au.agency.id = :agencyId")
    Optional<AgencyUser> findByEmailAndAgencyId(@Param("email") String email, @Param("agencyId") Long agencyId);
}