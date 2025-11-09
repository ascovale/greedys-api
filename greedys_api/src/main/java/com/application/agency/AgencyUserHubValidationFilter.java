package com.application.agency;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.application.common.security.SecurityPatterns;
import com.application.common.security.jwt.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filtro che valida l'accesso dei token Agency Hub agli endpoint.
 * Responsabilità: Solo autorizzazione Agency Hub (non autenticazione).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgencyUserHubValidationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Non applicare il filtro agli endpoint pubblici
        boolean isPublic = SecurityPatterns.isPublicPath(path);
        
        if (isPublic) {
            log.debug("Skipping AgencyHubValidationFilter for public endpoint: {}", path);
        }
        
        return isPublic; // Se è pubblico, non applicare il filtro
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        log.debug("AgencyHubValidationFilter: Processing {} {}", method, path);
        
        // Se arriviamo qui, l'endpoint è già protetto (SecurityConfig esclude i pubblici)
        // Dobbiamo solo verificare che ci sia un token valido
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String token = authHeader.substring(7);
        
        try {
            // Controlla se è un token Agency Hub
            if (jwtUtil.isAgencyHubToken(token) || jwtUtil.isAgencyHubRefreshToken(token)) {
                log.debug("Agency Hub token detected for path: {}", path);
                
                // I token Agency Hub possono accedere solo a endpoint specifici per agency
                if (!SecurityPatterns.isAgencyHubAllowedPath(path)) {
                    log.warn("Agency Hub token attempted to access forbidden endpoint: {} {}", method, path);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Agency Hub tokens can only access agency switching endpoints\"}");
                    return;
                }
                
                log.debug("Agency Hub token access granted for endpoint: {} {}", method, path);
            } else {
                log.debug("Regular AgencyUser token detected for path: {}", path);
                // Per i token AgencyUser normali, tutti gli endpoint sono permessi (saranno validati dal AgencyUserRequestFilter)
            }
            
        } catch (Exception e) {
            log.error("Error validating agency hub token for path {}: {}", path, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid token\"}");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}