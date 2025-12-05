package com.application.agency.persistence.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.agency.persistence.model.AgencyLogo;

/**
 * Repository per la gestione del logo dell'agenzia
 */
@Repository
public interface AgencyLogoDAO extends JpaRepository<AgencyLogo, Long> {

    /**
     * Trova il logo attivo dell'agenzia
     */
    @Query("SELECT l FROM AgencyLogo l WHERE l.agency.id = :agencyId AND l.isActive = true")
    Optional<AgencyLogo> findActiveLogoByAgencyId(@Param("agencyId") Long agencyId);

    /**
     * Verifica se esiste un logo attivo per l'agenzia
     */
    boolean existsByAgencyIdAndIsActiveTrue(Long agencyId);

    /**
     * Disattiva tutti i logo di un'agenzia (per sostituzione)
     */
    @Modifying
    @Query("UPDATE AgencyLogo l SET l.isActive = false WHERE l.agency.id = :agencyId")
    int deactivateAllByAgencyId(@Param("agencyId") Long agencyId);

    /**
     * Soft delete del logo
     */
    @Modifying
    @Query("UPDATE AgencyLogo l SET l.isActive = false WHERE l.id = :logoId")
    int softDeleteById(@Param("logoId") Long logoId);
}
