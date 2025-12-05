package com.application.common.persistence.dao.group;

import com.application.common.persistence.model.group.ProposalRevision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO per ProposalRevision - Audit trail delle proposte
 */
@Repository
public interface ProposalRevisionDAO extends JpaRepository<ProposalRevision, Long> {

    List<ProposalRevision> findByProposalIdOrderByVersionDesc(Long proposalId);

    Page<ProposalRevision> findByProposalId(Long proposalId, Pageable pageable);

    Optional<ProposalRevision> findByProposalIdAndVersion(Long proposalId, Integer version);

    @Query("SELECT r FROM ProposalRevision r WHERE r.proposal.id = :proposalId " +
           "ORDER BY r.version DESC LIMIT 1")
    Optional<ProposalRevision> findLatestByProposal(@Param("proposalId") Long proposalId);

    @Query("SELECT r FROM ProposalRevision r WHERE r.proposal.id = :proposalId " +
           "AND r.modifiedByType = :type ORDER BY r.version DESC")
    List<ProposalRevision> findByProposalAndModifierType(
        @Param("proposalId") Long proposalId, 
        @Param("type") String type);

    @Query("SELECT COUNT(r) FROM ProposalRevision r WHERE r.proposal.id = :proposalId")
    Long countByProposal(@Param("proposalId") Long proposalId);

    @Query("SELECT MAX(r.version) FROM ProposalRevision r WHERE r.proposal.id = :proposalId")
    Integer findMaxVersionByProposal(@Param("proposalId") Long proposalId);
}
