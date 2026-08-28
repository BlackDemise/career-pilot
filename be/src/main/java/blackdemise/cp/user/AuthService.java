package blackdemise.cp.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import blackdemise.cp.common.exception.ConflictException;
import blackdemise.cp.common.exception.UnauthorizedException;
import blackdemise.cp.security.jwt.IssuedToken;
import blackdemise.cp.security.jwt.JwtTokenProvider;
import blackdemise.cp.security.jwt.TokenBlacklistService;
import blackdemise.cp.security.jwt.TokenType;
import blackdemise.cp.user.dto.LoginRequest;
import blackdemise.cp.user.dto.RegisterRequest;
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

    public TokenPair register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email is already registered");
        }

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        userRepository.save(user);

        return issueTokens(user);
    }

    public TokenPair login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return issueTokens(user);
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
}
