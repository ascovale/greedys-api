package com.application.persistence.model.customer;

import java.util.List;

import com.application.persistence.model.user.BaseRole;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "role")
public class Role implements BaseRole<Privilege> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    @ManyToMany
    @JoinTable(name = "role_has_privilege", 
        joinColumns = @JoinColumn(name = "role_id"), 
        inverseJoinColumns = @JoinColumn(name = "privilege_id")
    )
    private List<Privilege> privileges;

    public Role() {
        super();
    }

    public Role(final String name) {
        super();
        this.name = name;
    }

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

    @Override
	public List<Privilege> getPrivileges() {
		return privileges;
	}

	@Override
    public void setPrivileges(List<Privilege> privileges) {
		this.privileges = privileges;
	}

	@Override
    public void addPrivilege(Privilege privilege) {
        if (privileges == null) {
            privileges = new java.util.ArrayList<>();
        }
        privileges.add(privilege);
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Role role = (Role) obj;
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

    public List<Privilege> getPermissions() {
        return this.privileges;
    }


    public void setPermissions(List<Privilege> privileges) {
        this.privileges = privileges;
    }
    


}