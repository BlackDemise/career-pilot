package blackdemise.cp.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import blackdemise.cp.common.exception.ConflictException;
import blackdemise.cp.common.exception.BadRequestException;
import blackdemise.cp.common.exception.TooManyRequestsException;
import blackdemise.cp.common.exception.UnauthorizedException;
import blackdemise.cp.security.jwt.IssuedToken;
import blackdemise.cp.security.jwt.JwtTokenProvider;
import blackdemise.cp.security.jwt.TokenBlacklistService;
import blackdemise.cp.security.jwt.TokenType;
import blackdemise.cp.user.dto.LoginRequest;
import blackdemise.cp.user.dto.ForgotPasswordRequest;
import blackdemise.cp.user.dto.RegisterRequest;
import blackdemise.cp.user.dto.ResetPasswordRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmailTokenService emailTokenService;
    private final EmailService emailService;

    public void register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        ensureCooldownExpired(email, emailTokenService.registrationCooldownRemaining(email));

        String token = emailTokenService.createRegistrationToken(new EmailVerificationToken(
                request.firstName(), request.lastName(), email, passwordEncoder.encode(request.password())));
        emailService.sendRegistrationVerification(email, token);
    }

    public void resendRegistration(String email) {
        String normalizedEmail = normalize(email);
        EmailVerificationToken pending = emailTokenService.pendingRegistration(normalizedEmail).orElse(null);
        if (pending == null) {
            return;
        }
        ensureCooldownExpired(normalizedEmail, emailTokenService.registrationCooldownRemaining(normalizedEmail));
        String token = emailTokenService.createRegistrationToken(pending);
        emailService.sendRegistrationVerification(normalizedEmail, token);
    }

    public void verifyRegistration(String token) {
        EmailVerificationToken pending;
        try {
            pending = emailTokenService.consumeRegistrationToken(token);
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid or expired registration link");
        }
        if (userRepository.existsByEmail(pending.email())) {
            throw new ConflictException("Email is already registered");
        }
        User user = new User();
        user.setFirstName(pending.firstName());
        user.setLastName(pending.lastName());
        user.setEmail(pending.email());
        user.setPassword(pending.passwordHash());
        user.setRole(Role.USER);
        userRepository.save(user);
    }

    public TokenPair login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return issueTokens(user);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        String email = normalize(request.email());
        userRepository.findByEmail(email).ifPresent(user -> {
            if (emailTokenService.passwordResetCooldownRemaining(email).isPresent()) {
                return;
            }
            String token = emailTokenService.createPasswordResetToken(new PasswordResetToken(user.getId(), email));
            emailService.sendPasswordReset(email, token);
        });
    }

    public void resendPasswordReset(String email) {
        String normalizedEmail = normalize(email);
        PasswordResetToken pending = emailTokenService.pendingPasswordReset(normalizedEmail).orElse(null);
        if (pending == null) {
            return;
        }
        ensureCooldownExpired(normalizedEmail, emailTokenService.passwordResetCooldownRemaining(normalizedEmail));
        String token = emailTokenService.createPasswordResetToken(pending);
        emailService.sendPasswordReset(normalizedEmail, token);
    }

    public void resetPassword(ResetPasswordRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Password and confirmation do not match");
        }
        PasswordResetToken pending;
        try {
            pending = emailTokenService.consumePasswordResetToken(request.token());
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid or expired password reset link");
        }
        User user = userRepository.findById(pending.userId())
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);
    }

    public TokenPair refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Missing refresh token");
        }

        Claims claims = parseOrThrow(refreshToken, "Invalid or expired refresh token");

        if (!TokenType.REFRESH.name().equals(claims.get("typ", String.class))) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        if (tokenBlacklistService.isBlacklisted(claims.getId())) {
            throw new UnauthorizedException("Refresh token has already been used or revoked");
        }

        // Rotate immediately: a refresh token is single-use.
        tokenBlacklistService.blacklist(claims.getId(), claims.getExpiration().toInstant());

        User user = userRepository.findById(java.util.UUID.fromString(claims.getSubject()))
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));

        return issueTokens(user);
    }

    public void logout(String accessToken, String refreshToken) {
        tryBlacklist(accessToken);
        tryBlacklist(refreshToken);
    }

    private TokenPair issueTokens(User user) {
        IssuedToken access = jwtTokenProvider.generateAccessToken(user);
        IssuedToken refresh = jwtTokenProvider.generateRefreshToken(user);
        return new TokenPair(access.token(), refresh.token(), refresh.expiresAt());
    }

    private Claims parseOrThrow(String token, String errorMessage) {
        try {
            return jwtTokenProvider.parse(token).getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException(errorMessage);
        }
    }

    private void tryBlacklist(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            Claims claims = jwtTokenProvider.parse(token).getPayload();
            tokenBlacklistService.blacklist(claims.getId(), claims.getExpiration().toInstant());
        } catch (JwtException | IllegalArgumentException ex) {
            // Already invalid/expired: nothing to revoke, logout still succeeds.
        }
    }

    private void ensureCooldownExpired(String email, java.util.Optional<java.time.Duration> remaining) {
        if (remaining.isPresent()) {
            throw new TooManyRequestsException("Please wait before requesting another email");
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
