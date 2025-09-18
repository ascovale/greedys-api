package com.application.restaurant;

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
 * Filtro che valida l'accesso dei token Hub agli endpoint.
 * Responsabilità: Solo autorizzazione Hub (non autenticazione).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RUserHubValidationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Non applicare il filtro agli endpoint pubblici
        boolean isPublic = SecurityPatterns.isPublicPath(path);
        
        if (isPublic) {
            log.debug("Skipping HubValidationFilter for public endpoint: {}", path);
        }
        
        return isPublic; // Se è pubblico, non applicare il filtro
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        log.debug("HubValidationFilter: Processing {} {}", method, path);
        
        // Se arriviamo qui, l'endpoint è già protetto (SecurityConfig esclude i pubblici)
        // Dobbiamo solo verificare che ci sia un token valido
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String token = authHeader.substring(7);
        
        try {
            // Controlla se è un token Hub
            if (jwtUtil.isHubToken(token) || jwtUtil.isHubRefreshToken(token)) {
                log.debug("Hub token detected for path: {}", path);
                
                // I token Hub possono accedere solo a endpoint specifici
                if (!SecurityPatterns.isHubAllowedPath(path)) {
                    log.warn("Hub token attempted to access forbidden endpoint: {} {}", method, path);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Hub tokens can only access restaurant switching endpoints\"}");
                    return;
                }
                
                log.debug("Hub token access granted for endpoint: {} {}", method, path);
            } else {
                log.debug("Regular RUser token detected for path: {}", path);
                // Per i token RUser normali, tutti gli endpoint sono permessi (saranno validati dal RUserRequestFilter)
            }
            
        } catch (Exception e) {
            log.error("Error validating hub token for path {}: {}", path, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid token\"}");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}
