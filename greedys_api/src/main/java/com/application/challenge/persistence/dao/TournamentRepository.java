package com.application.challenge.persistence.dao;

import com.application.challenge.persistence.model.Tournament;
import com.application.challenge.persistence.model.enums.TournamentPhase;
import com.application.challenge.persistence.model.enums.TournamentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository per Tournament.
 */
@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    // ==================== FIND BY NAME ====================

    Optional<Tournament> findByNameIgnoreCase(String name);

    boolean existsByName(String name);

    // ==================== FIND BY STATUS ====================

    List<Tournament> findByStatus(TournamentStatus status);

    Page<Tournament> findByStatus(TournamentStatus status, Pageable pageable);

    List<Tournament> findByStatusIn(List<TournamentStatus> statuses);

    @Query("SELECT t FROM Tournament t WHERE t.status IN :statuses ORDER BY t.tournamentStart ASC")
    List<Tournament> findActiveOrUpcoming(@Param("statuses") List<TournamentStatus> statuses);

    // ==================== FIND BY PHASE ====================

    List<Tournament> findByCurrentPhase(TournamentPhase phase);

    @Query("SELECT t FROM Tournament t WHERE t.currentPhase IN :phases AND t.status = :status")
    List<Tournament> findByPhasesAndStatus(
            @Param("phases") List<TournamentPhase> phases,
            @Param("status") TournamentStatus status);

    // ==================== FIND BY LOCATION ====================

    List<Tournament> findByCity(String city);

    List<Tournament> findByCityAndStatus(String city, TournamentStatus status);

    @Query("SELECT t FROM Tournament t WHERE t.city = :city AND t.status IN :statuses ORDER BY t.tournamentStart ASC")
    List<Tournament> findByCityAndStatuses(
            @Param("city") String city,
            @Param("statuses") List<TournamentStatus> statuses);

    // ==================== FIND BY DATE ====================

    List<Tournament> findByTournamentStartBetween(LocalDate start, LocalDate end);

    List<Tournament> findByTournamentEndBefore(LocalDate date);

    @Query("SELECT t FROM Tournament t WHERE t.registrationStart <= :date AND t.registrationEnd >= :date AND t.status = 'REGISTRATION'")
    List<Tournament> findWithOpenRegistration(@Param("date") LocalDate date);

    // ==================== SEARCH ====================

    @Query("SELECT t FROM Tournament t WHERE " +
           "(LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND t.status IN :statuses")
    Page<Tournament> searchByQueryAndStatuses(
            @Param("query") String query,
            @Param("statuses") List<TournamentStatus> statuses,
            Pageable pageable);

    // ==================== STATISTICS ====================

    @Query("SELECT COUNT(t) FROM Tournament t WHERE t.status = :status")
    long countByStatus(@Param("status") TournamentStatus status);
}
