package com.application.agency;

import java.io.IOException;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.application.common.security.SecurityPatterns;
import com.application.common.security.jwt.JwtUtil;
import com.application.agency.service.security.AgencyUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AgencyUserRequestFilter extends OncePerRequestFilter {

    private final AgencyUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String jwt = authorizationHeader.substring(7);
        String username = jwtUtil.extractUsername(jwt);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // 🔍 Verifica che il token sia per Agency (user o hub)
            String userType = jwtUtil.extractUserType(jwt);
            if (!"agency-user".equals(userType) && !"agency-user-hub".equals(userType)) {
                // Token non valido per questo filtro - ERRORE 401
                throw new AuthenticationCredentialsNotFoundException("Invalid token type for agency endpoint");
            }
            
            // Il AgencyUserHubValidationFilter ha già gestito la validazione Hub
            // Qui gestiamo sia AgencyUser normali che Hub per l'autenticazione
            
            UserDetails userDetails;
            try {
            if (jwtUtil.isAnyAgencyHubToken(jwt)) {
                // Crea UserDetails custom per agency hub
                Object claims = jwtUtil.extractAllClaims(jwt);
                String email = (String) ((java.util.Map<?,?>)claims).get("email");
                if (email == null) {
                    throw new AuthenticationCredentialsNotFoundException("Email claim missing in agency hub token");
                }
                
                // ✅ Solo access token sono accettati sui filtri di autenticazione
                // I refresh token sono gestiti direttamente nei service degli endpoint pubblici
                if (jwtUtil.isAgencyHubRefreshToken(jwt)) {
                    // Refresh token non dovrebbe essere usato per autenticazione su endpoint protetti
                    throw new AuthenticationCredentialsNotFoundException("Refresh token cannot be used for protected endpoints");
                } else {
                    // Access Agency Hub token: permessi completi
                    userDetails = org.springframework.security.core.userdetails.User
                        .withUsername(email) 
                        .password("") 
                        .authorities("PRIVILEGE_AGENCY_HUB","PRIVILEGE_CHANGE_PASSWORD") // Permessi completi
                        .build();
                }
            } else {
                // 👤 Token AgencyUser normale
                userDetails = this.userDetailsService.loadUserByUsername(username);
                
                // 🔄 Per refresh token, potremmo limitare i permessi (opzionale)
                if (jwtUtil.isRefreshToken(jwt)) {
                    // Refresh token: permessi limitati (opzionale - dipende dai requirements)
                    // In questo caso manteniamo tutti i permessi per compatibilità
                    // ma potremmo creare UserDetails con permessi limitati se necessario
                }
            }
            if (jwtUtil.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            } else {
                throw new AuthenticationCredentialsNotFoundException("Token validation failed");
            }
            } catch (Exception e) {
                throw new AuthenticationCredentialsNotFoundException("Invalid token claims");
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