package com.application.persistence.model.admin;

import java.util.Collection;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_role")
public class AdminRole {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;

    @ManyToMany
    @JoinTable(name = "admin_role_has_admin_privilege", 
    	joinColumns = @JoinColumn(name = "admin_role_id"), 
    	inverseJoinColumns = @JoinColumn(name = "admin_privilege_id")
    )	
    private Collection<AdminPrivilege> adminPrivileges;

    public AdminRole() {
        super();
    }

    public AdminRole(final String name) {
        super();
        this.name = name;
    }

    //

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }


	public Collection<AdminPrivilege> getAdminPrivileges() {
		return adminPrivileges;
	}

	public void setAdminPrivileges(Collection<AdminPrivilege> adminPrivileges) {
		this.adminPrivileges = adminPrivileges;
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

}