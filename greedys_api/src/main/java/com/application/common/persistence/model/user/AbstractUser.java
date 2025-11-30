package com.application.common.persistence.model.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
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

    @Column(length = 60)
    private String password; // Nullable per supportare Customer non registrati

    private String phoneNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private AbstractUser createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by_id")
    private AbstractUser modifiedBy;
    
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

    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Check if user has a password set (i.e., can authenticate)
     */
    public boolean hasPassword() {
        return password != null && !password.trim().isEmpty();
    }
}
