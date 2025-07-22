package com.application.restaurant;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.application.common.jwt.JwtUtil;
import com.application.restaurant.service.security.RUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RUserRequestFilter extends OncePerRequestFilter {

    private final RUserDetailsService userDetailsService;
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
        Object claims;
        String tokenType;

        try {
            claims = jwtUtil.extractAllClaims(jwt);
            tokenType = (String) ((java.util.Map<?, ?>) claims).get("type");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid token claims.");
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("[FILTER] No authentication found in context. Loading user details...");
            UserDetails userDetails;
            if ("hub".equals(tokenType)) {
                // Crea un UserDetails custom per hub solo se claims non è null
                String email = null;
                if (claims != null) {
                    email = (String) ((java.util.Map<?,?>)claims).get("email");
                }
                if (email == null) {
                    System.out.println("[FILTER] Email claim is missing for hub token, sending 401");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Email claim missing in hub token.");
                    return;
                }
                userDetails = org.springframework.security.core.userdetails.User
                    .withUsername(email) 
                    .password("") // Nessuna password
                    .authorities("PRIVILEGE_HUB","PRIVILEGE_CHANGE_PASSWORD")
                    .build();
                System.out.println("[FILTER] Created custom UserDetails for hub: " + userDetails.getUsername());
            } else {
                userDetails = this.userDetailsService.loadUserByUsername(username);
                System.out.println("[FILTER] Authorities: " + userDetails.getAuthorities());
            }
            if (jwtUtil.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token validation failed.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui/") ||
               path.startsWith("/favicon.ico") ||
               path.startsWith("/logo_api.png") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/");
    }

    private UserDetails loadUserDetails(String username, String tokenType, Object claims, HttpServletResponse response)
            throws IOException {
        if ("hub".equals(tokenType)) {
            String email = (String) ((java.util.Map<?, ?>) claims).get("email");
            if (email == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Email claim missing in hub token.");
                return null;
            }
            return org.springframework.security.core.userdetails.User //ORRIBILE
                    .withUsername(email)
                    .password("")
                    .authorities("PRIVILEGE_HUB", "PRIVILEGE_CHANGE_PASSWORD")
                    .build();
        } else {
            return userDetailsService.loadUserByUsername(username);
        }
    }
}
