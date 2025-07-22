package com.application.admin.model;

import java.util.List;

import com.application.common.persistence.model.user.BaseRole;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "admin_role")
@Getter
@Setter
public class AdminRole implements BaseRole<AdminPrivilege> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;

    @ManyToMany
    @JoinTable(name = "admin_role_has_admin_privilege", 
    	joinColumns = @JoinColumn(name = "admin_role_id"), 
    	inverseJoinColumns = @JoinColumn(name = "admin_privilege_id")
    )	
    private List<AdminPrivilege> adminPrivileges;

    public AdminRole() {
        super();
    }

    public AdminRole(final String name) {
        super();
        this.name = name;
    }

	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof String) {
            return name.equals(obj);
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AdminRole role = (AdminRole) obj;
        if (!name.equals(role.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Role [name=").append(name).append("]").append("[id=").append(id).append("]");
        return builder.toString();
    }


    @Override
    public List<AdminPrivilege> getPrivileges() {
        return adminPrivileges;
    }

    @Override
    public void setPrivileges(List<AdminPrivilege> privileges) {
        this.adminPrivileges = privileges;
    }

    @Override
    public void addPrivilege(AdminPrivilege privilege) {
        if (adminPrivileges == null) {
            adminPrivileges = List.of();
        }
        adminPrivileges.add(privilege);
    }


}