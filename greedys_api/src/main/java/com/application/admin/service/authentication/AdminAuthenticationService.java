package com.application.admin.service.authentication;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.model.Admin;
import com.application.admin.service.AdminService;
import com.application.common.jwt.JwtUtil;
import com.application.common.web.dto.get.AdminDTO;
import com.application.common.web.dto.post.AuthRequestDTO;
import com.application.common.web.dto.post.AuthResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdminAuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AdminService adminService;

    /**
     * Authenticates an admin user and returns a JWT token
     * 
     * @param authenticationRequest The authentication request containing username and password
     * @return ResponseEntity with AuthResponseDTO containing JWT token and admin details, or error response
     */
    public ResponseEntity<?> login(AuthRequestDTO authenticationRequest) {
        
        log.debug("Admin authentication request received for username: {}", authenticationRequest.getUsername());

        try {
            // Authenticate the user using Spring Security's AuthenticationManager
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.getUsername(),
                            authenticationRequest.getPassword()));

            log.debug("Admin authentication successful for username: {}", authenticationRequest.getUsername());

            // Find the admin user by email
            final Admin adminDetails = adminService.findAdminByEmail(authenticationRequest.getUsername());
            if (adminDetails == null) {
                log.warn("No admin found with email: {}", authenticationRequest.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }

            // Generate JWT token
            final String jwt = jwtUtil.generateToken(adminDetails);
            log.debug("JWT generated for admin username: {}", authenticationRequest.getUsername());

            // Create response DTO
            final AuthResponseDTO responseDTO = new AuthResponseDTO(jwt, new AdminDTO(adminDetails));
            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            log.error("Authentication failed for admin username: {}", authenticationRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }
}
