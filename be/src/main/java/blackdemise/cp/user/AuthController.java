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

import blackdemise.cp.common.ApiResponse;
import blackdemise.cp.security.jwt.JwtProperties;
import blackdemise.cp.user.dto.EmailRequest;
import blackdemise.cp.user.dto.ForgotPasswordRequest;
import blackdemise.cp.user.dto.LoginRequest;
import blackdemise.cp.user.dto.RegisterRequest;
import blackdemise.cp.user.dto.ResetPasswordRequest;
import blackdemise.cp.user.dto.VerifyRegistrationRequest;
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
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.accepted().body(ApiResponse.success(
                HttpStatus.ACCEPTED.value(), "Check your email to verify your account", null));
    }

    @PostMapping("/register/resend")
    public ResponseEntity<ApiResponse> resendRegistration(@Valid @RequestBody EmailRequest request) {
        authService.resendRegistration(request.email());
        return ResponseEntity.accepted().body(ApiResponse.success(
                HttpStatus.ACCEPTED.value(), "A new verification email was sent", null));
    }

    @PostMapping("/register/verify")
    public ResponseEntity<ApiResponse> verifyRegistration(
            @Valid @RequestBody VerifyRegistrationRequest request) {
        authService.verifyRegistration(request.token());
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Your account is verified. Please log in", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        TokenPair tokens = authService.login(request);
        applyRefreshCookie(response, tokens);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Login successful", tokens.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refresh(
            @CookieValue(name = "${app.jwt.refresh-cookie.name}", required = false) String refreshToken,
            HttpServletResponse response) {
        TokenPair tokens = authService.refresh(refreshToken);
        applyRefreshCookie(response, tokens);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Token refreshed successfully", tokens.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(
            @CookieValue(name = "${app.jwt.refresh-cookie.name}", required = false) String refreshToken,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            HttpServletResponse response) {
        authService.logout(extractBearerToken(authorizationHeader), refreshToken);
        clearRefreshCookie(response);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Logout successful", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.accepted().body(ApiResponse.success(
                HttpStatus.ACCEPTED.value(),
                "If an account exists for this email, check your inbox for reset instructions", null));
    }

    @PostMapping("/forgot-password/resend")
    public ResponseEntity<ApiResponse> resendPasswordReset(@Valid @RequestBody EmailRequest request) {
        authService.resendPasswordReset(request.email());
        return ResponseEntity.accepted().body(ApiResponse.success(
                HttpStatus.ACCEPTED.value(), "A new password reset email was sent", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Your password was reset. Please log in", null));
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
