package com.application.common.security.jwt;

import java.time.Clock;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.application.agency.persistence.model.user.AgencyUserHub;
import com.application.common.service.SecretManager;
import com.application.restaurant.persistence.model.user.RUserHub;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh.expiration:604800000}") // 7 giorni default per refresh token
    private Long refreshExpiration;

    private final SecretManager secretManager;
    private SecretKey key;
    private Clock clock = Clock.systemDefaultZone();

    public JwtUtil(SecretManager secretManager) {
        this.secretManager = secretManager;
    }

    @PostConstruct
    public void init() {
        try {
            String secretValue = secretManager.readSecret("jwt_secret", "jwt.secret");
            log.info("🔐 JWT Secret letto con modalità: {} - Valore inizia con: {}...", 
                     secretManager.getExecutionMode(), 
                     secretValue.length() > 10 ? secretValue.substring(0, 10) : secretValue);
            
            byte[] keyBytes = Decoders.BASE64.decode(secretValue);
            if (keyBytes.length < 32) {
                throw new IllegalArgumentException("JWT secret key is too short, must be at least 256 bits");
            }
            this.key = Keys.hmacShaKeyFor(keyBytes);
            log.info("✅ JWT Key inizializzata correttamente (lunghezza: {} bytes)", keyBytes.length);
        } catch (Exception e) {
            log.error("❌ Errore nell'inizializzazione del JWT secret: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize JWT secret", e);
        }
    }
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                                 .verifyWith(key)
                                 .build()
                                 .parseSignedClaims(token);
            return jws.getPayload();
        } catch (JwtException e) {
            throw new SecurityException("Invalid JWT token", e);
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(Date.from(clock.instant()));
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", userDetails.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toList()));
        claims.put("access_type", "access");
        claims.put("user_type", determineUserType(userDetails));
        
        // Add user_id, restaurantId or agencyId if available
        addUserAndOrganizationIdsToClaims(claims, userDetails);
        
        return createToken(claims, userDetails.getUsername(), expiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "ruser");  // Tipo di token per distinguerlo da hub
        claims.put("access_type", "refresh");
        claims.put("user_type", determineUserType(userDetails));
        claims.put("email", userDetails.getUsername());
        claims.put("authorities", List.of("PRIVILEGE_REFRESH_ONLY")); // Solo permesso di refresh
        return createToken(claims, userDetails.getUsername(), refreshExpiration);
    }

    /**
     * Determina il tipo di utente basandosi sulla classe dell'oggetto UserDetails
     */
    private String determineUserType(UserDetails userDetails) {
        String className = userDetails.getClass().getSimpleName();
        switch (className) {
            case "Customer":
                return "customer";
            case "Admin":
                return "admin";
            case "RUser":
                return "restaurant-user";
            case "AgencyUser":
                return "agency-user";
            default:
                throw new IllegalArgumentException("Unknown user type: " + className);
        }
    }

    public String generateHubRefreshToken(RUserHub user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "hub");
        claims.put("access_type", "refresh");
        claims.put("email", user.getEmail());
        claims.put("authorities", List.of("PRIVILEGE_REFRESH_ONLY")); // Solo permesso di refresh per Hub
        claims.put("user_type", "restaurant-user-hub"); // Tipo utente specifico per Hub
        return createToken(claims, user.getEmail(), refreshExpiration);
    }

    public String generateAgencyHubRefreshToken(AgencyUserHub user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "agency-hub");
        claims.put("access_type", "refresh");
        claims.put("email", user.getEmail());
        claims.put("authorities", List.of("PRIVILEGE_REFRESH_ONLY")); // Solo permesso di refresh per Agency Hub
        claims.put("user_type", "agency-user-hub"); // Tipo utente specifico per Agency Hub
        return createToken(claims, user.getEmail(), refreshExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, Long tokenExpiration) {
        long now = clock.millis();
        Date issuedAt = new Date(now);
        Date expiry = new Date(now + tokenExpiration);
        return Jwts.builder()
                   .claims(claims)
                   .subject(subject)
                   .issuedAt(issuedAt)
                   .expiration(expiry)
                   .signWith(key, Jwts.SIG.HS256)
                   .compact();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "refresh".equals(claims.get("access_type"));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String accessType = (String) claims.get("access_type");
            return "access".equals(accessType) || "extended".equals(accessType);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isHubToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "hub".equals(claims.get("type")) && "access".equals(claims.get("access_type"));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isHubRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "hub".equals(claims.get("type")) && "refresh".equals(claims.get("access_type"));
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean isAnyHubToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "hub".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAgencyHubToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "agency-hub".equals(claims.get("type")) && "access".equals(claims.get("access_type"));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAgencyHubRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "agency-hub".equals(claims.get("type")) && "refresh".equals(claims.get("access_type"));
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean isAnyAgencyHubToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "agency-hub".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    public String generateHubToken(RUserHub user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "hub");
        claims.put("access_type", "access");
        claims.put("authorities", hubPrivileges());
        claims.put("email", user.getEmail());
        claims.put("user_type", "restaurant-user-hub"); // Tipo utente specifico per Hub
        return createToken(claims, user.getEmail(), expiration);
    }

    public String generateAgencyHubToken(AgencyUserHub user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "agency-hub");
        claims.put("access_type", "access");
        claims.put("authorities", agencyHubPrivileges());
        claims.put("email", user.getEmail());
        claims.put("user_type", "agency-user-hub"); // Tipo utente specifico per Agency Hub
        return createToken(claims, user.getEmail(), expiration);
    }

    private List<String> hubPrivileges() {
        return List.of("PRIVILEGE_HUB", "PRIVILEGE_CHANGE_PASSWORD");
    }

    private List<String> agencyHubPrivileges() {
        return List.of("PRIVILEGE_AGENCY_HUB", "PRIVILEGE_CHANGE_PASSWORD");
    }
    
    /**
     * Estrae le authorities dal token JWT
     */
    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);
        Object authoritiesObj = claims.get("authorities");
        if (authoritiesObj instanceof List) {
            return (List<String>) authoritiesObj;
        }
        return List.of(); // Fallback per token senza authorities
    }
    
    /**
     * Estrae il tipo di utente dal token JWT
     */
    public String extractUserType(String token) {
        Claims claims = extractAllClaims(token);
        return (String) claims.get("user_type");
    }
    
    /**
     * Aggiunge user_id, restaurant_id o agency_id ai claims JWT
     * 
     * - Tutti gli user types → aggiunge user_id via getId()
     * - RUser → aggiunge anche restaurant_id
     * - AgencyUser → aggiunge anche agency_id
     * 
     * @param claims Mappa dei claims JWT
     * @param userDetails Dettagli utente
     */
    private void addUserAndOrganizationIdsToClaims(Map<String, Object> claims, UserDetails userDetails) {
        try {
            // Aggiungi user_id per RUser
            if (userDetails instanceof com.application.restaurant.persistence.model.user.RUser) {
                com.application.restaurant.persistence.model.user.RUser rUser = 
                    (com.application.restaurant.persistence.model.user.RUser) userDetails;
                Long userId = rUser.getId();
                if (userId != null) {
                    claims.put("user_id", userId);
                    log.debug("✅ Added user_id to JWT: {}", userId);
                }
                // Aggiungi restaurant_id
                if (rUser.getRestaurant() != null) {
                    Long restaurantId = rUser.getRestaurant().getId();
                    claims.put("restaurant_id", restaurantId);
                    log.debug("✅ Added restaurant_id to JWT: {}", restaurantId);
                }
                // DISTINCTIVE LOG
                log.info("🔐🔐🔐 [JWT-RUSER] Generated JWT with claims: user_id={}, restaurant_id={}, user_type={}", 
                         userId, rUser.getRestaurant() != null ? rUser.getRestaurant().getId() : null, "restaurant-user");
            }
            // Aggiungi user_id per AgencyUser
            else if (userDetails instanceof com.application.agency.persistence.model.user.AgencyUser) {
                com.application.agency.persistence.model.user.AgencyUser agencyUser = 
                    (com.application.agency.persistence.model.user.AgencyUser) userDetails;
                Long userId = agencyUser.getId();
                if (userId != null) {
                    claims.put("user_id", userId);
                    log.debug("✅ Added user_id to JWT: {}", userId);
                }
                // Aggiungi agency_id
                if (agencyUser.getAgency() != null) {
                    Long agencyId = agencyUser.getAgency().getId();
                    claims.put("agency_id", agencyId);
                    log.debug("✅ Added agency_id to JWT: {}", agencyId);
                }
                // DISTINCTIVE LOG
                log.info("🔐🔐🔐 [JWT-AGENCY] Generated JWT with claims: user_id={}, agency_id={}, user_type={}", 
                         userId, agencyUser.getAgency() != null ? agencyUser.getAgency().getId() : null, "agency-user");
            }
            // Aggiungi user_id per Customer
            else if (userDetails instanceof com.application.customer.persistence.model.Customer) {
                com.application.customer.persistence.model.Customer customer = 
                    (com.application.customer.persistence.model.Customer) userDetails;
                Long userId = customer.getId();
                if (userId != null) {
                    claims.put("user_id", userId);
                    log.debug("✅ Added user_id to JWT: {}", userId);
                }
                // DISTINCTIVE LOG
                log.info("🔐🔐🔐 [JWT-CUSTOMER] Generated JWT with claims: user_id={}, user_type={}", userId, "customer");
            }
            // Aggiungi user_id per Admin
            else if (userDetails instanceof com.application.admin.persistence.model.Admin) {
                com.application.admin.persistence.model.Admin admin = 
                    (com.application.admin.persistence.model.Admin) userDetails;
                Long userId = admin.getId();
                if (userId != null) {
                    claims.put("user_id", userId);
                    log.debug("✅ Added user_id to JWT: {}", userId);
                }
                // DISTINCTIVE LOG
                log.info("🔐🔐🔐 [JWT-ADMIN] Generated JWT with claims: user_id={}, user_type={}", userId, "admin");
            }
        } catch (Exception e) {
            log.warn("⚠️ Error adding IDs to JWT claims: {}", e.getMessage());
        }
    }
}
