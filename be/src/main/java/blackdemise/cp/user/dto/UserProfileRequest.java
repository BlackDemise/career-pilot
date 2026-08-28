package blackdemise.cp.user.dto;

import jakarta.validation.constraints.Size;

public record UserProfileRequest(
        @Size(max = 100) String preferredLanguage,
        @Size(max = 100) String responseStyle,
        @Size(max = 10000) String technicalBackground,
        @Size(max = 10000) String careerGoal,
        @Size(max = 10000) String customInstructions) {
}