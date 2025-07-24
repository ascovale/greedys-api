package com.application.common.security.jwt;

import java.time.Clock;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.application.restaurant.persistence.model.user.RUserHub;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey key;
    private Clock clock = Clock.systemDefaultZone();

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key is too short, must be at least 256 bits");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
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
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        long now = clock.millis();
        Date issuedAt = new Date(now);
        Date expiry = new Date(now + expiration);
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

    public String generateHubToken(RUserHub user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "hub");
        claims.put("authorities", hubPrivileges());
        claims.put("email", user.getEmail());
        long now = clock.millis();
        Date issuedAt = new Date(now);
        Date expiry = new Date(now + expiration);
        return Jwts.builder()
                   .claims(claims)
                   .subject(user.getEmail())
                   .issuedAt(issuedAt)
                   .expiration(expiry)
                   .signWith(key, Jwts.SIG.HS256)
                   .compact();
    }

    private List<String> hubPrivileges() {
        return List.of("PRIVILEGE_HUB", "PRIVILEGE_CHANGE_PASSWORD");
    }
}
