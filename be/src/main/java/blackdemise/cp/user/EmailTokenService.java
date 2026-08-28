package blackdemise.cp.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailTokenService {

    private static final Duration TOKEN_TTL = Duration.ofHours(1);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final String REGISTRATION_TOKEN_PREFIX = "auth:registration:token:";
    private static final String REGISTRATION_EMAIL_PREFIX = "auth:registration:email:";
    private static final String PASSWORD_RESET_TOKEN_PREFIX = "auth:password-reset:token:";
    private static final String PASSWORD_RESET_EMAIL_PREFIX = "auth:password-reset:email:";
    private static final String REGISTRATION_COOLDOWN_PREFIX = "auth:registration:cooldown:";
    private static final String PASSWORD_RESET_COOLDOWN_PREFIX = "auth:password-reset:cooldown:";

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public Optional<Duration> registrationCooldownRemaining(String email) {
        return cooldownRemaining(REGISTRATION_COOLDOWN_PREFIX + normalize(email));
    }

    public Optional<Duration> passwordResetCooldownRemaining(String email) {
        return cooldownRemaining(PASSWORD_RESET_COOLDOWN_PREFIX + normalize(email));
    }

    public String createRegistrationToken(EmailVerificationToken value) {
        return createToken(REGISTRATION_TOKEN_PREFIX, REGISTRATION_EMAIL_PREFIX,
            REGISTRATION_COOLDOWN_PREFIX, value.email(), value);
    }

    public String createPasswordResetToken(PasswordResetToken value) {
        return createToken(PASSWORD_RESET_TOKEN_PREFIX, PASSWORD_RESET_EMAIL_PREFIX,
            PASSWORD_RESET_COOLDOWN_PREFIX, value.email(), value);
    }

    public EmailVerificationToken consumeRegistrationToken(String rawToken) {
        return consumeToken(REGISTRATION_TOKEN_PREFIX, rawToken, EmailVerificationToken.class);
    }

    public PasswordResetToken consumePasswordResetToken(String rawToken) {
        return consumeToken(PASSWORD_RESET_TOKEN_PREFIX, rawToken, PasswordResetToken.class);
    }

    public Optional<EmailVerificationToken> pendingRegistration(String email) {
        return pending(REGISTRATION_TOKEN_PREFIX, REGISTRATION_EMAIL_PREFIX, email, EmailVerificationToken.class);
    }

    public Optional<PasswordResetToken> pendingPasswordReset(String email) {
        return pending(PASSWORD_RESET_TOKEN_PREFIX, PASSWORD_RESET_EMAIL_PREFIX, email, PasswordResetToken.class);
    }

    private <T> String createToken(String tokenPrefix, String emailPrefix, String cooldownPrefix,
            String email, T value) {
        String rawToken = randomToken();
        String tokenKey = tokenPrefix + hash(rawToken);
        String emailKey = emailPrefix + normalize(email);
        String oldToken = redisTemplate.opsForValue().get(emailKey);
        if (oldToken != null) {
            redisTemplate.delete(tokenPrefix + hash(oldToken));
        }
        try {
            redisTemplate.opsForValue().set(tokenKey, jsonMapper.writeValueAsString(value), TOKEN_TTL);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not store email token", ex);
        }
        redisTemplate.opsForValue().set(emailKey, rawToken, TOKEN_TTL);
        redisTemplate.opsForValue().set(cooldownPrefix + normalize(email), "1", RESEND_COOLDOWN);
        return rawToken;
    }

    private <T> Optional<T> pending(String tokenPrefix, String emailPrefix, String email, Class<T> type) {
        String rawToken = redisTemplate.opsForValue().get(emailPrefix + normalize(email));
        if (rawToken == null) {
            return Optional.empty();
        }
        String serialized = redisTemplate.opsForValue().get(tokenPrefix + hash(rawToken));
        if (serialized == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(jsonMapper.readValue(serialized, type));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read email token", ex);
        }
    }

    private <T> T consumeToken(String tokenPrefix, String rawToken, Class<T> type) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        String key = tokenPrefix + hash(rawToken);
        String serialized = redisTemplate.opsForValue().getAndDelete(key);
        if (serialized == null) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        try {
            return jsonMapper.readValue(serialized, type);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read email token", ex);
        }
    }

    private Optional<Duration> cooldownRemaining(String key) {
        Long ttlSeconds = redisTemplate.getExpire(key);
        if (ttlSeconds == null || ttlSeconds <= 0) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofSeconds(ttlSeconds));
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
