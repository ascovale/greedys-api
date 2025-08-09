package com.application.customer;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.persistence.model.Customer;
import com.application.customer.service.security.CustomerUserDetailsService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CustomerAuthenticationProvider extends DaoAuthenticationProvider {

    private final CustomerDAO customerDAO;

    public CustomerAuthenticationProvider(CustomerDAO customerDAO, CustomerUserDetailsService userDetailsService) {
        super(userDetailsService);
        this.customerDAO = customerDAO;
    }

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        try {
            // Prima verifica se l'utente esiste
            final Customer user = customerDAO.findByEmail(auth.getName());
            if (user == null) {
                log.debug("Failed to find user '{}'", auth.getName());
                throw new BadCredentialsException("Invalid username or password");
            }

            // Verifica lo stato dell'account prima dell'autenticazione
            if (!user.isAccountNonLocked()) {
                String statusMessage = getStatusMessage(user.getStatus());
                log.warn("Account locked for user {}: {}", auth.getName(), statusMessage);
                throw new BadCredentialsException(statusMessage);
            }

            // Lascia che DaoAuthenticationProvider gestisca l'autenticazione (password, authorities)
            final Authentication result = super.authenticate(auth);
            
            // Restituisci il token con l'entità Customer invece di UserDetails
            return new UsernamePasswordAuthenticationToken(user, result.getCredentials(), result.getAuthorities());
            
        } catch (AuthenticationException e) {
            log.error("Errore durante l'autenticazione per l'utente {}: {}", auth.getName(), e.getMessage());
            throw e;
        }
    }

    private String getStatusMessage(Customer.Status status) {
        switch (status) {
            case VERIFY_TOKEN:
                return "Account not verified. Please check your email and click the verification link.";
            case DISABLED:
                return "Account is disabled. Please contact support.";
            case BLOCKED:
                return "Account is blocked. Please contact support.";
            case DELETED:
                return "Account has been deleted.";
            case AUTO_DELETE:
                return "Account is scheduled for deletion.";
            default:
                return "Account is not active.";
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}

