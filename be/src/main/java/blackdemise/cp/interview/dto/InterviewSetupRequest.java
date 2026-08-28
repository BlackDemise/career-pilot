package blackdemise.cp.interview.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InterviewSetupRequest(
        @NotNull UUID roleId,
        @NotNull UUID levelId,
        List<UUID> topicIds,
        @Min(60) @Max(3600) Integer durationSeconds,
        @Min(1) @Max(30) Integer minimumPrimaryQuestions,
        @Min(1) @Max(30) Integer maximumPrimaryQuestions,
        boolean randomPlan) {
}