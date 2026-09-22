package blackdemise.cp.cv.dto;

import java.util.List;

import blackdemise.cp.cv.CvMatchStatus;
import blackdemise.cp.cv.EvidenceVerificationStatus;

public record CvRequirementMatch(
        String requirementId,
        CvMatchStatus status,
        String section,
        String evidenceQuote,
        List<String> sourceBlockIds,
        double confidence,
        EvidenceVerificationStatus verification) {
}