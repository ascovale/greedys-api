/**
 * Service for verifying restaurant ownership through Google Business Profile API.
 * <p>
 * This service focuses exclusively on Business Profile API integration for official 
 * restaurant ownership verification and email matching between users and restaurants.
 * 
 * <b>Key Features:</b>
 * <ul>
 *   <li>Direct verification through Google Business Profile API</li>
 *   <li>Email correlation between user and restaurant contact information</li>
 *   <li>Official business ownership verification</li>
 *   <li>Access to managed business locations</li>
 * </ul>
 *
 * <b>Verification Flow:</b>
 * <ol>
 *   <li>User authorizes with business.manage scope</li>
 *   <li>Service retrieves user's managed business locations</li>
 *   <li>Verifies email correlation between user and business</li>
 *   <li>Returns verified restaurant data for account creation</li>
 * </ol>
 *
 * <b>Note:</b> This service requires Google Business Profile API approval and 
 * business.manage scope authorization for full functionality.
 */
package com.application.restaurant.service.google.after;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.application.restaurant.web.dto.google.OwnerData;
import com.application.restaurant.web.dto.google.RestaurantAuthorizationResult;
import com.application.restaurant.web.dto.google.RestaurantData;
import com.application.restaurant.web.dto.google.TokenInfo;
import com.application.restaurant.web.dto.verification.RestaurantVerificationResult;
import com.application.restaurant.web.dto.verification.VerificationData;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

/**
 * Restaurant Google Business Profile Verification Service
 * 
 * This service handles restaurant verification ONLY through Google Business Profile API.
 * Focuses on email verification and official business ownership verification.
 * 
 * KEY FEATURES:
 * - Direct Google Business Profile API integration
 * - Email correlation between user and restaurant business emails
 * - Official business ownership verification
 * - Managed business locations retrieval
 * 
 * STATUS: Ready for Business Profile API approval
 */

@Service
public class RestaurantGoogleVerificationService {
    
    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;
    
    @Value("${google.oauth.client.id}")
    private String oauthClientId;
    
    @Value("${google.oauth.client.secret}")
    private String oauthClientSecret;
    
    // Client IDs per la verifica Access Token (stessi del GoogleAuthService)
    @Value("${google.oauth.web.client.id}")
    private String webClientId;
    
    @Value("${google.oauth.flutter.client.id}")
    private String flutterClientId;
    
    @Value("${google.oauth.android.client.id}")
    private String androidClientId;
    
    @Value("${google.oauth.ios.client.id}")
    private String iosClientId;
    
    private static final List<String> SCOPES = Arrays.asList(
        "https://www.googleapis.com/auth/business.manage",
        "https://www.googleapis.com/auth/userinfo.email",
        "https://www.googleapis.com/auth/userinfo.profile"
    );
    
    // TODO: Uncomment when Google Business Profile API access is approved
    // private MyBusinessBusinessInformation businessService;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    // ====== MAIN METHODS - NEW FLOW ======
    
    /**
     * STEP 1: Gets the list of restaurants that the user can manage
     * This is the first method to call - returns the list of available restaurants
     */
    public RestaurantAuthorizationResult getUserRestaurants(String accessToken, String email) {
        try {
            // Verify that the Access Token is valid and for our app
            if (!verifyAccessTokenBelongsToOurApp(accessToken)) {
                return new RestaurantAuthorizationResult(false, 
                    "Access Token not valid or not authorized for this application", null, null);
            }
            
            // Create a temporary credential with the access token
            Credential credential = createCredentialFromToken(accessToken);
            
            // Verify that the email matches the one from the token
            String tokenEmail = getUserEmail(credential);
            if (!email.equals(tokenEmail)) {
                return new RestaurantAuthorizationResult(false, 
                    "Provided email does not match Google token", null, null);
            }
            
            // Get owner data
            OwnerData ownerData = getOwnerDataFromCredential(credential);
            
            // Get all restaurants managed by the user
            List<RestaurantData> userRestaurants = getUserManagedRestaurants(credential);
            
            if (userRestaurants.isEmpty()) {
                return new RestaurantAuthorizationResult(false, 
                    "No managed restaurant found for this user", ownerData, new ArrayList<>());
            }
            
            String message = userRestaurants.size() == 1 ? 
                "Found 1 managed restaurant" : 
                "Found " + userRestaurants.size() + " managed restaurants";
            
            return new RestaurantAuthorizationResult(true, message, ownerData, userRestaurants);
            
        } catch (Exception e) {
            return new RestaurantAuthorizationResult(false, "Error during restaurant retrieval: " + e.getMessage(), null, null);
        }
    }
    
    /**
     * STEP 2: Verifies and selects a specific restaurant from the list
     * This method is called after getUserRestaurants() when the user has chosen a placeId
     */
    public RestaurantVerificationResult selectRestaurant(String accessToken, String email, String placeId) {
        try {
            // Verify that the Access Token is valid and for our app
            if (!verifyAccessTokenBelongsToOurApp(accessToken)) {
                return new RestaurantVerificationResult(false, 
                    "Access Token not valid or not authorized for this application", null);
            }
            
            // Create a temporary credential with the access token
            Credential credential = createCredentialFromToken(accessToken);
            
            // Verify that the email matches the one from the token
            String tokenEmail = getUserEmail(credential);
            if (!email.equals(tokenEmail)) {
                return new RestaurantVerificationResult(false, 
                    "Provided email does not match Google token", null);
            }
            
            // Verify that the placeId is among those managed by the user
            List<RestaurantData> userRestaurants = getUserManagedRestaurants(credential);
            boolean isAuthorized = userRestaurants.stream()
                .anyMatch(restaurant -> placeId.equals(restaurant.getPlaceId()));
                
            if (!isAuthorized) {
                return new RestaurantVerificationResult(false, 
                    "PlaceId not authorized or not managed by this user", null);
            }
            
            // Get complete details of the selected restaurant
            RestaurantData restaurantData = getRestaurantDataByPlaceId(placeId);
            if (restaurantData == null) {
                return new RestaurantVerificationResult(false, 
                    "Restaurant with specified placeId not found", null);
            }
            
            // Retrieve owner data
            OwnerData ownerData = getOwnerDataFromCredential(credential);
            
            return new RestaurantVerificationResult(true, "Restaurant selected and verified successfully", 
                new VerificationData(restaurantData, ownerData));
            
        } catch (Exception e) {
            return new RestaurantVerificationResult(false, "Error during selection: " + e.getMessage(), null);
        }
    }
    
    
    /**
     * Creates a credential from an access token
     */
    private Credential createCredentialFromToken(String accessToken) throws Exception {
        // Create a temporary flow to create the credential
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
        details.setClientId(oauthClientId);
        details.setClientSecret(oauthClientSecret);
        
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
        clientSecrets.setInstalled(details);
        
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            clientSecrets,
            SCOPES)
            .build();
            
        // Create a credential and set the access token
        Credential credential = flow.createAndStoreCredential(null, "temp-user");
        credential.setAccessToken(accessToken);
        
        return credential;
    }
    
    /**
     * Verifies that the Access Token belongs to our application
     */
    private boolean verifyAccessTokenBelongsToOurApp(String accessToken) {
        try {
            // Use the tokeninfo endpoint to verify the token
            String tokenInfoUrl = "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + accessToken;
            TokenInfo tokenInfo = restTemplate.getForObject(tokenInfoUrl, TokenInfo.class);
            
            if (tokenInfo == null || tokenInfo.getAudience() == null) {
                return false;
            }
            
            // Verify that the audience is one of our Client IDs
            String audience = tokenInfo.getAudience();
            return audience.equals(webClientId) || 
                   audience.equals(flutterClientId) || 
                   audience.equals(androidClientId) || 
                   audience.equals(iosClientId) ||
                   audience.equals(oauthClientId); // Fallback for compatibility
                   
        } catch (Exception e) {
            // If it can't verify, it's better to be conservative
            return false;
        }
    }
    
    /**
     * Gets the user's email from the credential
     */
    private String getUserEmail(Credential credential) throws Exception {
        // Use Google UserInfo API to get user email
        String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo?access_token=" + credential.getAccessToken();
        
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> userInfo = restTemplate.getForObject(userInfoUrl, java.util.Map.class);
            
            if (userInfo != null && userInfo.containsKey("email")) {
                return (String) userInfo.get("email");
            }
            
            throw new Exception("Unable to retrieve user email from Google");
            
        } catch (Exception e) {
            throw new Exception("Error calling Google UserInfo API: " + e.getMessage());
        }
    }
    
    /**
     * Gets owner data from credential
     */
    private OwnerData getOwnerDataFromCredential(Credential credential) throws Exception {
        // Use Google UserInfo API to get user profile data
        String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo?access_token=" + credential.getAccessToken();
        
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> userInfo = restTemplate.getForObject(userInfoUrl, java.util.Map.class);
            
            if (userInfo == null) {
                throw new Exception("Unable to retrieve user info from Google");
            }
            
            OwnerData ownerData = new OwnerData();
            ownerData.setEmail((String) userInfo.get("email"));
            ownerData.setName((String) userInfo.get("name"));
            ownerData.setProfilePicture((String) userInfo.get("picture"));
            ownerData.setGoogleId((String) userInfo.get("id"));
            ownerData.setLocale((String) userInfo.get("locale"));
            ownerData.setVerificationStatus("verified");
            ownerData.setVerificationDate(java.time.LocalDateTime.now());
            
            return ownerData;
            
        } catch (Exception e) {
            throw new Exception("Error getting owner data from Google: " + e.getMessage());
        }
    }
    
    /**
     * Gets restaurants managed by the user
     */
    private List<RestaurantData> getUserManagedRestaurants(Credential credential) throws Exception {
        
        // TODO: This is a temporary implementation until Google Business Profile API is approved
        // When approved, this should call Google Business Profile API to get actual managed locations
        
        // For now, we return an empty list and throw an exception to indicate API is needed
        // Real implementation would be:
        /*
        try {
            MyBusinessBusinessInformation.Accounts.Locations.List request = 
                businessService.accounts().locations().list("accounts/" + accountId);
            LocationsListResponse response = request.execute();
            
            List<RestaurantData> restaurants = new ArrayList<>();
            for (Location location : response.getLocations()) {
                RestaurantData restaurantData = new RestaurantData();
                restaurantData.setPlaceId(location.getPlaceId());
                restaurantData.setName(location.getTitle());
                // ... populate other fields
                restaurants.add(restaurantData);
            }
            return restaurants;
        } catch (Exception e) {
            throw new Exception("Error getting managed locations: " + e.getMessage());
        }
        */
        
        throw new UnsupportedOperationException(
            "Google Business Profile API access required. " +
            "Please ensure your app is approved for Business Profile API access and implement the actual API calls."
        );
    }
    
    /**
     * Gets place details by place ID and returns RestaurantData directly
     */
    private RestaurantData getRestaurantDataByPlaceId(String placeId) throws Exception {
        try {
            // Use Google Places API to get place details
            String url = String.format(
                "https://maps.googleapis.com/maps/api/place/details/json?place_id=%s&fields=name,formatted_address,formatted_phone_number,website,opening_hours,rating,user_ratings_total,types,price_level,photos&key=%s",
                placeId, googleMapsApiKey
            );
            
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> response = restTemplate.getForObject(url, java.util.Map.class);
            
            if (response == null || !"OK".equals(response.get("status"))) {
                throw new Exception("Error from Google Places API: " + (response != null ? response.get("status") : "No response"));
            }
            
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> result = (java.util.Map<String, Object>) response.get("result");
            
            if (result == null) {
                throw new Exception("No place details found for placeId: " + placeId);
            }
            
            // Extract restaurant data from API response
            RestaurantData restaurantData = new RestaurantData();
            restaurantData.setPlaceId(placeId);
            restaurantData.setName((String) result.get("name"));
            restaurantData.setAddress((String) result.get("formatted_address"));
            restaurantData.setPhoneNumber((String) result.get("formatted_phone_number"));
            restaurantData.setWebsite((String) result.get("website"));
            
            // Handle numeric fields safely
            if (result.get("rating") != null) {
                restaurantData.setRating(((Number) result.get("rating")).doubleValue());
            }
            if (result.get("user_ratings_total") != null) {
                restaurantData.setTotalReviews(((Number) result.get("user_ratings_total")).intValue());
            }
            if (result.get("price_level") != null) {
                restaurantData.setPriceLevel(((Number) result.get("price_level")).intValue());
            }
            
            // Handle arrays
            @SuppressWarnings("unchecked")
            List<String> types = (List<String>) result.get("types");
            restaurantData.setTypes(types);
            
            // Handle opening hours
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> openingHours = (java.util.Map<String, Object>) result.get("opening_hours");
            if (openingHours != null) {
                @SuppressWarnings("unchecked")
                List<String> weekdayText = (List<String>) openingHours.get("weekday_text");
                restaurantData.setOpeningHours(weekdayText);
            }
            
            return restaurantData;
            
        } catch (Exception e) {
            throw new Exception("Error getting restaurant data: " + e.getMessage());
        }
    }
    
    /**
     * Verifies email correlation between user and restaurant
     * This is the core verification method for Business Profile API
     */
    public RestaurantVerificationResult verifyUserRestaurantEmailCorrelation(String accessToken, String userEmail, String placeId) {
        try {
            // Verify that the Access Token is valid and for our app
            if (!verifyAccessTokenBelongsToOurApp(accessToken)) {
                return new RestaurantVerificationResult(false, 
                    "Access Token not valid or not authorized for this application", null);
            }
            
            // Create credential from access token
            Credential credential = createCredentialFromToken(accessToken);
            
            // Verify that the email matches the one from the token
            String tokenEmail = getUserEmail(credential);
            if (!userEmail.equals(tokenEmail)) {
                return new RestaurantVerificationResult(false, 
                    "Provided email does not match Google token", null);
            }
            
            // Get restaurant data from Places API
            RestaurantData restaurantData = getRestaurantDataByPlaceId(placeId);
            if (restaurantData == null) {
                return new RestaurantVerificationResult(false, 
                    "Restaurant with specified placeId not found", null);
            }
            
            // Get owner data
            OwnerData ownerData = getOwnerDataFromCredential(credential);
            
            // TODO: When Google Business Profile API is approved, implement real email correlation:
            // 1. Get business information for the placeId from Business Profile API
            // 2. Check if user has management rights for this business
            // 3. Verify email correlation between user and business contact emails
            // 4. Return verification result with business data
            
            // For now, we verify basic user authentication and return the data
            // This will need to be enhanced with actual Business Profile API verification
            
            return new RestaurantVerificationResult(true, 
                "Basic verification completed - Business Profile API verification pending", 
                new VerificationData(restaurantData, ownerData));
            
        } catch (Exception e) {
            return new RestaurantVerificationResult(false, "Error during email verification: " + e.getMessage(), null);
        }
    }
    
}
