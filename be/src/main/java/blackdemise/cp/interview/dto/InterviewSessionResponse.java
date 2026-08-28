package blackdemise.cp.interview.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import blackdemise.cp.interview.InterviewStatus;

public record InterviewSessionResponse(
        UUID id,
        String role,
        String level,
        String topic,
        String difficulty,
        Integer numQuestions,
        Integer durationSeconds,
        Integer minimumPrimaryQuestions,
        Integer maximumPrimaryQuestions,
        Integer primaryQuestionsAsked,
        Integer totalTurns,
        java.time.Instant endsAt,
        blackdemise.cp.interview.InterviewPhase currentPhase,
        InterviewStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<InterviewQuestionResponse> questions) {
}