package blackdemise.cp.cv.dto;

import java.util.List;

public record CvJdMatchResult(
        int matchScore,
        List<String> matchedSkills,
        List<String> missingSkills,
        List<String> experienceGaps,
        List<String> recommendations) {
}
