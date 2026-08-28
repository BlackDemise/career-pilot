package blackdemise.cp.security.jwt;

import java.util.UUID;

// Claims-only authenticated principal populated by JwtAuthenticationFilter; no DB hit per request.
public record JwtUserPrincipal(UUID id, String email, String firstName, String lastName, String role) {
}
