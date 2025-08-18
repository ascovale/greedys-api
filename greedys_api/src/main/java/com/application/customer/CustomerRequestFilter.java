package com.application.customer;

import java.io.IOException;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import com.application.common.security.SecurityPatterns;
import com.application.common.security.jwt.JwtUtil;
import com.application.customer.service.security.CustomerUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerRequestFilter extends OncePerRequestFilter {

    private final CustomerUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            username = jwtUtil.extractUsername(jwt);
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // 🔍 Verifica che il token sia per Customer
            String userType = jwtUtil.extractUserType(jwt);
            if (!"customer".equals(userType)) {
                // Token non valido per questo filtro - ERRORE 401
                throw new AuthenticationCredentialsNotFoundException("Invalid token type for customer endpoint");
            }
            
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails)) {
                
                // ✅ Solo access token sono accettati sui filtri di autenticazione
                // I refresh token sono gestiti direttamente nei service degli endpoint pubblici
                if (jwtUtil.isRefreshToken(jwt)) {
                    // Refresh token non dovrebbe essere usato per autenticazione su endpoint protetti
                    throw new AuthenticationCredentialsNotFoundException("Refresh token cannot be used for protected endpoints");
                }

                // Access token: mantieni tutte le authorities originali dal DB
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Usa SecurityPatterns per verificare se l'endpoint è pubblico (include risorse statiche)
        return SecurityPatterns.isPublicPath(path);
    }
}
