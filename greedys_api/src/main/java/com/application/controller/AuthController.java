package com.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.application.persistence.model.user.User;
import com.application.security.jwt.JwtUtil;
import com.application.security.user.UserUserDetailsService;
import com.application.service.UserService;
import com.application.web.dto.AuthRequestDTO;
import com.application.web.dto.AuthResponseDTO;
import com.application.web.dto.get.UserDTO;

@Tag(name = "Auth", description = "Controller per la gestione dell'autenticazione")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private AuthenticationManager authenticationManager;
    private JwtUtil jwtUtil;
    private UserService userDetailsService;

    public AuthController(AuthenticationManager authenticationManager, 
                          JwtUtil jwtUtil, 
                          UserService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Operation(summary = "Crea un token di autenticazione",
               description = "Autentica un utente e restituisce un token JWT",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Autenticazione riuscita",
                                content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
                   @ApiResponse(responseCode = "401", description = "Autenticazione fallita")
               })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Richiesta di autenticazione",
        required = true,
        content = @Content(
            schema = @Schema(
                implementation = AuthRequestDTO.class
            )
        )
    )
    @PostMapping("/user")
    public ResponseEntity<AuthResponseDTO> createAuthenticationToken(@RequestBody AuthRequestDTO authenticationRequest) throws Exception {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword())
        );

        final User userDetails = userDetailsService.findUserByEmail(authenticationRequest.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);
        final AuthResponseDTO responseDTO = new AuthResponseDTO(jwt, new UserDTO(userDetails));

        return ResponseEntity.ok(responseDTO);
    }
}