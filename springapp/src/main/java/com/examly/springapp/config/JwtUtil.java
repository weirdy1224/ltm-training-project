package com.examly.springapp.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Creates and validates short-lived access tokens. Refresh tokens are handled by RefreshTokenService.
 * <p>
 * Claims of an access token: sub = email, role, userId (number), username, typ = "access", iat, exp.
 * userId and username let the Angular app read them with jwt-decode without an extra request.
 * Never put the password or other secrets into a token.
 *
 * @author Suriya
 */
@Component
public class JwtUtil {
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_VIDEO = "video";

    private final Key key;
    private final long accessExpirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.access-expiration-ms:900000}") long accessExpirationMs) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessExpirationMs = accessExpirationMs;
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    /**
     * Builds a signed access token.
     *
     * @param email    becomes the subject
     * @param role     ADMIN or CUSTOMER
     * @param userId   database id of the user, stored as the "userId" claim (a number)
     * @param username display name, stored as the "username" claim
     */
    public String generateToken(String email, String role, Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("userId", userId)
                .claim("username", username)
                .claim("typ", TYPE_ACCESS)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessExpirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims claims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    public String extractEmail(String token) {
        return claims(token).getSubject();
    }

    public String extractRole(String token) {
        return claims(token).get("role", String.class);
    }

    /** The "userId" claim, or null when the token has none. */
    public Long extractUserId(String token) {
        Object value = claims(token).get("userId");
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    public String extractUsername(String token) {
        return claims(token).get("username", String.class);
    }

    /**
     * Short-lived token that lets ONE video be streamed. A browser {@code <video>} tag cannot send an Authorization header,
     * so the Angular app first asks (with its normal login token) for this token and puts it in the video URL.
     * It carries typ = "video" so it can never be used as a login token, and a login token can never stream.
     */
    public String generateStreamToken(Long videoId, Long userId, long validMinutes) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("videoId", videoId)
                .claim("typ", TYPE_VIDEO)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + validMinutes * 60_000L))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** True when the token is a genuine, unexpired stream token for exactly this video. */
    public boolean isValidStreamToken(String token, Long videoId) {
        try {
            Claims c = claims(token);
            Object id = c.get("videoId");
            return c.getExpiration().after(new Date())
                    && TYPE_VIDEO.equals(c.get("typ", String.class))
                    && id instanceof Number
                    && ((Number) id).longValue() == videoId;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isValid(String token) {
        try {
            Claims c = claims(token);
            return c.getExpiration().after(new Date()) && TYPE_ACCESS.equals(c.get("typ", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateToken(String token) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'validateToken'");
    }
}