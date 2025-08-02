package com.application.common.service.authentication;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.application.common.web.dto.security.AuthRequestGoogleDTO;
import com.application.common.web.dto.security.AuthResponseDTO;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {
    
    // Client IDs per la verifica ID Token
    @Value("${google.oauth.web.client.id}")
    private String webClientId;
    
    @Value("${google.oauth.flutter.client.id}")
    private String flutterClientId;
    
    @Value("${google.oauth.android.client.id}")
    private String androidClientId;
    
    @Value("${google.oauth.ios.client.id}")
    private String iosClientId;

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
        log.warn("Received Google authentication request: token hash={}", authRequest.getToken() != null ? authRequest.getToken().hashCode() : "null");
        GoogleIdToken idToken = verifyGoogleToken(authRequest.getToken());

        if (idToken != null) {
            String email = idToken.getPayload().getEmail();
            String name = (String) idToken.getPayload().get("name");
            log.warn("Google token verified. Email: {}, Name: {}", email, name);
            // which data do we want to retrieve from Google?
            U customer = userFun.apply(email);
            if (customer == null) {
                customer = createUserFun.apply(email, idToken);
            }
            String jwt = genToken.apply(customer);
            return new AuthResponseDTO(jwt, customer);
        } else {
            log.warn("Google token verification failed.");
            throw new Exception("Google token verification failed.");
        }
    }

    private GoogleIdToken verifyGoogleToken(String token) throws Exception {
        try {
            log.debug("Verifying Google token... {}", token);
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Arrays.asList(
                            webClientId,     // Web client ID (API)
                            flutterClientId, // Flutter Web
                            androidClientId, // Android
                            iosClientId      // iOS
                    ))
                    .build();

            GoogleIdToken idToken = verifier.verify(token);
            if (idToken != null) {
                log.debug("Google token verified successfully.");
            } else {
                log.warn("Google token verification failed: Invalid token.");
            }
            return idToken;
        } catch (GeneralSecurityException e) {
            log.error("Google token verification failed: GeneralSecurityException - {}", e.getMessage(), e);
        } catch (IOException e) {
            log.error("Google token verification failed: IOException - {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Google token verification failed: Exception - {}", e.getMessage(), e);
        }
        return null;
    }

}
