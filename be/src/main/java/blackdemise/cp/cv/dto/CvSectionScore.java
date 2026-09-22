package blackdemise.cp.cv.dto;

import java.util.List;

public record CvSectionScore(
        String section,
        int score,
        List<String> matchedRequirementIds,
        List<String> missingRequirementIds,
        List<String> gaps) {
}