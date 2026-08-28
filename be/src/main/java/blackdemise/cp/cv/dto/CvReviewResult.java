package blackdemise.cp.cv.dto;

import java.util.List;

public record CvReviewResult(
        String overallAssessment,
        List<String> strengths,
        List<String> weaknesses,
        List<String> recommendations) {
}
