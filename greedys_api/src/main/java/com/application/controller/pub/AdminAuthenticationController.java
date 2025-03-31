package com.application.controller.pub;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.admin.Admin;
import com.application.security.jwt.JwtUtil;
import com.application.service.AdminService;
import com.application.web.dto.AdminAuthResponseDTO;
import com.application.web.dto.get.AdminDTO;
import com.application.web.dto.post.AuthRequestDTO;
import com.application.web.dto.post.AuthResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin Authentication Controller", description = "Controller per la gestione dell'autenticazione degli utenti Admin")
@RestController
@RequestMapping(value = "/public/admin", produces = "application/json")
public class AdminAuthenticationController {

    private AuthenticationManager authenticationManager;
    private JwtUtil jwtUtil;
    private AdminService adminService;

    public AdminAuthenticationController(@Qualifier("adminAuthenticationManager") AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            AdminService adminService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.adminService = adminService;
    }

    @Operation(summary = "Crea un token di autenticazione", description = "Autentica un utente e restituisce un token JWT", responses = {
            @ApiResponse(responseCode = "200", description = "Autenticazione riuscita", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Autenticazione fallita", content = @Content(mediaType = "application/json"))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Richiesta di autenticazione", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthRequestDTO.class)))
    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<AdminAuthResponseDTO> createAuthenticationToken(@RequestBody AuthRequestDTO authenticationRequest)
            throws Exception {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(),
                        authenticationRequest.getPassword()));

        final Admin userDetails = adminService.findAdminByEmail(authenticationRequest.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);
        final AdminAuthResponseDTO responseDTO = new AdminAuthResponseDTO(jwt, new AdminDTO(userDetails));

        return ResponseEntity.ok(responseDTO);
    }

}