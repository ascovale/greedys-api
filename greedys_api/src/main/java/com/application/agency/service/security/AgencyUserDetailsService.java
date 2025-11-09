package com.application.agency.service.security;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.security.LoginAttemptService;
import com.application.agency.persistence.dao.AgencyUserDAO;
import com.application.agency.persistence.model.user.AgencyUser;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AgencyUserDetailsService implements UserDetailsService {

    private final AgencyUserDAO agencyUserDAO;
    private final LoginAttemptService loginAttemptService;
    private final HttpServletRequest request;

    @Override
    public UserDetails loadUserByUsername(final String username) {
        final String ip = getClientIP();
        if (loginAttemptService.isBlocked(ip)) {
            throw new RuntimeException("blocked");
        }

        try {
            // Split the username into email and agencyId
            String[] parts = username.split(":");
            if (parts.length != 2) {
                throw new UsernameNotFoundException("Invalid username format. Expected 'email:agencyId'.");
            }

            String email = parts[0];
            Long agencyId;
            try {
                agencyId = Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                throw new UsernameNotFoundException("Invalid agencyId format.");
            }

            AgencyUser user = null;
            // Trova tutti gli AgencyUser per questa email e cerca quello con l'agencyId corretto
            List<AgencyUser> users = agencyUserDAO.findAgencyUsersByEmail(email);
            for (AgencyUser u : users) {
                if (u.getAgency() != null && agencyId.equals(u.getAgency().getId())) {
                    user = u;
                    break;
                }
            }
            
            if (user == null) {
                throw new UsernameNotFoundException("No user found with email: " + email + " and agency ID: " + agencyId);
            }

            // Forza il caricamento lazy delle autorità
            user.getAuthorities().size();

            return user;
        } catch (UsernameNotFoundException e) {
            // Re-lancia l'eccezione di autenticazione senza wrapping
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public UserDetails loadUserById(final Long agencyUserId) throws UsernameNotFoundException {
        try {
            final AgencyUser user = agencyUserDAO.findById(agencyUserId).orElse(null);
            if (user == null) {
                throw new UsernameNotFoundException("No user found with ID: " + agencyUserId);
            }

            // Forza il caricamento lazy delle autorità
            user.getAuthorities().size();

            return user;
        } catch (UsernameNotFoundException e) {
            // Re-lancia l'eccezione di autenticazione senza wrapping
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getClientIP() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}