package blackdemise.cp.cv.dto;

import java.time.Instant;
import java.util.UUID;

import blackdemise.cp.cv.CvExtractionJobStatus;

public record CvExtractionJobResponse(
        UUID jobId,
        UUID cvId,
        CvExtractionJobStatus status,
        String stage,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt) {
}