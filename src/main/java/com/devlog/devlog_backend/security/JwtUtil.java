package com.devlog.devlog_backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

// WHY @Component: makes this a Spring-managed bean so we can
// @Autowired inject it anywhere. It's not a @Service because
// it has no business logic — it's a pure utility.
@Component
public class JwtUtil {

    // WHY @Value: reads jwt.secret from application.properties.
    // Never hardcode secrets — this way you can change them per environment.
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration; // 86400000ms = 24 hours

    // WHY this method: converts the plain String secret into a
    // cryptographic Key object that jjwt needs for HMAC-SHA256 signing.
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // ── TOKEN GENERATION ──────────────────────────────────────────────

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // You could add extra data here: claims.put("role", "USER")
        // but keep it minimal — JWTs are sent on EVERY request.
        return buildToken(claims, userDetails.getUsername());
    }

    private String buildToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)          // sub = email address
                .setIssuedAt(new Date())      // iat = when token was created
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // exp
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // sign it
                .compact();                   // serialize to the "xxx.yyy.zzz" string
    }

    // ── TOKEN VALIDATION ──────────────────────────────────────────────

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        // Token is valid only if the email matches AND it hasn't expired
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ── CLAIM EXTRACTION ──────────────────────────────────────────────

    public String extractUsername(String token) {
        // 'subject' in JWT = the email we stored during login
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // WHY this generic method: instead of parsing the JWT 10 different times,
    // we parse it once and pass a function to extract whichever claim we want.
    // Function<Claims, T> means "give me a function that takes Claims and returns T"
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        // parserBuilder validates the signature using our secret key.
        // If tampered with, this throws a SignatureException automatically.
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}