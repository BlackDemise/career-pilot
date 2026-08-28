package blackdemise.cp.user;

public record EmailVerificationToken(
        String firstName,
        String lastName,
        String email,
        String passwordHash) {
}
