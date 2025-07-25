package com.application.admin.service.authentication;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.application.admin.persistence.model.Admin;
import com.application.admin.service.AdminService;
import com.application.admin.web.dto.get.AdminDTO;
import com.application.common.security.jwt.JwtUtil;
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

            // Create response DTO
            final AuthResponseDTO responseDTO = new AuthResponseDTO(jwt, new AdminDTO(adminDetails));
            return responseDTO;

        
    }
}
