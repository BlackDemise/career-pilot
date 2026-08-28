package blackdemise.cp.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Credentials for the single development/dev user seeded on startup (see DevUserInitializer).
@ConfigurationProperties(prefix = "app.dev-user")
public record DevUserProperties(String username, String password) {
}
