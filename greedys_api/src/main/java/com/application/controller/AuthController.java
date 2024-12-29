package com.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.google.api.client.json.gson.GsonFactory;
import java.util.UUID;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.application.persistence.model.user.User;
import com.application.security.jwt.JwtUtil;
import com.application.security.user.UserUserDetailsService;
import com.application.service.UserService;
import com.application.web.dto.AuthRequestDTO;
import com.application.web.dto.AuthRequestGoogleDTO;
import com.application.web.dto.AuthResponseDTO;
import com.application.web.dto.get.UserDTO;
import com.application.web.dto.post.NewUserDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag(name = "Auth", description = "Controller per la gestione dell'autenticazione")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private AuthenticationManager authenticationManager;
    private JwtUtil jwtUtil;
    private UserService userService;

    public AuthController(AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Operation(summary = "Crea un token di autenticazione", description = "Autentica un utente e restituisce un token JWT", responses = {
            @ApiResponse(responseCode = "200", description = "Autenticazione riuscita", content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Autenticazione fallita")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Richiesta di autenticazione", required = true, content = @Content(schema = @Schema(implementation = AuthRequestDTO.class)))
    @PostMapping("/user")
    public ResponseEntity<AuthResponseDTO> createAuthenticationToken(@RequestBody AuthRequestDTO authenticationRequest)
            throws Exception {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(),
                        authenticationRequest.getPassword()));

        final User userDetails = userService.findUserByEmail(authenticationRequest.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);
        final AuthResponseDTO responseDTO = new AuthResponseDTO(jwt, new UserDTO(userDetails));

        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Autentica con Google", description = "Autentica un utente utilizzando un token di Google e restituisce un token JWT", responses = {
            @ApiResponse(responseCode = "200", description = "Autenticazione riuscita", content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Autenticazione fallita")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Richiesta di autenticazione con Google", required = true, content = @Content(schema = @Schema(implementation = AuthRequestGoogleDTO.class)))
    @PostMapping("/google")
    public ResponseEntity<AuthResponseDTO> authenticateWithGoogle(@RequestBody AuthRequestGoogleDTO authRequest)
            throws Exception {
        logger.warn("Received Google authentication request: {}", authRequest.getToken());
        GoogleIdToken idToken = verifyGoogleToken(authRequest.getToken());

        if (idToken != null) {
            String email = idToken.getPayload().getEmail();
            String name = (String) idToken.getPayload().get("name");
            logger.warn("Google token verified. Email: {}, Name: {}", email, name);
            // quali dati vogliamo prendere da google?
            User user = userService.findUserByEmail(email);
            if (user == null) {
                NewUserDTO accountDto = new NewUserDTO();
                // devo verificare questa cosa
                accountDto.setFirstName(name.split(" ")[0]);
                accountDto.setLastName(name.split(" ")[1]);
                accountDto.setEmail(email);
                accountDto.setPassword(generateRandomPassword()); // Generate and set a random password
                user = userService.registerNewUserAccount(accountDto);
            }
            String jwt = jwtUtil.generateToken(user);
            return ResponseEntity.ok(new AuthResponseDTO(jwt, new UserDTO(user)));
        } else {
            logger.warn("Google token verification failed.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private GoogleIdToken verifyGoogleToken(String token) throws Exception {
        try {
            
            logger.debug("Verifying Google token: {}", token);
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections
                            .singletonList("982346813437-3s1uepb5ic7ib5r4mfegdsbrkjjvtl7b.apps.googleusercontent.com"))
                    .build();
            GoogleIdToken idToken = verifier.verify(token);
            if (idToken != null) {
                logger.debug("Google token verified successfully.");
            } else {
                logger.warn("Google token verification failed.");
            }
            return idToken;
        } catch (GeneralSecurityException e) {
            logger.error("Google token verification failed: GeneralSecurityException", e);
        } catch (IOException e) {
            logger.error("Google token verification failed: IOException", e);
        }
        return null;
    }

    private String generateRandomPassword() {
        // Implement a method to generate a random password
        return UUID.randomUUID().toString();
    }
}