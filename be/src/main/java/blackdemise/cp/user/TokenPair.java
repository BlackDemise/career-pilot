package blackdemise.cp.user;

import java.time.Instant;

// Internal pairing of a freshly issued access + refresh token, not exposed over the wire as-is.
public record TokenPair(String accessToken, String refreshToken, Instant refreshExpiresAt) {
}
