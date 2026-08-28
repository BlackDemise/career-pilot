package blackdemise.cp.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

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
import io.jsonwebtoken.Jws;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
    private final AuthService authService =
            new AuthService(userRepository, passwordEncoder, jwtTokenProvider, tokenBlacklistService);

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setFirstName("Ada");
        existingUser.setLastName("Lovelace");
        existingUser.setEmail("ada@example.com");
        existingUser.setPassword("encoded-hash");
        existingUser.setRole(Role.USER);

        when(jwtTokenProvider.generateAccessToken(any(User.class)))
                .thenReturn(new IssuedToken("access-token", "access-jti", Instant.now().plusSeconds(900)));
        when(jwtTokenProvider.generateRefreshToken(any(User.class)))
                .thenReturn(new IssuedToken("refresh-token", "refresh-jti", Instant.now().plusSeconds(604800)));
    }

    @Test
    void register_savesEncodedPasswordAndDefaultRole_whenEmailNotTaken() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");

        TokenPair tokens = authService.register(
                new RegisterRequest("New", "User", "new@example.com", "password123"));

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        verify(userRepository).save(argThatUserHas("new@example.com", "encoded", Role.USER));
    }

    @Test
    void register_throwsConflictException_whenEmailAlreadyRegistered() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Ada", "Lovelace", "ada@example.com", "password123")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void login_returnsTokens_whenCredentialsMatch() {
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("password123", "encoded-hash")).thenReturn(true);

        TokenPair tokens = authService.login(new LoginRequest("ada@example.com", "password123"));

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_throwsUnauthorizedException_whenUserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@example.com", "password123")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_throwsUnauthorizedException_whenPasswordDoesNotMatch() {
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong", "encoded-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ada@example.com", "wrong")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refresh_rotatesTokenAndIssuesNewPair_whenRefreshTokenIsValid() {
        Claims claims = mock(Claims.class);
        when(claims.get("typ", String.class)).thenReturn(TokenType.REFRESH.name());
        when(claims.getId()).thenReturn("old-refresh-jti");
        when(claims.getExpiration()).thenReturn(java.util.Date.from(Instant.now().plusSeconds(1000)));
        when(claims.getSubject()).thenReturn(existingUser.getId().toString());
        stubParse("valid-refresh-token", claims);

        when(tokenBlacklistService.isBlacklisted("old-refresh-jti")).thenReturn(false);
        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));

        TokenPair tokens = authService.refresh("valid-refresh-token");

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        verify(tokenBlacklistService).blacklist(eq("old-refresh-jti"), any(Instant.class));
    }

    @Test
    void refresh_throwsUnauthorizedException_whenTokenIsMissing() {
        assertThatThrownBy(() -> authService.refresh(null)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> authService.refresh("  ")).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refresh_throwsUnauthorizedException_whenTokenIsAlreadyBlacklisted() {
        Claims claims = mock(Claims.class);
        when(claims.get("typ", String.class)).thenReturn(TokenType.REFRESH.name());
        when(claims.getId()).thenReturn("used-jti");
        stubParse("used-refresh-token", claims);
        when(tokenBlacklistService.isBlacklisted("used-jti")).thenReturn(true);

        assertThatThrownBy(() -> authService.refresh("used-refresh-token"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refresh_throwsUnauthorizedException_whenTokenTypeIsNotRefresh() {
        Claims claims = mock(Claims.class);
        when(claims.get("typ", String.class)).thenReturn(TokenType.ACCESS.name());
        stubParse("access-token-used-as-refresh", claims);

        assertThatThrownBy(() -> authService.refresh("access-token-used-as-refresh"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void logout_blacklistsBothTokens_whenBothAreValid() {
        Claims accessClaims = mock(Claims.class);
        when(accessClaims.getId()).thenReturn("access-jti");
        when(accessClaims.getExpiration()).thenReturn(java.util.Date.from(Instant.now().plusSeconds(500)));
        stubParse("access-token", accessClaims);

        Claims refreshClaims = mock(Claims.class);
        when(refreshClaims.getId()).thenReturn("refresh-jti");
        when(refreshClaims.getExpiration()).thenReturn(java.util.Date.from(Instant.now().plusSeconds(9000)));
        stubParse("refresh-token", refreshClaims);

        authService.logout("access-token", "refresh-token");

        verify(tokenBlacklistService).blacklist(eq("access-jti"), any(Instant.class));
        verify(tokenBlacklistService).blacklist(eq("refresh-jti"), any(Instant.class));
    }

    @Test
    void logout_doesNotThrow_whenTokensAreMissingOrInvalid() {
        when(jwtTokenProvider.parse(anyString())).thenThrow(new JwtException("bad token"));

        authService.logout(null, "garbage");

        verify(tokenBlacklistService, never()).blacklist(anyString(), any(Instant.class));
    }

    @SuppressWarnings("unchecked")
    private void stubParse(String token, Claims claims) {
        Jws<Claims> jws = mock(Jws.class);
        when(jws.getPayload()).thenReturn(claims);
        when(jwtTokenProvider.parse(token)).thenReturn(jws);
    }

    private User argThatUserHas(String email, String encodedPassword, Role role) {
        return org.mockito.ArgumentMatchers.argThat(user ->
                user != null
                        && email.equals(user.getEmail())
                        && encodedPassword.equals(user.getPassword())
                        && role == user.getRole());
    }
}
