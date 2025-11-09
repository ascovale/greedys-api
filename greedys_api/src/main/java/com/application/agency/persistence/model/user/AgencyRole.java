package com.application.agency.persistence.model.user;

import java.util.ArrayList;
import java.util.List;

import com.application.common.persistence.model.user.BaseRole;
import com.application.agency.persistence.model.Agency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "agency_role")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyRole implements BaseRole<AgencyPrivilege> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private Agency agency;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "agency_role_has_privilege",
        joinColumns = @JoinColumn(name = "agency_role_id"),
        inverseJoinColumns = @JoinColumn(name = "agency_privilege_id")
    )
    @Builder.Default
    private List<AgencyPrivilege> privileges = new ArrayList<>();

    @ManyToMany(mappedBy = "agencyRoles", fetch = FetchType.LAZY)
    @Builder.Default
    private List<AgencyUser> agencyUsers = new ArrayList<>();

    @Override
    public List<AgencyPrivilege> getPrivileges() {
        return privileges;
    }

    @Override
    public void setPrivileges(List<AgencyPrivilege> privileges) {
        this.privileges = privileges;
    }

    @Override
    public void addPrivilege(AgencyPrivilege privilege) {
        if (this.privileges == null) {
            this.privileges = new ArrayList<>();
        }
        this.privileges.add(privilege);
    }

    /**
     * Add agency user to this role
     */
    public void addAgencyUser(AgencyUser agencyUser) {
        if (this.agencyUsers == null) {
            this.agencyUsers = new ArrayList<>();
        }
        this.agencyUsers.add(agencyUser);
        if (!agencyUser.getAgencyRoles().contains(this)) {
            agencyUser.getAgencyRoles().add(this);
        }
    }

    /**
     * Remove agency user from this role
     */
    public void removeAgencyUser(AgencyUser agencyUser) {
        if (this.agencyUsers != null) {
            this.agencyUsers.remove(agencyUser);
            agencyUser.getAgencyRoles().remove(this);
        }
    }
}