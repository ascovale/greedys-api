package com.application.common.service.authentication;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.application.common.jwt.JwtUtil;
import com.application.common.web.dto.AuthRequestGoogleDTO;
import com.application.common.web.dto.post.AuthResponseDTO;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@Service
public class GoogleAuthService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthService.class);
    private JwtUtil jwtUtil;

    public GoogleAuthService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Authenticates a user with Google OAuth2 and returns an authentication response.
     *
     * @param authRequest the authentication request containing the Google token
     * @param userFun function to retrieve user details by email
     * @param createUserFun function to create a new user if not found
     * @param genToken function to generate JWT token for the authenticated user
     * @return AuthResponseDTO containing JWT and user details
     * @throws Exception if token verification fails or any other error occurs
     */
    public <U> AuthResponseDTO authenticateWithGoogle(
            AuthRequestGoogleDTO authRequest, 
            Function<String, U> userFun, 
            BiFunction<String, GoogleIdToken, U> createUserFun,
            Function<U, String> genToken)
            throws Exception {
        logger.warn("Received Google authentication request: token hash={}", authRequest.getToken() != null ? authRequest.getToken().hashCode() : "null");
        GoogleIdToken idToken = verifyGoogleToken(authRequest.getToken());

        if (idToken != null) {
            String email = idToken.getPayload().getEmail();
            String name = (String) idToken.getPayload().get("name");
            logger.warn("Google token verified. Email: {}, Name: {}", email, name);
            // which data do we want to retrieve from Google?
            U customer = userFun.apply(email);
            if (customer == null) {
                customer = createUserFun.apply(email, idToken);
            }
            String jwt = genToken.apply(customer);
            return new AuthResponseDTO(jwt, customer);
        } else {
            logger.warn("Google token verification failed.");
            throw new Exception("Google token verification failed.");
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
