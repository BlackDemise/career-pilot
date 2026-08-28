package blackdemise.cp.security.jwt;

import java.time.Instant;

public record IssuedToken(String token, String jti, Instant expiresAt) {
}
