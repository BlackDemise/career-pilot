package blackdemise.cp.cv.dto;

import java.util.List;

public record CvRequirementMatchesResponse(
        List<CvRequirementMatch> requirementMatches,
        List<String> matchedSkills,
        List<String> missingSkills,
        List<String> experienceGaps,
        List<String> recommendations) {
}