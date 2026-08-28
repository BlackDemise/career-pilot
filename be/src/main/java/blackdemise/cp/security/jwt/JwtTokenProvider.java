package blackdemise.cp.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import blackdemise.cp.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// Issues and parses signed JWTs for both access and refresh tokens (HS256, shared secret).
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_FIRST_NAME = "firstName";
    private static final String CLAIM_LAST_NAME = "lastName";
    private static final String CLAIM_ROLE = "role";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public IssuedToken generateAccessToken(User user) {
        return generate(user, TokenType.ACCESS, properties.accessTtlSeconds());
    }

    public IssuedToken generateRefreshToken(User user) {
        return generate(user, TokenType.REFRESH, properties.refreshTtlSeconds());
    }

    private IssuedToken generate(User user, TokenType type, long ttlSeconds) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(ttlSeconds);

        String token = Jwts.builder()
                .id(jti)
                .subject(user.getId().toString())
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_TYPE, type.name())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_FIRST_NAME, user.getFirstName())
                .claim(CLAIM_LAST_NAME, user.getLastName())
                .claim(CLAIM_ROLE, user.getRole().name())
                .signWith(key)
                .compact();

        return new IssuedToken(token, jti, expiresAt);
    }

    /**
     * @throws io.jsonwebtoken.JwtException if the token is malformed, expired, or has an invalid signature
     */
    public Jws<Claims> parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }
}
