package blackdemise.cp.interview.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitAnswerRequest(@NotBlank String content) {
}