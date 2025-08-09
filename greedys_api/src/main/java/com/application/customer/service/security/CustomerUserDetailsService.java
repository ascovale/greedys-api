package com.application.customer.service.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.security.LoginAttemptService;
import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.persistence.model.Customer;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerDAO custumerDAO;
    private final LoginAttemptService loginAttemptService;
    private final HttpServletRequest request;

    @Override
    public UserDetails loadUserByUsername(final String email) throws UsernameNotFoundException {
        final String ip = getClientIP();
        if (loginAttemptService.isBlocked(ip)) {
            throw new RuntimeException("blocked");
        }
        try {
            final Customer customer = custumerDAO.findByEmail(email);
            if (customer == null) {
                throw new UsernameNotFoundException("No user found with username: " + email);
            }

            // Forza il caricamento lazy delle autorità
            customer.getAuthorities().size();

            return customer;
        } catch (UsernameNotFoundException e) {
            // Re-lancia l'eccezione di autenticazione senza wrapping
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final String getClientIP() {
        final String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

}
