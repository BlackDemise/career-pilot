package blackdemise.cp.user;

import java.util.UUID;

public record PasswordResetToken(UUID userId, String email) {
}
