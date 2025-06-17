package com.application.spring.requestfilter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.application.security.jwt.JwtUtil;
import com.application.security.user.restaurant.RestaurantUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RestaurantUserRequestFilter extends OncePerRequestFilter {
    
    private RestaurantUserDetailsService userDetailsService;
    private JwtUtil jwtUtil;

    public RestaurantUserRequestFilter(RestaurantUserDetailsService userDetailsService, JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        System.out.println("Filtering request: " + request.getRequestURI());
    
        final String authorizationHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;
    
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            username = jwtUtil.extractUsername(jwt);
            System.out.println("Extracted username: " + username);
            try {
                var claims = jwtUtil.extractAllClaims(jwt);
                String type = (String) claims.get("type");
                String path = request.getRequestURI();

                if ("/restaurant/user/auth/select-restaurant".equals(path)) {
                    // Solo token con type hub sono accettati su questa rotta
                    if (!"hub".equals(type)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("Token must be of type hub for this endpoint.");
                        return;
                    }
                } else {
                    // Su tutte le altre rotte, rifiuta se type è hub
                    if ("hub".equals(type)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("Token of type hub not allowed for this endpoint.");
                        return;
                    }
                }
            } catch (Exception e) {
                // In caso di errore parsing claims, prosegui senza autenticare
                chain.doFilter(request, response);
                return;
            }
        }
    
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("No authentication found in context. Loading user details...");
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            System.out.println("Authorities: " + userDetails.getAuthorities()+"\n\n\n\\n\n\n");

    
            if (jwtUtil.validateToken(jwt, userDetails)) {
                System.out.println("Token validated. Setting authentication...");
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                System.out.println("\n\n\n\nSetting details for authentication token...");
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                System.out.println("Setting authentication in SecurityContextHolder...");
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            } else {
                System.out.println("Token validation failed.");
            }
        }
        System.out.println("Continuing filter chain...");
    
        chain.doFilter(request, response);
    }
}
