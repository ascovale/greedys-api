package com.application.spring.requestfilter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.application.security.jwt.JwtUtil;
import com.application.security.user.restaurant.RUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RUserRequestFilter extends OncePerRequestFilter {
    
    private RUserDetailsService userDetailsService;
    private JwtUtil jwtUtil;

    public RUserRequestFilter(RUserDetailsService userDetailsService, JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        System.out.println("[FILTER] Filtering request: " + request.getRequestURI());
    
        final String authorizationHeader = request.getHeader("Authorization");
        System.out.println("[FILTER] Authorization header: " + authorizationHeader);
        String username = null;
        String jwt = null;
        Object claims = null; // claims ora visibile ovunque
        String type = null;   // type ora visibile ovunque
    
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            System.out.println("[FILTER] JWT found: " + jwt);
            username = jwtUtil.extractUsername(jwt);
            System.out.println("[FILTER] Extracted username: " + username);
            try {
                claims = jwtUtil.extractAllClaims(jwt);
                type = (String) ((java.util.Map<?,?>)claims).get("type");
                String path = request.getRequestURI();
                System.out.println("[FILTER] Token type: " + type + ", Path: " + path);
                
            } catch (Exception e) {
                System.out.println("[FILTER] Exception during claims parsing: " + e.getMessage());
                // In caso di errore parsing claims, prosegui senza autenticare
                chain.doFilter(request, response);
                return;
            }
        } else {
            System.out.println("[FILTER] No valid Authorization header found");
        }
    
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("[FILTER] No authentication found in context. Loading user details...");
            UserDetails userDetails;
            if ("hub".equals(type)) {
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
                    .withUsername(email) // Use only the email, no ":0"
                    .password("") // Nessuna password
                    .authorities("PRIVILEGE_HUB","PRIVILEGE_CHANGE_PASSWORD")
                    .build();
                System.out.println("[FILTER] Created custom UserDetails for hub: " + userDetails.getUsername());
            } else {
                userDetails = this.userDetailsService.loadUserByUsername(username);
                System.out.println("[FILTER] Authorities: " + userDetails.getAuthorities());
            }
            if (jwtUtil.validateToken(jwt, userDetails)) {
                System.out.println("[FILTER] Token validated. Setting authentication...");
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                System.out.println("[FILTER] Setting details for authentication token...");
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                System.out.println("[FILTER] Setting authentication in SecurityContextHolder...");
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            } else {
                System.out.println("[FILTER] Token validation failed.");
            }
        } else {
            System.out.println("[FILTER] Username is null or authentication already present.");
        }
        System.out.println("[FILTER] Continuing filter chain...");
    
        chain.doFilter(request, response);
    }
}
