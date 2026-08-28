package blackdemise.cp.cv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CvJdMatchRequest(
        @NotBlank(message = "Job description is required")
        @Size(max = 50000, message = "Job description must not exceed 50000 characters") String jobDescription) {
}
