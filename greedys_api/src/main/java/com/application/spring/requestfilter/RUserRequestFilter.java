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

    private final RUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    public RUserRequestFilter(RUserDetailsService userDetailsService, JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

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
            UserDetails userDetails = loadUserDetails(username, tokenType, claims, response);
            if (userDetails == null) {
                return; // Response already handled in loadUserDetails
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
