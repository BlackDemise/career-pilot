package blackdemise.cp.user.dto;

// Refresh token is never included here - it is only ever transported via the httpOnly cookie
// set by AuthController.
public record AuthResponse(String accessToken) {
}
