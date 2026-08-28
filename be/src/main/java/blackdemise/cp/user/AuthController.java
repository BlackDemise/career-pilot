package blackdemise.cp.user;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import blackdemise.cp.security.jwt.JwtProperties;
import blackdemise.cp.user.dto.AuthResponse;
import blackdemise.cp.user.dto.LoginRequest;
import blackdemise.cp.user.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        TokenPair tokens = authService.register(request);
        applyRefreshCookie(response, tokens);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(tokens.accessToken()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        TokenPair tokens = authService.login(request);
        applyRefreshCookie(response, tokens);
        return ResponseEntity.ok(new AuthResponse(tokens.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "${app.jwt.refresh-cookie.name}", required = false) String refreshToken,
            HttpServletResponse response) {
        TokenPair tokens = authService.refresh(refreshToken);
        applyRefreshCookie(response, tokens);
        return ResponseEntity.ok(new AuthResponse(tokens.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "${app.jwt.refresh-cookie.name}", required = false) String refreshToken,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            HttpServletResponse response) {
        authService.logout(extractBearerToken(authorizationHeader), refreshToken);
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void applyRefreshCookie(HttpServletResponse response, TokenPair tokens) {
        Duration maxAge = Duration.between(Instant.now(), tokens.refreshExpiresAt());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(tokens.refreshToken(), maxAge.isNegative() ? Duration.ZERO : maxAge));
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO));
    }

    private String buildCookie(String value, Duration maxAge) {
        JwtProperties.RefreshCookie cookieProperties = jwtProperties.refreshCookie();
        return ResponseCookie.from(cookieProperties.name(), value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(cookieProperties.path())
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
