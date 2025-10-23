package com.application.common.security;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.application.common.security.jwt.JwtUtil;
import com.application.common.web.ErrorDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filtro che valida il tipo di token (Access vs Refresh).
 * Responsabilità: Solo validazione del tipo di token.
 * 
 * UNIFIED SECURITY: Usa solo SecurityPatterns come fonte di verità
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenTypeValidationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Bypass CORS preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // UNIFIED SECURITY: Usa solo SecurityPatterns come fonte di verità
        boolean isPublic = SecurityPatterns.isPublicPath(path);

        if (isPublic) {
            log.debug("Skipping TokenTypeValidationFilter for public endpoint: {}", path);
        }
        return isPublic;
    }

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
            writeAuthenticationError(response, "Authorization token required");
            return;
        }
        
        String token = authHeader.substring(7);
        
        try {
            // 🔄 ENDPOINT DI REFRESH: Validazione specifica per tipo
            if (SecurityPatterns.isRefreshTokenPath(path)) {
                if (path.equals("/restaurant/user/auth/refresh/hub")) {
                    // Endpoint Hub refresh: solo Hub refresh token
                    if (!jwtUtil.isHubRefreshToken(token)) {
                        log.warn("Hub refresh endpoint {} {} accessed with non-hub-refresh token", method, path);
                        writeAuthenticationError(response, "Hub refresh token required for this endpoint");
                        return;
                    }
                } else {
                    // Altri endpoint di refresh: solo refresh token normali
                    if (!jwtUtil.isRefreshToken(token)) {
                        log.warn("Refresh endpoint {} {} accessed with non-refresh token", method, path);
                        writeAuthenticationError(response, "Refresh token required for this endpoint");
                        return;
                    }
                }
                log.debug("Valid refresh token for endpoint {} {}", method, path);
            } else {
                // 🎯 ENDPOINT NORMALI: Solo access token
                if (!jwtUtil.isAccessToken(token) && !jwtUtil.isHubToken(token)) {
                    log.warn("Protected endpoint {} {} accessed with non-access token", method, path);
                    writeAuthenticationError(response, "Access token required for this endpoint");
                    return;
                }
                log.debug("Valid access token for endpoint {} {}", method, path);
            }
            
            // 🎯 CONTROLLO USER TYPE vs ENDPOINT
            if (!isValidUserTypeForEndpoint(token, path)) {
                log.warn("Token user type mismatch for endpoint {} {}", method, path);
                writeAccessDeniedError(response, "Token user type not allowed for this endpoint");
                return;
            }
            
        } catch (Exception e) {
            log.error("Error validating token type for path {} {}: {}", method, path, e.getMessage());
            writeAuthenticationError(response, "Invalid token");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Verifica se il tipo di utente del token è compatibile con l'endpoint
     */
    private boolean isValidUserTypeForEndpoint(String token, String path) {
        try {
            String userType = jwtUtil.extractUserType(token);
            
            if (path.startsWith("/customer/")) {
                return "customer".equals(userType);
            } else if (path.startsWith("/admin/")) {
                return "admin".equals(userType);
            } else if (path.startsWith("/restaurant/")) {
                return "restaurant-user".equals(userType) || "restaurant-user-hub".equals(userType);
            }
            
            return true; // Per altri endpoint
        } catch (Exception e) {
            log.error("Error extracting user type from token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Scrive una risposta di errore di autenticazione (401) in formato JSON compatibile con ResponseWrapper
     */
    private void writeAuthenticationError(HttpServletResponse response, String message) throws IOException {
        writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                         "Authentication required: " + message, "AUTHENTICATION_REQUIRED");
    }
    
    /**
     * Scrive una risposta di errore di accesso negato (403) in formato JSON compatibile con ResponseWrapper
     */
    private void writeAccessDeniedError(HttpServletResponse response, String message) throws IOException {
        writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, 
                         "Access denied: " + message, "ACCESS_DENIED");
    }
    
    /**
     * Metodo generico per scrivere risposte di errore in formato JSON
     */
    private void writeErrorResponse(HttpServletResponse response, int status, String message, String errorCode) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        ErrorDetails errorResponse = ErrorDetails.of(errorCode, message);
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}
