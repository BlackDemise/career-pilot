package blackdemise.cp.user.dto;

import java.util.UUID;

public record UserProfileResponse(UUID userId, String preferredLanguage, String responseStyle,
        String technicalBackground, String careerGoal, String customInstructions) {
}