package blackdemise.cp.cv.dto;

import java.time.Instant;
import java.util.UUID;

public record CvResponse(UUID id, String fileName, String extractedText, Instant createdAt) {
}
