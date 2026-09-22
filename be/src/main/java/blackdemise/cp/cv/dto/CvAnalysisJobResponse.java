package blackdemise.cp.cv.dto;

import java.time.Instant;
import java.util.UUID;

import blackdemise.cp.cv.CvAnalysisJobStatus;
import blackdemise.cp.cv.CvAnalysisType;

public record CvAnalysisJobResponse(
        UUID jobId,
        UUID cvId,
        CvAnalysisType type,
        CvAnalysisJobStatus status,
        String stage,
        UUID analysisId,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt) {
}