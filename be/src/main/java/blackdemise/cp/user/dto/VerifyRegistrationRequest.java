package blackdemise.cp.user.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyRegistrationRequest(@NotBlank String token) {
}
