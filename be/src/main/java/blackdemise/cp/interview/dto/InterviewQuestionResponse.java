package blackdemise.cp.interview.dto;

import java.util.UUID;

public record InterviewQuestionResponse(
        UUID id,
        Integer orderIndex,
        String content,
        String topic,
        String difficulty,
        String answer) {
}