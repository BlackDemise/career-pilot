package blackdemise.cp.interview.dto;

import java.util.List;

public record InterviewEvaluationResult(
        Integer score, List<String> strengths, List<String> weaknesses, String feedback) {
}