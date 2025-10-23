package com.application.common.persistence.model.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Hidden
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class AbstractUser implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @NotNull
    @Builder.Default
    private Integer toReadNotification = 0;

    @NotNull
    private String name;

    @NotNull
    private String surname;

    private String nickName;

    @NotNull
    private String email;

    @NotNull
    @Column(length = 60)
    private String password;

    private String phoneNumber;
    
    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    // Abstract methods for roles and privileges
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

    @Override
    public String getUsername() {
        return email;
    }
}
