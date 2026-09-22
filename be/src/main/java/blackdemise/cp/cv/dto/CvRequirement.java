package blackdemise.cp.cv.dto;

import blackdemise.cp.cv.RequirementCategory;

public record CvRequirement(
        String id,
        String requirement,
        RequirementCategory category,
        double categoryConfidence,
        String categoryRationale,
        String sourceText,
        String section) {
}