package com.application.persistence.model.user;

import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractUser implements UserDetails {
    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;
    protected String name;
    protected String surname;
    protected String email;
    @Column(length = 60)
    protected String password;
    protected String phoneNumber;
    protected Integer toReadNotification = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Integer getToReadNotification() { return toReadNotification; }
    public void setToReadNotification(Integer toReadNotification) { this.toReadNotification = toReadNotification; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    // Metodi astratti da implementare nelle sottoclassi
    @Override
    public abstract boolean isEnabled();
    @Override
    public abstract boolean isAccountNonExpired();
    @Override
    public abstract boolean isAccountNonLocked();
    @Override
    public abstract java.util.List<? extends GrantedAuthority> getAuthorities();

    //public abstract java.util.List <? extends Notification> getNotifications();
}
