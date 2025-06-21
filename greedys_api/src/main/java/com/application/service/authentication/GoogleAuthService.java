package com.application.service.authentication;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.application.controller.customer.CustomerAuthenticationController;
import com.application.persistence.model.customer.Customer;
import com.application.security.jwt.JwtUtil;
import com.application.web.dto.AuthRequestGoogleDTO;
import com.application.web.dto.get.CustomerDTO;
import com.application.web.dto.post.AuthResponseDTO;
import com.application.web.dto.post.NewCustomerDTO;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Service
public class GoogleAuthService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthService.class);
    private JwtUtil jwtUtil;

    public GoogleAuthService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }


    public <U> ResponseEntity<AuthResponseDTO> authenticateWithGoogle(
            AuthRequestGoogleDTO authRequest, 
            Function<String, U> userFun, 
            BiFunction<String, GoogleIdToken, U> createUserFun,
            Function<U, String> genToken)
            throws Exception {
        logger.warn("Received Google authentication request: {}", authRequest.getToken());
        GoogleIdToken idToken = verifyGoogleToken(authRequest.getToken());

        if (idToken != null) {
            String email = idToken.getPayload().getEmail();
            String name = (String) idToken.getPayload().get("name");
            logger.warn("Google token verified. Email: {}, Name: {}", email, name);
            // quali dati vogliamo prendere da google?
            U customer = userFun.apply(email);
            if (customer == null) {
                customer = createUserFun.apply(email, idToken);
            }
            String jwt = genToken.apply(customer);
            return ResponseEntity.ok(new AuthResponseDTO(jwt, customer));
        } else {
            logger.warn("Google token verification failed.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private GoogleIdToken verifyGoogleToken(String token) throws Exception {
        try {
            logger.debug("Verifying Google token... {}", token);
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Arrays.asList(
                            "982346813437-3s1uepb5ic7ib5r4mfegdsbrkjjvtl7b.apps.googleusercontent.com", // Web client ID
                                                                                                        // (API)
                            "982346813437-d0kerhe6h2km0veqs563avsgtv6vb7p5.apps.googleusercontent.com", // Flutter Web
                            "982346813437-e1vsuujvorosiaamfdc3honrrbur17ri.apps.googleusercontent.com", // Android
                            "982346813437-iosclientid.apps.googleusercontent.com" // TODO: Inserire il token per Ios
                    ))
                    .build();

            GoogleIdToken idToken = verifier.verify(token);
            if (idToken != null) {
                logger.debug("Google token verified successfully.");
            } else {
                logger.warn("Google token verification failed: Invalid token.");
            }
            return idToken;
        } catch (GeneralSecurityException e) {
            logger.error("Google token verification failed: GeneralSecurityException - {}", e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Google token verification failed: IOException - {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Google token verification failed: Exception - {}", e.getMessage(), e);
        }
        return null;
    }

}
