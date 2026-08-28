package blackdemise.cp.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import blackdemise.cp.user.Role;
import blackdemise.cp.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;

class JwtTokenProviderTest {

    private final JwtProperties properties = new JwtProperties(
            "test-secret-at-least-32-bytes-long!!",
            "career-pilot-test",
            900,
            604800,
            new JwtProperties.RefreshCookie("refreshToken", "/api/v1/auth", true, "Lax"));

    private final JwtTokenProvider provider = new JwtTokenProvider(properties);

    private User testUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setEmail("ada@example.com");
        user.setPassword("hashed");
        user.setRole(Role.USER);
        return user;
    }

    @Test
    void generateAccessToken_producesAParsableTokenWithExpectedClaims() {
        User user = testUser();

        IssuedToken issued = provider.generateAccessToken(user);
        Claims claims = provider.parse(issued.token()).getPayload();

        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.getId()).isEqualTo(issued.jti());
        assertThat(claims.get("typ", String.class)).isEqualTo(TokenType.ACCESS.name());
        assertThat(claims.get("email", String.class)).isEqualTo("ada@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    void generateRefreshToken_isMarkedAsRefreshType() {
        IssuedToken issued = provider.generateRefreshToken(testUser());

        Claims claims = provider.parse(issued.token()).getPayload();

        assertThat(claims.get("typ", String.class)).isEqualTo(TokenType.REFRESH.name());
    }

    @Test
    void parse_throwsExpiredJwtException_forAnAlreadyExpiredToken() {
        JwtProperties expiredProperties = new JwtProperties(
                properties.secret(), properties.issuer(), -1, -1, properties.refreshCookie());
        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProperties);

        String token = expiredProvider.generateAccessToken(testUser()).token();

        assertThatThrownBy(() -> expiredProvider.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }
}
