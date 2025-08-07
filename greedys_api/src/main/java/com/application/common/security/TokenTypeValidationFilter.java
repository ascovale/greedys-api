package com.application.common.security;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.application.common.security.jwt.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filtro che valida il tipo di token (Access vs Refresh).
 * Responsabilità: Solo validazione del tipo di token.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenTypeValidationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        log.debug("TokenTypeValidationFilter: Processing {} {}", method, path);
        
        // Se arriviamo qui, l'endpoint è già protetto (SecurityConfig esclude i pubblici)
        // Dobbiamo solo verificare che ci sia un token valido
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Protected endpoint {} {} accessed without token", method, path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Authorization token required\"}");
            return;
        }
        
        String token = authHeader.substring(7);
        
        try {
            // 🔄 ENDPOINT DI REFRESH: Validazione specifica per tipo
            if (isRefreshEndpoint(path)) {
                if (path.equals("/restaurant/user/auth/refresh/hub")) {
                    // Endpoint Hub refresh: solo Hub refresh token
                    if (!jwtUtil.isHubRefreshToken(token)) {
                        log.warn("Hub refresh endpoint {} {} accessed with non-hub-refresh token", method, path);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Hub refresh token required for this endpoint\"}");
                        return;
                    }
                } else {
                    // Altri endpoint di refresh: solo refresh token normali
                    if (!jwtUtil.isRefreshToken(token)) {
                        log.warn("Refresh endpoint {} {} accessed with non-refresh token", method, path);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Refresh token required for this endpoint\"}");
                        return;
                    }
                }
                log.debug("Valid refresh token for endpoint {} {}", method, path);
            } else {
                // 🎯 ENDPOINT NORMALI: Solo access token
                if (!jwtUtil.isAccessToken(token) && !jwtUtil.isHubToken(token)) {
                    log.warn("Protected endpoint {} {} accessed with non-access token", method, path);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Access token required for this endpoint\"}");
                    return;
                }
                log.debug("Valid access token for endpoint {} {}", method, path);
            }
        } catch (Exception e) {
            log.error("Error validating token type for path {} {}: {}", method, path, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid token\"}");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Verifica se l'endpoint è un endpoint di refresh che richiede refresh token
     */
    private boolean isRefreshEndpoint(String path) {
        return path.equals("/customer/auth/refresh") || 
               path.equals("/admin/auth/refresh") ||
               path.equals("/restaurant/user/auth/refresh") ||
               path.equals("/restaurant/user/auth/refresh/hub");
    }
}
