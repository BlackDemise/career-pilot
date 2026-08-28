package blackdemise.cp.cv.dto;

import java.time.Instant;
import java.util.UUID;

import blackdemise.cp.cv.CvAnalysisType;

public record CvAnalysisResponse(
        UUID id,
        UUID cvId,
        CvAnalysisType type,
        Object result,
        String jobDescription,
        Instant createdAt) {
}
