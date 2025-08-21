package com.application.admin.service.authentication;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.application.admin.persistence.model.Admin;
import com.application.admin.service.AdminService;
import com.application.admin.web.dto.admin.AdminDTO;
import com.application.common.security.jwt.JwtUtil;
import com.application.common.web.dto.security.AuthRequestDTO;
import com.application.common.web.dto.security.AuthResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AdminAuthenticationService {

    private final AdminAuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AdminService adminService;

    /**
     * Authenticates an admin user and returns a JWT token
     * 
     * @param authenticationRequest The authentication request containing username and password
     * @return ResponseEntity with AuthResponseDTO containing JWT token and admin details, or error response
     */
    public AuthResponseDTO login(AuthRequestDTO authenticationRequest) {
        
        log.debug("Admin authentication request received for username: {}", authenticationRequest.getUsername());
        
        // Authenticate the user using Spring Security's AuthenticationManager
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authenticationRequest.getUsername(),
                        authenticationRequest.getPassword()));

        log.debug("Admin authentication successful for username: {}", authenticationRequest.getUsername());

        // Find the admin user by email
        final Admin adminDetails = adminService.findAdminByEmail(authenticationRequest.getUsername());
        if (adminDetails == null) {
            log.error("Admin not found for username: {}", authenticationRequest.getUsername());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // Generate JWT token
        final String jwt = jwtUtil.generateToken(adminDetails);
        log.debug("JWT generated for admin username: {}", authenticationRequest.getUsername());

        // Create response DTO with conditional refresh token
        if (authenticationRequest.isRememberMe()) {
            final String refreshToken = jwtUtil.generateRefreshToken(adminDetails);
            log.debug("Remember me enabled: refresh token generated for admin: {}", authenticationRequest.getUsername());
            
            return AuthResponseDTO.builder()
                    .jwt(jwt)
                    .refreshToken(refreshToken)
                    .user(new AdminDTO(adminDetails))
                    .build();
        } else {
            return new AuthResponseDTO(jwt, new AdminDTO(adminDetails));
        }
    }

    /**
     * Refreshes an admin JWT token using a refresh token
     *
     * @param refreshToken The refresh token to validate and use for generating new tokens
     * @return AuthResponseDTO containing new JWT and refresh tokens
     * @throws SecurityException if the refresh token is invalid or expired
     */
    public AuthResponseDTO refreshToken(String refreshToken) {
        log.debug("Admin refresh token request received");
        
        try {
            // Verifica che sia un refresh token valido
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                throw new SecurityException("Invalid refresh token type");
            }
            
            // Estrae l'username dal refresh token
            String username = jwtUtil.extractUsername(refreshToken);
            
            // Trova l'admin
            final Admin adminDetails = adminService.findAdminByEmail(username);
            if (adminDetails == null) {
                log.warn("No admin found with username from refresh token: {}", username);
                throw new SecurityException("Admin not found");
            }
            
            // Verifica il refresh token
            if (!jwtUtil.validateToken(refreshToken, adminDetails)) {
                throw new SecurityException("Invalid or expired refresh token");
            }
            
            // Genera nuovi token
            final String newJwt = jwtUtil.generateToken(adminDetails);
            final String newRefreshToken = jwtUtil.generateRefreshToken(adminDetails);
            
            log.debug("New admin tokens generated for username: {}", username);
            
            return AuthResponseDTO.builder()
                    .jwt(newJwt)
                    .refreshToken(newRefreshToken)
                    .user(new AdminDTO(adminDetails))
                    .build();
                    
        } catch (Exception e) {
            log.error("Admin refresh token validation failed: {}", e.getMessage());
            throw new SecurityException("Invalid refresh token");
        }
    }
}
