package com.application.controller.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.customer.Customer;
import com.application.security.jwt.JwtUtil;
import com.application.service.CustomerService;
import com.application.web.dto.get.CustomerDTO;
import com.application.web.dto.post.AuthRequestDTO;
import com.application.web.dto.post.AuthResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "1. Authentication", description = "Controller for managing customer authentication")
@RequestMapping("/customer/auth")
public class CustomerAuthenticationController {
    private CustomerService customerService;
    private AuthenticationManager authenticationManager;
    private JwtUtil jwtUtil;

    private static final Logger logger = LoggerFactory.getLogger(CustomerAuthenticationController.class);

    @Autowired
    public CustomerAuthenticationController(CustomerService customerService,
            @Qualifier("customerAuthenticationManager") AuthenticationManager authenticationManager,
            JwtUtil jwtUtil) {
        this.customerService = customerService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "Create an authentication token", description = "Authenticates a customer and returns a JWT token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(mediaType = "application/json"))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Authentication request", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthRequestDTO.class)))
    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<?> createAuthenticationToken(
            @RequestBody AuthRequestDTO authenticationRequest) {

        logger.debug("Authentication request received for username: {}", authenticationRequest.getUsername());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(),
                            authenticationRequest.getPassword()));

            logger.debug("Authentication successful for username: {}", authenticationRequest.getUsername());

            final Customer customerDetails = customerService.findCustomerByEmail(authenticationRequest.getUsername());
            if (customerDetails == null) {
                logger.warn("No customer found with email: {}", authenticationRequest.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }

            final String jwt = jwtUtil.generateToken(customerDetails);
            logger.debug("JWT generated for username: {}", authenticationRequest.getUsername());

            final AuthResponseDTO responseDTO = new AuthResponseDTO(jwt, new CustomerDTO(customerDetails));
            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            logger.error("Authentication failed for username: {}. Error: {}", authenticationRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed");
        }
    }

}
