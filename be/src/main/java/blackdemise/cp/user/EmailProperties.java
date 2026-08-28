package blackdemise.cp.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email")
public record EmailProperties(
        String from,
        String frontendBaseUrl,
        String registrationPath,
        String passwordResetPath) {
}
