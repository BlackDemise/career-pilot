package blackdemise.cp.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

// JWT signing/TTL config and refresh-cookie settings, sourced from application.yml / env.
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long accessTtlSeconds,
        long refreshTtlSeconds,
        RefreshCookie refreshCookie) {

    public record RefreshCookie(String name, String path, boolean secure, String sameSite) {
    }
}
