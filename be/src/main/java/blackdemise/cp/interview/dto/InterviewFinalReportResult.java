package blackdemise.cp.interview.dto;

import java.util.List;

public record InterviewFinalReportResult(
        Integer overallScore, List<String> strengths, List<String> weaknesses, List<String> recommendations) {
}