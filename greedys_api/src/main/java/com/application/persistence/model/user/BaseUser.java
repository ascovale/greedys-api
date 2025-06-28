package com.application.persistence.model.user;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseUser implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;
    protected Integer toReadNotification = 0;
    protected Enum<?> status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getToReadNotification() { return toReadNotification; }
    public void setToReadNotification(Integer toReadNotification) { this.toReadNotification = toReadNotification; }

    public Enum<?> getStatus() { return status; }
    public void setStatus(Enum<?> status) { this.status = status; }

    // Metodi astratti per dati identificativi
    public abstract String getEmail();
    public abstract String getPhoneNumber();
    public abstract String getName();
    public abstract String getSurname();
    public abstract String getPassword();

    // Metodi astratti per ruoli/privilegi
    public abstract List<? extends BaseRole<?>> getRoles();
    public abstract List<? extends BasePrivilege> getPrivileges();
    public abstract List<String> getPrivilegesStrings();


    @Override
    public List<? extends GrantedAuthority> getAuthorities() {
        return getGrantedAuthorities(getPrivilegesStrings());
    }
    protected List<GrantedAuthority> getGrantedAuthorities(List<String> privileges) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String privilege : privileges) {
            authorities.add(new SimpleGrantedAuthority(privilege));
        }
        return authorities;
    }
}
